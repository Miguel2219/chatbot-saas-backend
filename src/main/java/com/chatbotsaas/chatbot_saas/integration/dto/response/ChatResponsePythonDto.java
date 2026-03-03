package com.chatbotsaas.chatbot_saas.integration.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatResponsePythonDto {

    @JsonProperty(value = "response")
    private String response;
}
