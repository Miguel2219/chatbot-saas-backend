package com.chatbotsaas.chatbot_saas.integration;

import com.chatbotsaas.chatbot_saas.integration.dto.request.ChatRequest;
import com.chatbotsaas.chatbot_saas.integration.dto.request.ProcessDocumentRequest;
import org.springframework.beans.factory.annotation.Value;
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
                .bodyToMono(Void.class)
                .block();
    }

    public String chat(ChatRequest request) {
        return webClient.post()
                .uri("/chat")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
