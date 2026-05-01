package com.chatbotsaas.chatbot_saas.lead.controller;

import com.chatbotsaas.chatbot_saas.lead.dto.response.LeadResponseDto;
import com.chatbotsaas.chatbot_saas.lead.enums.LeadStatus;
import com.chatbotsaas.chatbot_saas.lead.service.LeadService;
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

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lead")
public class LeadController {
    private final LeadService leadService;

//    @PostMapping("/")
//    @PreAuthorize("@permissionChecker.hasPermission('leads', 'view')")
//    public ResponseEntity<LeadResponseDto> saveLead(
//            @Valid @RequestBody LeadRequestDto lead) {
//        return new ResponseEntity<>(leadService.saveLead(lead), HttpStatus.CREATED);
//    }

    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('leads', 'view')")
    public ResponseEntity<Page<LeadResponseDto>> getLeads(
            @RequestParam(required = false) UUID botId,
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String order_by,
            @RequestParam(defaultValue = "desc") String order
    ) {
        Pageable pageable = order.equalsIgnoreCase("desc")
                ? PageRequest.of(offset, limit, Sort.by(order_by).descending())
                : PageRequest.of(offset, limit, Sort.by(order_by).ascending());
        return new ResponseEntity<>(leadService.getLeads(botId, tenantId, pageable), HttpStatus.OK);
    }

    @PutMapping("/update_status/{lead_id}")
    @PreAuthorize("@permissionChecker.hasPermission('leads', 'edit')")
    public ResponseEntity<HttpStatus> updateStatus(
            @Valid @RequestBody LeadStatus status,
            @PathVariable(name = "lead_id") UUID leadId
            ) {
        leadService.updateLeadStatus(leadId, status);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
