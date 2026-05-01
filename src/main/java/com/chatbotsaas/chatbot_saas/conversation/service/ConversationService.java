package com.chatbotsaas.chatbot_saas.conversation.service;

import com.chatbotsaas.chatbot_saas.auth.service.AuthService;
import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.bot.repository.BotRepository;
import com.chatbotsaas.chatbot_saas.conversation.dto.request.ConversationRequestDto;
import com.chatbotsaas.chatbot_saas.conversation.dto.response.ConversationResponseDto;
import com.chatbotsaas.chatbot_saas.conversation.entity.Conversation;
import com.chatbotsaas.chatbot_saas.conversation.enums.ConversationStatus;
import com.chatbotsaas.chatbot_saas.conversation.repository.ConversationRepository;
import com.chatbotsaas.chatbot_saas.integration.dto.request.ChatMessageDto;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.shared.security.TenantAccessValidator;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {
    private final BotRepository botRepository;
    private final ConversationRepository conversationRepository;
    private final AuthService authService;
    private final TenantAccessValidator tenantAccessValidator;

    public ConversationService(BotRepository botRepository, ConversationRepository conversationRepository, AuthService authService, TenantAccessValidator tenantAccessValidator) {
        this.botRepository = botRepository;
        this.conversationRepository = conversationRepository;
        this.authService = authService;
        this.tenantAccessValidator = tenantAccessValidator;
    }

    private ConversationResponseDto toResponseDto (Conversation conversation) {
        return ConversationResponseDto.builder()
                .conversationId(conversation.getId())
                .botId(conversation.getBotId())
                .sessionId(conversation.getSessionId())
                .role(conversation.getRole())
                .message(conversation.getMessage())
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    /**
     * Reconstruye el {@code chat_history} para mandar al RAG en {@code POST /chat}.
     *
     * <p>Trae los últimos 40 mensajes de la sesión ({@code chat_history_max_turns × 2}
     * según la convención del RAG) en orden cronológico ASC. El role guardado
     * en BD es uppercase (USER/ASSISTANT) — lo mapea a lowercase porque el
     * schema del RAG valida {@code Literal["user", "assistant"]}.
     *
     * <p><b>Importante</b>: el caller (ChatService) debe invocar este método
     * <i>antes</i> de guardar el mensaje del usuario actual. Si lo invoca
     * después, el mensaje recién insertado se incluiría en el histórico y
     * el RAG lo recibiría duplicado (también lo recibe como {@code message}).
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getRecentHistoryForRag(UUID botId, String sessionId) {
        List<Conversation> recent = new ArrayList<>(
                conversationRepository.findTop40ByBotIdAndSessionIdOrderByCreatedAtDesc(botId, sessionId)
        );
        // Invertir DESC → ASC (cronológico). El RAG espera los más viejos primero
        // porque va a apilar los mensajes en ese orden al armar el prompt.
        Collections.reverse(recent);
        return recent.stream()
                .map(c -> ChatMessageDto.builder()
                        .role(c.getRole().toLowerCase())
                        .content(c.getMessage())
                        .build())
                .toList();
    }

    @Transactional
    public ConversationResponseDto saveMessage(ConversationRequestDto request) {
        botRepository.findById(request.getBotId()).orElseThrow(
                () -> new IllegalArgumentException("Bot not found")
        );
        Conversation conversation = conversationRepository.save(
                    Conversation.create(
                            request.getBotId(),
                            request.getSessionId(),
                            request.getRole(),
                            request.getMessage()
                    )
        );
        return toResponseDto(conversation);
    }

    @Transactional(readOnly = true)
    public Page<ConversationResponseDto> getConversations(UUID botId, UUID tenantId, Pageable pageable) {
        User user = authService.getUserAuthenticated();
        if (user == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
        boolean isAdmin = user.isAdmin();
        UUID myTenantId = user.getTenant() != null ? user.getTenant().getId() : null;

        if (!isAdmin && tenantId != null && !tenantId.equals(myTenantId)) {
            throw new AppException("Forbidden", HttpStatus.FORBIDDEN);
        }

        Page<Conversation> conversations;
        if (botId != null) {
            Bot bot = botRepository.findById(botId)
                    .orElseThrow(() -> new AppException("Bot not found", HttpStatus.NOT_FOUND));
            if (!isAdmin && !bot.getTenant().getId().equals(myTenantId)) {
                throw new AppException("Forbidden", HttpStatus.FORBIDDEN);
            }
            conversations = conversationRepository.findByBotId(botId, pageable);
        } else if (tenantId != null) {
            conversations = conversationRepository.findByTenantId(tenantId, pageable);
        } else if (isAdmin) {
            conversations = conversationRepository.findAll(pageable);
        } else {
            conversations = conversationRepository.findByTenantId(myTenantId, pageable);
        }
        return conversations.map(this::toResponseDto);
    }

    @Transactional
    public List<ConversationResponseDto> getConversationBySession(String sessionId) {
        if (!conversationRepository.existsBySessionId(sessionId)) {
            throw new AppException("session not found", HttpStatus.NOT_FOUND);
        }
        List<Conversation> conversations = conversationRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        if (conversations.isEmpty()) {
            return List.of();
        }
        // Aislamiento multi-tenant — el sessionId es de baja entropía (lo
        // genera el widget front-end), así que un caller malicioso con
        // `conversations:view` podría leer conversaciones ajenas si no
        // validamos por tenant. Resolvemos el bot desde la primera
        // conversación y delegamos al validator.
        UUID botId = conversations.get(0).getBotId();
        Bot bot = botRepository.findById(botId).orElseThrow(
                () -> new AppException("Bot not found", HttpStatus.NOT_FOUND)
        );
        tenantAccessValidator.assertCanAccessBot(bot);
        return conversations.stream().map(
                this::toResponseDto
        ).toList();
    }

    /**
     * Actualiza el status de la última conversación del par ({@code botId},
     * {@code sessionId}).
     *
     * <p><b>Método de contexto de sistema</b> — hoy sólo lo invoca
     * {@code WhatsappService.processIncomingMessage} desde el webhook público
     * de Meta Cloud API, donde <i>no existe usuario autenticado</i>. El tenant ya
     * viene resuelto upstream vía {@code WhatsappConfig.phoneNumberId} →
     * {@code bot.tenantId}, así que no podemos (ni debemos) correr
     * {@code TenantAccessValidator} aquí: tiraría 401 en producción.
     *
     * <p><b>¡No exponer como endpoint!</b> Si en el futuro se necesita un
     * endpoint HTTP para cambiar el status de una conversación, debe:
     * <ol>
     *   <li>Validar el {@code bot_id} con {@link TenantAccessValidator#assertCanAccessBot}
     *       ANTES de llamar a este método, o</li>
     *   <li>Crear un método separado (p.ej. {@code updateStatusAsUser}) que
     *       envuelva esta lógica con la verificación.</li>
     * </ol>
     */
    @Transactional
    public void updateStatus(UUID botId, String sessionId, ConversationStatus newStatus) {
        conversationRepository.findTopByBotIdAndSessionIdOrderByCreatedAtDesc(botId, sessionId)
                .ifPresent(conversation -> {
                    conversation.setStatus(newStatus);
                    conversationRepository.save(conversation);
                });
    }
}
