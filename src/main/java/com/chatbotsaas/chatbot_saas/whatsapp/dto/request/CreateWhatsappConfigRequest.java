package com.chatbotsaas.chatbot_saas.whatsapp.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class CreateWhatsappConfigRequest {
    private String phoneNumberId;
    private String apiKey;
}
