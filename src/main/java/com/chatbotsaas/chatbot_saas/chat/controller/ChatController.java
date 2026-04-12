package com.chatbotsaas.chatbot_saas.chat.controller;

import com.chatbotsaas.chatbot_saas.chat.dto.request.ChatRequestDto;
import com.chatbotsaas.chatbot_saas.chat.dto.response.ChatResponseDto;
import com.chatbotsaas.chatbot_saas.chat.service.ChatService;
import com.chatbotsaas.chatbot_saas.lead.dto.request.LeadRequestDto;
import com.chatbotsaas.chatbot_saas.lead.enums.LeadChannel;
import com.chatbotsaas.chatbot_saas.lead.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;
    private final LeadService leadService;

    @PostMapping()
    public ResponseEntity<ChatResponseDto> sendMessage(
            @Valid @RequestBody ChatRequestDto request
    ) {
        ChatResponseDto response = chatService.sendMessage(request);

        if (Boolean.TRUE.equals(response.getLeadCaptured())
                && response.getLeadData() != null
                && response.getLeadData().getName() != null) {
            leadService.saveLead(LeadRequestDto.builder()
                    .botId(request.getBotId())
                    .sessionId(request.getSessionId())
                    .name(response.getLeadData().getName())
                    .phone(response.getLeadData().getPhone())
                    .email(response.getLeadData().getEmail())
                    .requestDetail(response.getRequestDetail())
                    .leadChannel(LeadChannel.WIDGET)
                    .build());
        }

        return new ResponseEntity<>(ChatResponseDto.builder()
                .sessionId(response.getSessionId())
                .response(response.getResponse())
                .build(), HttpStatus.OK);
    }
}
