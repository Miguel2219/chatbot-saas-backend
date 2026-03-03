package com.chatbotsaas.chatbot_saas.integration.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeleteDocumentRequest {

    @JsonProperty(value = "bot_id")
    private UUID botId;

    @JsonProperty(value = "document_id")
    private UUID documentId;
}
