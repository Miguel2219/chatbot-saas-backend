package com.chatbotsaas.chatbot_saas.document.service;

import com.chatbotsaas.chatbot_saas.bot.repository.BotRepository;
import com.chatbotsaas.chatbot_saas.document.dto.response.DocumentResponseDto;
import com.chatbotsaas.chatbot_saas.document.entity.Document;
import com.chatbotsaas.chatbot_saas.document.repository.DocumentRepository;
import com.chatbotsaas.chatbot_saas.integration.PythonRagClient;
import com.chatbotsaas.chatbot_saas.integration.dto.request.DeleteDocumentRequest;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.upload-dir}")
    private String uploadDir;

    public DocumentService(DocumentRepository documentRepository, BotRepository botRepository, PythonRagClient pythonRagClient) {
        this.documentRepository = documentRepository;
        this.botRepository = botRepository;
        this.pythonRagClient = pythonRagClient;
    }

    @Transactional(rollbackFor = IOException.class)
    public List<DocumentResponseDto> uploadDocument(UUID botId, List<MultipartFile> files) throws IOException {
        botRepository.findById(botId).orElseThrow(
                () -> new IllegalArgumentException("Bot not found")
        );
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
    public List<DocumentResponseDto> getDocumentsByBotId(UUID botId) {
        List<Document> documents = documentRepository.findByBotId(botId);
        return documents.stream().map(document ->
            DocumentResponseDto.builder()
                    .documentId(document.getDocumentId())
                    .fileName(document.getFileName())
                    .fileSize(document.getFileSize())
                    .fileType(document.getFileType())
                    .createdAt(document.getCreatedAt())
                    .build()
        ).toList();
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId).orElseThrow(
                () -> new AppException("Document not found", HttpStatus.NOT_FOUND)
        );

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
