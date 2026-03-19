package com.chatbotsaas.chatbot_saas.lead.controller;

import com.chatbotsaas.chatbot_saas.lead.dto.request.LeadRequestDto;
import com.chatbotsaas.chatbot_saas.lead.dto.response.LeadResponseDto;
import com.chatbotsaas.chatbot_saas.lead.enums.LeadStatus;
import com.chatbotsaas.chatbot_saas.lead.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lead/")
public class LeadController {
    private final LeadService leadService;

    @PostMapping
    public ResponseEntity<LeadResponseDto> saveLead(
            @Valid @RequestBody LeadRequestDto lead) {
        return new ResponseEntity<>(leadService.saveLead(lead), HttpStatus.CREATED);
    }

    @GetMapping("/get_leads_by_bot/{bot_id}")
    public ResponseEntity<List<LeadResponseDto>> getBotsByTenantId(
            @PathVariable(name = "bot_id") UUID botId
    ) {
        return new ResponseEntity<>(leadService.getLeadsByBot(botId), HttpStatus.OK);
    }

    @PutMapping("/update_status/{lead_id}")
    public ResponseEntity<HttpStatus> updateStatus(
            @Valid @RequestBody LeadStatus status,
            @PathVariable(name = "lead_id") UUID leadId
            ) {
        leadService.updateLeadStatus(leadId, status);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
