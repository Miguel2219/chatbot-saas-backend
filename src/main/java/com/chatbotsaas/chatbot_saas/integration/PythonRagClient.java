package com.chatbotsaas.chatbot_saas.integration;

import com.chatbotsaas.chatbot_saas.integration.dto.request.ChatRequest;
import com.chatbotsaas.chatbot_saas.integration.dto.request.DeleteDocumentRequest;
import com.chatbotsaas.chatbot_saas.integration.dto.request.ProcessDocumentRequest;
import com.chatbotsaas.chatbot_saas.integration.dto.response.ChatResponsePythonDto;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PythonRagClient {
    private final WebClient webClient;

    public PythonRagClient(@Value("${app.python-service-url}") String pythonUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(pythonUrl)
                .build();
    }

    public void processDocument(ProcessDocumentRequest request) {
        webClient.post()
                .uri("/process")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new AppException(body, HttpStatus.BAD_REQUEST))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new AppException("Python service error: "+ body, HttpStatus.INTERNAL_SERVER_ERROR))
                )
                .bodyToMono(Void.class)
                .block();
    }

    public ChatResponsePythonDto chat(ChatRequest request) {
        return webClient.post()
                .uri("/chat")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new AppException(body, HttpStatus.BAD_REQUEST))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .map(body -> new AppException("Python service error: "+ body, HttpStatus.INTERNAL_SERVER_ERROR))
                )
                .bodyToMono(ChatResponsePythonDto.class)
                .block();
    }

    public void deleteDocument(DeleteDocumentRequest request) {
        webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/delete-document")
                        .queryParam("botId", request.getBotId())
                        .queryParam("documentId", request.getDocumentId())
                        .build())
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
