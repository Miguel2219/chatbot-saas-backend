package com.chatbotsaas.chatbot_saas.whatsapp.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@Getter
@Builder
@AllArgsConstructor
public class WhatsappConfigResponseDto {

    private UUID id;

    @JsonProperty(value = "bot_id")
    private UUID botId;

    @JsonProperty(value = "phone_number_id")
    private String phoneNumberId;

    @JsonProperty(value = "is_active")
    private Boolean isActive;
}
