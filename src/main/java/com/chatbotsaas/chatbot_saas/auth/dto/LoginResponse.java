package com.chatbotsaas.chatbot_saas.auth.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private String type = "Bearer";

    public LoginResponse (String token) {
        this.token = token;
    }
}
