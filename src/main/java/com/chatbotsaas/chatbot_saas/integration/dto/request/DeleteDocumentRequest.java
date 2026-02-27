package com.chatbotsaas.chatbot_saas.integration.dto.request;

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
    private UUID botId;
    private UUID documentId;
}
