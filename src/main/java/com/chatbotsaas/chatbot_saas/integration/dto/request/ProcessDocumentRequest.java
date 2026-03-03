package com.chatbotsaas.chatbot_saas.integration.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProcessDocumentRequest {

    @JsonProperty(value = "bot_id")
    private UUID botId;

    @JsonProperty(value = "file_id")
    private UUID documentId;

    @JsonProperty(value = "file_path")
    private String filePath;
}
