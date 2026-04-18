package com.chatbotsaas.chatbot_saas.document.controller;

import com.chatbotsaas.chatbot_saas.document.dto.response.DocumentResponseDto;
import com.chatbotsaas.chatbot_saas.document.service.DocumentService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    public ResponseEntity<Page<DocumentResponseDto>> getDocumentsByBotId(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String order_by,
            @RequestParam(defaultValue = "desc") String order,
            @PathVariable(name = "bot_id") UUID botId
    ) {
        Pageable pageable = order.equalsIgnoreCase("desc")
                ? PageRequest.of(offset, limit, Sort.by(order_by).descending())
                : PageRequest.of(offset, limit, Sort.by(order_by).ascending());
       return new ResponseEntity<>(documentService.getDocumentsByBotId(botId, pageable), HttpStatus.OK);
    }

    @DeleteMapping("/{document_id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable(name = "document_id") UUID documentId
    ) {
        documentService.deleteDocument(documentId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
