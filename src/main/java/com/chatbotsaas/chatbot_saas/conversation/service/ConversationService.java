package com.chatbotsaas.chatbot_saas.conversation.service;

import com.chatbotsaas.chatbot_saas.bot.repository.BotRepository;
import com.chatbotsaas.chatbot_saas.conversation.dto.request.ConversationRequestDto;
import com.chatbotsaas.chatbot_saas.conversation.dto.response.ConversationResponseDto;
import com.chatbotsaas.chatbot_saas.conversation.entity.Conversation;
import com.chatbotsaas.chatbot_saas.conversation.enums.ConversationStatus;
import com.chatbotsaas.chatbot_saas.conversation.repository.ConversationRepository;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {
    private final BotRepository botRepository;
    private final ConversationRepository conversationRepository;

    public ConversationService(BotRepository botRepository, ConversationRepository conversationRepository) {
        this.botRepository = botRepository;
        this.conversationRepository = conversationRepository;
    }

    private ConversationResponseDto toResponseDto (Conversation conversation) {
        return ConversationResponseDto.builder()
                .conversationId(conversation.getId())
                .botId(conversation.getBotId())
                .sessionId(conversation.getSessionId())
                .role(conversation.getRole())
                .message(conversation.getMessage())
                .createdAt(conversation.getCreatedAt())
                .build();
    }

    @Transactional
    public ConversationResponseDto saveMessage(ConversationRequestDto request) {
        botRepository.findById(request.getBotId()).orElseThrow(
                () -> new IllegalArgumentException("Bot not found")
        );
        Conversation conversation = conversationRepository.save(
                    Conversation.create(
                            request.getBotId(),
                            request.getSessionId(),
                            request.getRole(),
                            request.getMessage()
                    )
        );
        return toResponseDto(conversation);
    }

    @Transactional
    public Page<ConversationResponseDto> getConversationByBot(UUID botId, Pageable pageable) {
        botRepository.findById(botId).orElseThrow(
                () -> new IllegalArgumentException("Bot not found")
        );
        return conversationRepository.findByBotIdOrderByCreatedAtAsc(botId, pageable).map(this::toResponseDto);
    }

    @Transactional
    public List<ConversationResponseDto> getConversationBySession(String sessionId) {
        if (!conversationRepository.existsBySessionId(sessionId)) {
            throw new AppException("session not found", HttpStatus.NOT_FOUND);
        }
        List<Conversation> conversations = conversationRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        return conversations.stream().map(
                this::toResponseDto
        ).toList();
    }

    @Transactional
    public void updateStatus(UUID botId, String sessionId, ConversationStatus newStatus) {
        conversationRepository.findTopByBotIdAndSessionIdOrderByCreatedAtDesc(botId, sessionId)
                .ifPresent(conversation -> {
                    conversation.setStatus(newStatus);
                    conversationRepository.save(conversation);
                });
    }
}
