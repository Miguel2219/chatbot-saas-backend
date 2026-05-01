package com.chatbotsaas.chatbot_saas.chat.service;

import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.bot.repository.BotRepository;
import com.chatbotsaas.chatbot_saas.chat.dto.request.ChatRequestDto;
import com.chatbotsaas.chatbot_saas.chat.dto.response.ChatResponseDto;
import com.chatbotsaas.chatbot_saas.conversation.dto.request.ConversationRequestDto;
import com.chatbotsaas.chatbot_saas.conversation.service.ConversationService;
import com.chatbotsaas.chatbot_saas.integration.PythonRagClient;
import com.chatbotsaas.chatbot_saas.integration.dto.request.ChatMessageDto;
import com.chatbotsaas.chatbot_saas.integration.dto.request.ChatRequest;
import com.chatbotsaas.chatbot_saas.integration.dto.response.ChatResponsePythonDto;
import com.chatbotsaas.chatbot_saas.quota.service.QuotaService;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {
    private final ConversationService conversationService;
    private final BotRepository botRepository;
    private final PythonRagClient pythonRagClient;
    private final QuotaService quotaService;

    public ChatService(
            ConversationService conversationService,
            BotRepository botRepository,
            PythonRagClient pythonRagClient,
            QuotaService quotaService
    ) {
        this.conversationService = conversationService;
        this.botRepository = botRepository;
        this.pythonRagClient = pythonRagClient;
        this.quotaService = quotaService;
    }

    @Transactional
    public ChatResponseDto sendMessage(ChatRequestDto request) {
        Bot bot = botRepository.findById(request.getBotId()).orElseThrow(
                () -> new AppException("Bot not found", HttpStatus.NOT_FOUND)
        );

        // Reconstruye el chat_history ANTES de guardar el mensaje actual del
        // usuario. Si lo hiciéramos después, el SELECT incluiría el mensaje
        // recién insertado y el RAG lo recibiría duplicado (también va como
        // `message` en el ChatRequest). Fase 5: el RAG es stateless — la
        // fuente de verdad del histórico es la tabla `conversations`.
        List<ChatMessageDto> chatHistory = conversationService.getRecentHistoryForRag(
                request.getBotId(), request.getSessionId()
        );

        // Save the user message in Conversation table
        conversationService.saveMessage(ConversationRequestDto.builder()
                .botId(request.getBotId())
                .message(request.getMessage())
                .sessionId(request.getSessionId())
                .role("USER")
                .build()
        );

        //Send the message to Python via PythonRagClient
        ChatResponsePythonDto response = pythonRagClient.chat(ChatRequest.builder()
                .botId(request.getBotId())
                .message(request.getMessage())
                .sessionId(request.getSessionId())
                .systemPrompt(bot.getSystemPrompt())
                .chatHistory(chatHistory)
                .build()
        );

        // Save the AI response in Conversation
        conversationService.saveMessage(ConversationRequestDto.builder()
                .botId(request.getBotId())
                .message(response.getResponse())
                .sessionId(request.getSessionId())
                .role("ASSISTANT")
                .build()
        );

        // Hook del sistema de cuotas — cuenta "una conversación" si es la primera vez que se
        // ve esta (tenant, session, día) dentro del ciclo vigente del tenant. Participa en esta
        // misma transacción: si algo hizo rollback antes, no se cuenta y no se dispara email
        // (el listener escucha AFTER_COMMIT).
        quotaService.registerConversationIfFirst(bot.getTenant().getId(), request.getSessionId());

        return ChatResponseDto.builder()
                .sessionId(request.getSessionId())
                .response(response.getResponse())
                .leadCaptured(response.getLeadCaptured())
                .leadData(response.getLeadData())
                .requestDetail(response.getRequestDetail())
                .cedeControl(response.getCedeControl())
                .build();
    }
}
