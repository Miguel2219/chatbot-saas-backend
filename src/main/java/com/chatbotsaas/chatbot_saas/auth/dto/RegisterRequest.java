package com.chatbotsaas.chatbot_saas.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class RegisterRequest {
    private String email;

    private String password;

    private String name;

    private String lastname;

    private String phone;

    @JsonProperty(value = "number_document")
    private String numberDocument;

    @JsonProperty("role_ids")
    private List<UUID> roleIds;

}
