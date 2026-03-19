package com.chatbotsaas.chatbot_saas.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Builder
@Getter
public class PersonResponse {

    @JsonProperty(value = "user_id")
    private UUID userId;

    @JsonProperty(value = "full_name")
    private String fullName;
}
