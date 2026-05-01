package com.chatbotsaas.chatbot_saas.whatsapp.controller;

import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.whatsapp.dto.WhatsappWebhookPayloadDto;
import com.chatbotsaas.chatbot_saas.whatsapp.service.WhatsappService;
import com.chatbotsaas.chatbot_saas.whatsapp.util.MetaSignatureVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook/whatsapp")
public class WhatsappWebhookController {

    private final WhatsappService whatsappService;
    private final MetaSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    @Value("${app.whatsapp.verify-token}")
    private String verifyToken;

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge
    ) {
        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).build();
    }


    @PostMapping
    public ResponseEntity<Void> receiveMessage(
            @RequestBody byte[] rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature
    ) {
        if (!signatureVerifier.verify(rawBody, signature)) {
            log.warn("WhatsApp webhook: rejected — invalid signature (header_present={})",
                    signature != null);
            throw new AppException(
                    "Invalid webhook signature",
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_WEBHOOK_SIGNATURE"
            );
        }

        WhatsappWebhookPayloadDto payload;
        try {
            payload = objectMapper.readValue(rawBody, WhatsappWebhookPayloadDto.class);
        } catch (Exception e) {
            log.warn("WhatsApp webhook: failed to deserialize payload — {}", e.getMessage());
            throw new AppException(
                    "Invalid webhook payload",
                    HttpStatus.BAD_REQUEST,
                    "INVALID_WEBHOOK_PAYLOAD"
            );
        }

        whatsappService.processIncomingMessage(payload);
        return ResponseEntity.ok().build();
    }
}
