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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService conversationService;

    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('conversations', 'view')")
    public ResponseEntity<ConversationResponseDto> saveConversations (
            @Valid @RequestBody ConversationRequestDto requestDto
    ) {
        return new ResponseEntity<>(conversationService.saveMessage(requestDto), HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('conversations', 'view')")
    public ResponseEntity<Page<ConversationResponseDto>> getConversations(
            @RequestParam(required = false) UUID botId,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String order_by,
            @RequestParam(defaultValue = "desc") String order
    )
    {
        Pageable pageable = order.equalsIgnoreCase("desc")
                ? PageRequest.of(offset, limit, Sort.by(order_by).descending())
                : PageRequest.of(offset, limit, Sort.by(order_by).ascending());

        return new ResponseEntity<>(conversationService.getConversations(botId, tenantId, pageable), HttpStatus.OK);
    }

    @GetMapping("/session/{session_id}")
    @PreAuthorize("@permissionChecker.hasPermission('conversations', 'view')")
    public ResponseEntity<List<ConversationResponseDto>> getConversationBySession(
            @PathVariable(name = "session_id") String sessionId
    ) {
        return new ResponseEntity<>(conversationService.getConversationBySession(sessionId), HttpStatus.OK);
    }

}
