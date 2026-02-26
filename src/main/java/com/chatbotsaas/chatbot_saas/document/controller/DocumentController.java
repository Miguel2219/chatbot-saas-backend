package com.chatbotsaas.chatbot_saas.document.controller;

import com.chatbotsaas.chatbot_saas.document.dto.response.DocumentResponseDto;
import com.chatbotsaas.chatbot_saas.document.service.DocumentService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentService documentService;

    @PostMapping(value = "/{bot_id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<DocumentResponseDto>> uploadDocument (
          @PathVariable(name = "bot_id") UUID botId,
          @RequestParam("files") @NotNull List<MultipartFile> files
    ) throws IOException {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.uploadDocument(botId,files ));
    }

    @GetMapping("/{bot_id}")
    public ResponseEntity<List<DocumentResponseDto>> getDocumentsByBotId(
            @PathVariable(name = "bot_id") UUID botId
    ) {
       return ResponseEntity.ok(documentService.getDocumentsByBotId(botId));
    }
}
