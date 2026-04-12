package com.chatbotsaas.chatbot_saas.whatsapp.controller;

import com.chatbotsaas.chatbot_saas.whatsapp.dto.WhatsappWebhookPayloadDto;
import com.chatbotsaas.chatbot_saas.whatsapp.service.WhatsappService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/webhook/whatsapp")
public class WhatsappWebhookController {
    private final WhatsappService whatsappService;

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
    public ResponseEntity<Void> receiveMessage(@RequestBody WhatsappWebhookPayloadDto payload) {
        whatsappService.processIncomingMessage(payload);
        return ResponseEntity.ok().build();
    }
}
