package com.chatbotsaas.chatbot_saas.document.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@AllArgsConstructor
public class DocumentResponseDto {

    @JsonProperty(value = "document_id")
    private UUID documentId;

    @JsonProperty(value = "file_name")
    private String fileName;

    @JsonProperty(value = "file_size")
    private Long fileSize;

    @JsonProperty(value = "file_type")
    private String fileType;

    @JsonProperty(value = "created_at")
    private LocalDateTime createdAt;
}
