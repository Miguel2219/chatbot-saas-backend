package com.chatbotsaas.chatbot_saas.whatsapp.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class CreateWhatsappConfigRequest {
    @JsonProperty("phone_number_id")
    private String phoneNumberId;

    @JsonProperty("access_token")
    private String accessToken;
}
