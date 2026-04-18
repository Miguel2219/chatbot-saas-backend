package com.chatbotsaas.chatbot_saas.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class ChangePasswordRequest {

    @JsonProperty(value = "current_password")
    private String currentPassword;

    @JsonProperty(value = "new_password")
    private String newPassword;
}
