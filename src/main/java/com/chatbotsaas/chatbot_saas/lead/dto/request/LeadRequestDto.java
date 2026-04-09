package com.chatbotsaas.chatbot_saas.lead.dto.request;

import com.chatbotsaas.chatbot_saas.lead.enums.LeadChannel;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class LeadRequestDto {

    @JsonProperty(value = "bot_id")
    private UUID botId;

    @JsonProperty(value = "session_id")
    private String sessionId;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "phone")
    private String phone;

    @JsonProperty(value = "email")
    private String email;

    @JsonProperty(value = "request_detail")
    private String requestDetail;

    @JsonProperty(value = "channel")
    private LeadChannel leadChannel;

}
