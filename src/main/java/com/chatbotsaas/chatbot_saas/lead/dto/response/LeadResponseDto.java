package com.chatbotsaas.chatbot_saas.lead.dto.response;

import com.chatbotsaas.chatbot_saas.lead.enums.LeadStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class LeadResponseDto {

    @JsonProperty(value = "id")
    private UUID id;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "phone")
    private String phone;

    @JsonProperty(value = "email")
    private String email;

    @JsonProperty(value = "status")
    private LeadStatus status;

    @JsonProperty(value = "assigned_adviser_id")
    private UUID assignedAdviserId;

    @JsonProperty(value = "created_at")
    private LocalDateTime createdAt;
}
