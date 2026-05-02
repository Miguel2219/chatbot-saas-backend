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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
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
    private final S3Client s3Client;

    @Value("${app.storage.bucket}")
    private String bucket;

    public DocumentService(DocumentRepository documentRepository, BotRepository botRepository,
                           PythonRagClient pythonRagClient, AuthService authService,
                           TenantAccessValidator tenantAccessValidator, S3Client s3Client) {
        this.documentRepository = documentRepository;
        this.botRepository = botRepository;
        this.pythonRagClient = pythonRagClient;
        this.authService = authService;
        this.tenantAccessValidator = tenantAccessValidator;
        this.s3Client = s3Client;
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

        List<DocumentResponseDto> documentsResponse = new ArrayList<>();
        for (MultipartFile file : files) {
            // Convención del key en R2: bots/{botId}/{uuid}_{originalFilename}
            // - El prefix por bot facilita auditar/limpiar todo de un bot
            //   con un solo prefix listObjects + deleteObjects.
            // - El UUID prepend evita colisiones si dos archivos suben con
            //   el mismo nombre original.
            String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String s3Key = "bots/" + botId + "/" + uniqueFileName;

            // Upload a R2. RequestBody.fromInputStream evita cargar el archivo
            // entero en memoria — stream directo del MultipartFile a R2.
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(
                    putRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            // file_path ahora guarda la S3 key (no un path absoluto). El
            // nombre de la columna se mantiene por compatibilidad de schema.
            Document saved = documentRepository.save(
                    Document.builder()
                            .fileName(file.getOriginalFilename())
                            .filePath(s3Key)
                            .fileType(file.getContentType())
                            .fileSize(file.getSize())
                            .botId(botId)
                            .build()
            );

            // El RAG ahora descarga directo de R2 con la s3_key.
            pythonRagClient.processDocument(ProcessDocumentRequest.builder()
                    .botId(botId)
                    .documentId(saved.getDocumentId())
                    .s3Key(s3Key)
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

        // Sending request to RAG to delete document chunks from ChromaDB
        pythonRagClient.deleteDocument(DeleteDocumentRequest.builder()
                .documentId(documentId)
                .botId(document.getBotId())
                .build()
        );

        // Borrar el objeto de R2. La columna `file_path` ahora guarda la
        // S3 key (ej: "bots/{botId}/{uuid}_{filename}").
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(document.getFilePath())
                    .build();
            s3Client.deleteObject(deleteRequest);
        } catch (Exception e) {
            // No abortamos el delete por una falla en R2: si el objeto ya
            // no existe (cleanup previo, race) o R2 está caído, la metadata
            // de BD igual se borra para no dejar al user con un row que no
            // puede ver. Logueamos y seguimos.
            throw new AppException("Failed to delete file from storage: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        documentRepository.deleteById(documentId);
    }
}
