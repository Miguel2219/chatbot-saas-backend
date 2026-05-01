package com.chatbotsaas.chatbot_saas.whatsapp.service;

import com.chatbotsaas.chatbot_saas.chat.dto.request.ChatRequestDto;
import com.chatbotsaas.chatbot_saas.chat.dto.response.ChatResponseDto;
import com.chatbotsaas.chatbot_saas.chat.service.ChatService;
import com.chatbotsaas.chatbot_saas.conversation.enums.ConversationStatus;
import com.chatbotsaas.chatbot_saas.conversation.repository.ConversationRepository;
import com.chatbotsaas.chatbot_saas.conversation.service.ConversationService;
import com.chatbotsaas.chatbot_saas.lead.service.LeadService;
import com.chatbotsaas.chatbot_saas.whatsapp.dto.WhatsappWebhookPayloadDto;
import com.chatbotsaas.chatbot_saas.whatsapp.dto.request.SendMessageRequestDto;
import com.chatbotsaas.chatbot_saas.whatsapp.entity.WhatsappConfig;
import com.chatbotsaas.chatbot_saas.whatsapp.repository.WhatsappConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class WhatsappService {

    private final String graphApiVersion;

    private final WebClient.Builder webClientBuilder;
    private final WhatsappConfigRepository whatsappConfigRepository;
    private final ConversationRepository conversationRepository;
    private final ChatService chatService;
    private final LeadService leadService;
    private final ConversationService conversationService;

    public WhatsappService(
            @Value("${app.whatsapp.graph-api-version}") String graphApiVersion,
            WebClient.Builder webClientBuilder,
            WhatsappConfigRepository whatsappConfigRepository,
            ConversationRepository conversationRepository,
            ChatService chatService,
            LeadService leadService,
            ConversationService conversationService
    ) {
        this.graphApiVersion = graphApiVersion;
        this.webClientBuilder = webClientBuilder;
        this.whatsappConfigRepository = whatsappConfigRepository;
        this.conversationRepository = conversationRepository;
        this.chatService = chatService;
        this.leadService = leadService;
        this.conversationService = conversationService;
    }


    private void sendToWhatsapp(String accessToken, String phoneNumberId, SendMessageRequestDto request) {
        String url = "https://graph.facebook.com/" + graphApiVersion + "/" + phoneNumberId + "/messages";

        webClientBuilder.build()
                .post()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }


    public void sendMessage(String accessToken, String phoneNumberId, String toPhone, String message) {
        SendMessageRequestDto request = SendMessageRequestDto.builder()
                .messagingProduct("whatsapp")
                .recipientType("individual")
                .to(sanitizePhone(toPhone))
                .type("text")
                .text(new SendMessageRequestDto.TextBody(message))
                .build();

        sendToWhatsapp(accessToken, phoneNumberId, request);
    }

    private static String sanitizePhone(String phone) {
        return phone == null ? null : phone.replace("+", "");
    }

    /**
     * Procesa un mensaje entrante del webhook de Meta WhatsApp Cloud API.
     *
     * <p><b>Contexto de seguridad multi-tenant</b>: este método corre en
     * un hilo {@code @Async} disparado desde
     * {@code WhatsappWebhookController}. <i>NO existe usuario
     * autenticado</i> en este flujo — el tenant se deriva
     * implícitamente del {@code WhatsappConfig} encontrado por
     * {@code phoneNumberId} (cada config pertenece a un único bot, y
     * cada bot a un único tenant). Por eso este método NO invoca a
     * {@code TenantAccessValidator}.
     *
     * <p><b>Sobre `notifyLeadAssignee` y el plan WhatsApp-only</b>: en
     * el modelo de Zolvion los bots WhatsApp no tienen asesores
     * asignados. El path {@code LeadService.saveLeadWhatsapp} fija
     * {@code assignedAdviserId = null} y <b>NO llama a
     * notifyLeadAssignee</b> (a diferencia de {@code saveLead} para
     * leads del widget/web). Por lo tanto la captura de leads en este
     * flujo nunca puede fallar por ausencia de asesor — el riesgo no
     * aplica. Si en el futuro se introduce notificación para leads
     * WhatsApp, habrá que envolverla en try/catch para no romper el
     * flujo de envío del mensaje al cliente.
     */
    @Async
    public void processIncomingMessage(WhatsappWebhookPayloadDto payload) {
        var value = payload.getEntry().get(0).getChanges().get(0).getValue();
        var messages = value.getMessages();
        var metadata = value.getMetadata();

        if (messages == null || messages.isEmpty()) return;
        var message = messages.get(0);
        if (!"text".equals(message.getType())) return;

        String phoneNumberId = metadata.getPhoneNumberId();
        String userPhone     = message.getFrom();
        String messageText   = message.getText().getBody();
        String messageId     = message.getId();  // wamid.xxx — para trazabilidad

        log.info("[WhatsApp] Incoming wamid={} from={} phoneNumberId={}",
                messageId, userPhone, phoneNumberId);

        WhatsappConfig config = whatsappConfigRepository.findByPhoneNumberId(phoneNumberId)
                .orElse(null);
        if (config == null) {
            log.warn("[WhatsApp] No config found for phoneNumberId={} (wamid={})",
                    phoneNumberId, messageId);
            return;
        }

        boolean botShouldRespond = conversationRepository.findTopByBotIdAndSessionIdOrderByCreatedAtDesc(
                config.getBot().getBotId(), userPhone)
                .map(conv -> conv.getStatus() == ConversationStatus.BOT_ACTIVE)
                .orElse(true);

        if (!botShouldRespond) {
            log.info("[WhatsApp] Bot suppressed by status (wamid={} botId={})",
                    messageId, config.getBot().getBotId());
            return;
        }

        ChatResponseDto response = chatService.sendMessage(
                ChatRequestDto.builder()
                        .botId(config.getBot().getBotId())
                        .sessionId(userPhone)
                        .message(messageText)
                        .build()
        );

        log.info("[WhatsApp] RAG response wamid={} leadCaptured={} cedeControl={}",
                messageId, response.getLeadCaptured(), response.getCedeControl());

        // Decisión: si cede_control=true, el mensaje al cliente NO es la
        // respuesta del RAG sino el mensaje de escalación (fusiona la
        // nota interna en un solo mensaje visible para cliente + operador).
        String outboundMessage;
        if (Boolean.TRUE.equals(response.getCedeControl())) {
            String customerName = resolveCustomerName(response, payload);
            outboundMessage = buildEscalationMessage(customerName, response.getRequestDetail());
        } else {
            outboundMessage = response.getResponse();
        }

        // Step 1 — Send message to user
        try {
            sendMessage(config.getAccessToken(), phoneNumberId, userPhone, outboundMessage);
        } catch (Exception e) {
            log.error("[WhatsApp] Failed to send message wamid={} to={}: {}",
                    messageId, userPhone, e.getMessage());
            return;
        }

        // Step 2 — Save lead if captured
        if (Boolean.TRUE.equals(response.getLeadCaptured())
                && response.getLeadData() != null
                && response.getLeadData().getName() != null) {
            leadService.saveLeadWhatsapp(
                    config.getBot().getBotId(),
                    userPhone,
                    response.getLeadData(),
                    response.getRequestDetail()
            );
        }

        // Step 3 — If cede_control, update conversation status
        // (el mensaje ya fue enviado en Step 1)
        if (Boolean.TRUE.equals(response.getCedeControl())) {
            conversationService.updateStatus(
                    config.getBot().getBotId(),
                    userPhone,
                    ConversationStatus.PENDING_HUMAN
            );
        }
    }

    /**
     * Resuelve el nombre del cliente para el mensaje de escalación, con
     * fallback en cascada:
     * <ol>
     *   <li>{@code response.leadData.name} si el RAG capturó lead en este turno.</li>
     *   <li>{@code payload.entry[0].changes[0].value.contacts[0].profile.name}
     *       si Meta envió el nombre del perfil.</li>
     *   <li>{@code null} si no hay ninguno disponible (la línea
     *       "Cliente: ..." se omite del mensaje).</li>
     * </ol>
     */
    private String resolveCustomerName(ChatResponseDto response, WhatsappWebhookPayloadDto payload) {
        if (response.getLeadData() != null && response.getLeadData().getName() != null
                && !response.getLeadData().getName().isBlank()) {
            return response.getLeadData().getName();
        }
        try {
            var contacts = payload.getEntry().get(0).getChanges().get(0).getValue().getContacts();
            if (contacts != null && !contacts.isEmpty()) {
                var profile = contacts.get(0).getProfile();
                if (profile != null && profile.getName() != null && !profile.getName().isBlank()) {
                    return profile.getName();
                }
            }
        } catch (Exception ignored) {
            // Defensa contra payloads malformados — caemos al fallback null.
        }
        return null;
    }

    private String buildEscalationMessage(String customerName, String requestDetail) {
        StringBuilder sb = new StringBuilder("🔴 ASESOR REQUERIDO\n");
        if (customerName != null && !customerName.isBlank()) {
            sb.append("Cliente: ").append(customerName).append('\n');
        }
        if (requestDetail != null && !requestDetail.isBlank()) {
            sb.append("Solicitud: ").append(requestDetail).append('\n');
        }
        sb.append("Una persona te atenderá en breve.").append('\n');
        sb.append("Por favor espera, no es necesario respondet este mensaje.");
        return sb.toString();
    }
}
