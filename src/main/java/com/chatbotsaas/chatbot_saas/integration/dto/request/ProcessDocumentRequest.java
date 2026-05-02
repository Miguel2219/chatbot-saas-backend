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

    /**
     * Key del objeto en R2 (ej: "bots/{botId}/{uuid}_{filename}").
     * El RAG la usa para descargar el archivo del bucket vía boto3.
     */
    @JsonProperty(value = "s3_key")
    private String s3Key;
}
