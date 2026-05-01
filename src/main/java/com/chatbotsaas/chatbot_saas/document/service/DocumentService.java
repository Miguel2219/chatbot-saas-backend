package com.chatbotsaas.chatbot_saas.document.service;

import com.chatbotsaas.chatbot_saas.auth.service.AuthService;
import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.bot.repository.BotRepository;
import com.chatbotsaas.chatbot_saas.document.dto.response.DocumentResponseDto;
import com.chatbotsaas.chatbot_saas.document.entity.Document;
import com.chatbotsaas.chatbot_saas.document.repository.DocumentRepository;
import com.chatbotsaas.chatbot_saas.integration.PythonRagClient;
import com.chatbotsaas.chatbot_saas.integration.dto.request.DeleteDocumentRequest;
import com.chatbotsaas.chatbot_saas.integration.dto.request.ProcessDocumentRequest;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.shared.security.TenantAccessValidator;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final BotRepository botRepository;
    private final PythonRagClient pythonRagClient;
    private final AuthService authService;
    private final TenantAccessValidator tenantAccessValidator;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public DocumentService(DocumentRepository documentRepository, BotRepository botRepository, PythonRagClient pythonRagClient, AuthService authService, TenantAccessValidator tenantAccessValidator) {
        this.documentRepository = documentRepository;
        this.botRepository = botRepository;
        this.pythonRagClient = pythonRagClient;
        this.authService = authService;
        this.tenantAccessValidator = tenantAccessValidator;
    }

    @Transactional(rollbackFor = IOException.class)
    public List<DocumentResponseDto> uploadDocument(UUID botId, List<MultipartFile> files) throws IOException {
        Bot bot = botRepository.findById(botId).orElseThrow(
                () -> new IllegalArgumentException("Bot not found")
        );
        // Aislamiento multi-tenant — non-admin sólo puede subir a bots de su
        // propio tenant. Sin esto, cualquier caller con `documents:upload`
        // podía envenenar el índice RAG de otros tenants.
        tenantAccessValidator.assertCanAccessBot(bot);
        Path directory = Paths.get(uploadDir, botId.toString());
        Files.createDirectories(directory);
        List<DocumentResponseDto> documentsResponse = new ArrayList<>();
        for (MultipartFile file : files) {
            String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = directory.resolve(uniqueFileName);
            file.transferTo(filePath);
            Document saved = documentRepository.save(
                    Document.builder()
                            .fileName(file.getOriginalFilename())
                            .filePath(filePath.toString())
                            .fileType(file.getContentType())
                            .fileSize(file.getSize())
                            .botId(botId)
                            .build()
            );
            pythonRagClient.processDocument(ProcessDocumentRequest.builder()
                    .botId(botId)
                    .documentId(saved.getDocumentId())
                    .filePath(filePath.toAbsolutePath().toString())
                    .build());
            documentsResponse.add(DocumentResponseDto.builder()
                    .documentId(saved.getDocumentId())
                    .fileName(saved.getFileName())
                    .fileSize(saved.getFileSize())
                    .fileType(saved.getFileType())
                    .createdAt(saved.getCreatedAt())
                    .build());
        }
        return documentsResponse;
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponseDto> getDocuments(UUID botId, UUID tenantId, Pageable pageable) {
        User user = authService.getUserAuthenticated();
        if (user == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
        boolean isAdmin = user.isAdmin();
        UUID myTenantId = user.getTenant() != null ? user.getTenant().getId() : null;

        if (!isAdmin && tenantId != null && !tenantId.equals(myTenantId)) {
            throw new AppException("Forbidden", HttpStatus.FORBIDDEN);
        }

        Page<Document> documents;
        if (botId != null) {
            Bot bot = botRepository.findById(botId)
                    .orElseThrow(() -> new AppException("Bot not found", HttpStatus.NOT_FOUND));
            if (!isAdmin && !bot.getTenant().getId().equals(myTenantId)) {
                throw new AppException("Forbidden", HttpStatus.FORBIDDEN);
            }
            documents = documentRepository.findByBotId(botId, pageable);
        } else if (tenantId != null) {
            documents = documentRepository.findByTenantId(tenantId, pageable);
        } else if (isAdmin) {
            documents = documentRepository.findAll(pageable);
        } else {
            documents = documentRepository.findByTenantId(myTenantId, pageable);
        }
        return documents.map(document ->
                DocumentResponseDto.builder()
                        .documentId(document.getDocumentId())
                        .fileName(document.getFileName())
                        .fileSize(document.getFileSize())
                        .fileType(document.getFileType())
                        .createdAt(document.getCreatedAt())
                        .build()
        );
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId).orElseThrow(
                () -> new AppException("Document not found", HttpStatus.NOT_FOUND)
        );
        // Aislamiento multi-tenant — el Document tiene {@code bot_id} plano
        // (no tenant_id directo), así que resolvemos vía bot. Este guard
        // además protege el índice RAG de Python: sin él, un caller podía
        // borrar chunks ajenos.
        Bot documentBot = botRepository.findById(document.getBotId()).orElseThrow(
                () -> new AppException("Bot not found", HttpStatus.NOT_FOUND)
        );
        tenantAccessValidator.assertCanAccessBot(documentBot);

        //Sending request from python to delete document
        pythonRagClient.deleteDocument(DeleteDocumentRequest.builder()
                .documentId(documentId)
                .botId(document.getBotId())
                .build()
        );

        //Getting path from file
        try {
            Path filePath = Paths.get(document.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new AppException("Failed to delete file", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        documentRepository.deleteById(documentId);
    }
}
