package com.chatbotsaas.chatbot_saas.whatsapp.service;

import com.chatbotsaas.chatbot_saas.chat.dto.request.ChatRequestDto;
import com.chatbotsaas.chatbot_saas.chat.dto.response.ChatResponseDto;
import com.chatbotsaas.chatbot_saas.chat.service.ChatService;
import com.chatbotsaas.chatbot_saas.conversation.enums.ConversationStatus;
import com.chatbotsaas.chatbot_saas.conversation.repository.ConversationRepository;
import com.chatbotsaas.chatbot_saas.conversation.service.ConversationService;
import com.chatbotsaas.chatbot_saas.lead.dto.response.LeadDataDto;
import com.chatbotsaas.chatbot_saas.lead.service.LeadService;
import com.chatbotsaas.chatbot_saas.whatsapp.dto.WhatsappWebhookPayloadDto;
import com.chatbotsaas.chatbot_saas.whatsapp.dto.request.SendMessageRequestDto;
import com.chatbotsaas.chatbot_saas.whatsapp.entity.WhatsappConfig;
import com.chatbotsaas.chatbot_saas.whatsapp.repository.WhatsappConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j  // ← add this
@Service
public class WhatsappService {
    private final WebClient.Builder webClientBuilder;
    private final WhatsappConfigRepository whatsappConfigRepository;
    private final ConversationRepository conversationRepository;
    private final ChatService chatService;
    private final LeadService leadService;
    private final ConversationService conversationService;

    public WhatsappService(WebClient.Builder webClientBuilder, WhatsappConfigRepository whatsappConfigRepository, ConversationRepository conversationRepository, ChatService chatService, LeadService leadService, ConversationService conversationService) {
        this.webClientBuilder = webClientBuilder;
        this.whatsappConfigRepository = whatsappConfigRepository;
        this.conversationRepository = conversationRepository;
        this.chatService = chatService;
        this.leadService = leadService;
        this.conversationService = conversationService;
    }

    private void sendToWhatsapp(String apiKey, SendMessageRequestDto request) {
        //TODO - Usar para prueba
        if (apiKey.equals("test-api")) {
            log.info("[WhatsApp MOCK] Would send: {}", request.getText().getBody());
            return;
        }

        webClientBuilder.build()
                .post()
                .uri("https://waba.360dialog.io/v1/messages")
                .header("D360-API-KEY", apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public void sendMessage(String apiKey, String toPhone, String message){
        SendMessageRequestDto request = SendMessageRequestDto.builder()
                .messagingProduct("whatsapp")
                .to(toPhone)
                .type("text")
                .text(new SendMessageRequestDto.TextBody(message))
                .build();

        sendToWhatsapp(apiKey, request);
    }

    public void sendInternalNote(String apiKey, String toPhone, String note){
        SendMessageRequestDto request = SendMessageRequestDto.builder()
                .messagingProduct("whatsapp")
                .to(toPhone)
                .type("note")
                .text(new SendMessageRequestDto.TextBody(note))
                .build();

        sendToWhatsapp(apiKey, request);
    }

    @Async
    public void processIncomingMessage(WhatsappWebhookPayloadDto payload) {
        var value = payload.getEntry().get(0).getChanges().get(0).getValue();
        var messages = value.getMessages();
        var metadata = value.getMetadata();

        if (messages == null || messages.isEmpty()) return;
        var message = messages.get(0);
        if (!"text".equals(message.getType())) return;

        String phoneNumberId = metadata.getPhoneNumberId();
        String userPhone = message.getFrom();
        String messageText = message.getText().getBody();

        WhatsappConfig config = whatsappConfigRepository.findByPhoneNumberId(phoneNumberId)
                .orElse(null);
        if (config == null) return;

        boolean botShouldRespond = conversationRepository.findTopByBotIdAndSessionIdOrderByCreatedAtDesc(
                config.getBot().getBotId(), userPhone)
                .map(conv -> conv.getStatus() == ConversationStatus.BOT_ACTIVE)
                .orElse(true);

        if (!botShouldRespond) return;

        ChatResponseDto response = chatService.sendMessage(
                ChatRequestDto.builder()
                        .botId(config.getBot().getBotId())
                        .sessionId(userPhone)
                        .message(messageText)
                        .build()
        );

        log.info("[Python MOCKUP] Wrote response: {}", response);

        // Step 1 — Send AI response to user
        try {
            sendMessage(config.getApiKey(), userPhone, response.getResponse());
        } catch (Exception e) {
            log.error("[WhatsApp] Failed to send message to {}: {}", userPhone, e.getMessage());
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

        // Step 3 — If cede_control, update status and send internal note
        if (Boolean.TRUE.equals(response.getCedeControl())) {
            conversationService.updateStatus(
                    config.getBot().getBotId(),
                    userPhone,
                    ConversationStatus.PENDING_HUMAN
            );

            String note;
            if (Boolean.TRUE.equals(response.getLeadCaptured()) && response.getLeadData() != null) {
                LeadDataDto lead = response.getLeadData();
                note = "🔴 Intervention required.\n"
                        + "🤖 Bot has finished this conversation.\n"
                        + "📋 Summary: " + (response.getRequestDetail() != null ? response.getRequestDetail() : "No summary available") + "\n"
                        + "👤 Contact: " + lead.getName()
                        + " | " + (lead.getPhone() != null ? lead.getPhone() : "N/A")
                        + " | " + (lead.getEmail() != null ? lead.getEmail() : "N/A") + "\n"
                        + "Please take over this conversation.";
            } else {
                note = "🔴 Intervention required.\n"
                        + "🤖 Bot could not answer from available context.\n"
                        + (response.getRequestDetail() != null ? "📋 Summary: " + response.getRequestDetail() + "\n" : "")
                        + "User needs assistance beyond the bot's knowledge.\n"
                        + "Please take over this conversation.";
            }

            sendInternalNote(config.getApiKey(), userPhone, note);
        }

    }


}
