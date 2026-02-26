package com.chatbotsaas.chatbot_saas.chat.service;

import com.chatbotsaas.chatbot_saas.bot.repository.BotRepository;
import com.chatbotsaas.chatbot_saas.chat.dto.request.ChatRequestDto;
import com.chatbotsaas.chatbot_saas.chat.dto.response.ChatResponseDto;
import com.chatbotsaas.chatbot_saas.conversation.dto.request.ConversationRequestDto;
import com.chatbotsaas.chatbot_saas.conversation.service.ConversationService;
import com.chatbotsaas.chatbot_saas.integration.PythonRagClient;
import com.chatbotsaas.chatbot_saas.integration.dto.request.ChatRequest;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private final ConversationService conversationService;
    private final BotRepository botRepository;
    private final PythonRagClient pythonRagClient;

    public ChatService(ConversationService conversationService, BotRepository botRepository, PythonRagClient pythonRagClient) {
        this.conversationService = conversationService;
        this.botRepository = botRepository;
        this.pythonRagClient = pythonRagClient;
    }

    @Transactional
    public ChatResponseDto sendMessage(ChatRequestDto request) {
        botRepository.findById(request.getBotId()).orElseThrow(
                () -> new AppException("Bot not found", HttpStatus.NOT_FOUND)
        );

        // Save the user message in Conversation table
        conversationService.saveMessage(ConversationRequestDto.builder()
                .botId(request.getBotId())
                .message(request.getMessage())
                .sessionId(request.getSessionId())
                .role("USER")
                .build()
        );

        //Send the message to Python via PythonRagClient
        String response = pythonRagClient.chat(ChatRequest.builder()
                .botId(request.getBotId())
                .message(request.getMessage())
                .sessionId(request.getSessionId())
                .build()
        );

        // Save the AI response in Conversation
        conversationService.saveMessage(ConversationRequestDto.builder()
                .botId(request.getBotId())
                .message(response)
                .sessionId(request.getSessionId())
                .role("ASSISTANT")
                .build()
        );

        return ChatResponseDto.builder()
                .sessionId(request.getSessionId())
                .response(response)
                .build();
    }
}
