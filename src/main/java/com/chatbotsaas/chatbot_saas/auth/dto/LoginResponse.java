package com.chatbotsaas.chatbot_saas.auth.dto;

import com.chatbotsaas.chatbot_saas.module.dto.ModulePermissionDto;
import com.chatbotsaas.chatbot_saas.user.dto.response.UserResponse;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    @JsonProperty("token")
    private String token;

    @JsonProperty("type")
    private String type = "Bearer";

    @JsonProperty("user")
    private UserResponse user;

    @JsonProperty("implementation_type")
    private String implementationType;

    @JsonProperty("modules")
    private List<ModulePermissionDto> modules;

    public LoginResponse(String token) {
        this.token = token;
        this.type = "Bearer";
    }
}