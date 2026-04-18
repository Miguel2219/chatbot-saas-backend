package com.chatbotsaas.chatbot_saas.conversation.controller;

import com.chatbotsaas.chatbot_saas.conversation.dto.request.ConversationRequestDto;
import com.chatbotsaas.chatbot_saas.conversation.dto.response.ConversationResponseDto;
import com.chatbotsaas.chatbot_saas.conversation.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ConversationResponseDto> saveConversations (
            @Valid @RequestBody ConversationRequestDto requestDto
    ) {
        return new ResponseEntity<>(conversationService.saveMessage(requestDto), HttpStatus.CREATED);
    }

    @GetMapping("/bot/{bot_id}")
    public ResponseEntity<Page<ConversationResponseDto>> getConversationsByBot(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String order_by,
            @RequestParam(defaultValue = "desc") String order,
            @PathVariable(value = "bot_id") UUID botId
    )
    {
        Pageable pageable = order.equalsIgnoreCase("desc")
                ? PageRequest.of(offset, limit, Sort.by(order_by).descending())
                : PageRequest.of(offset, limit, Sort.by(order_by).ascending());

        return new ResponseEntity<>(conversationService.getConversationByBot(botId, pageable), HttpStatus.OK);
    }

    @GetMapping("/session/{session_id}")
    public ResponseEntity<List<ConversationResponseDto>> getConversationBySession(
            @PathVariable(name = "session_id") String sessionId
    ) {
        return new ResponseEntity<>(conversationService.getConversationBySession(sessionId), HttpStatus.OK);
    }

}
