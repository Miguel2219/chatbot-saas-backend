package com.chatbotsaas.chatbot_saas.integration.dto.request;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProcessDocumentRequest {
    private UUID botId;
    private String filePath;
}
