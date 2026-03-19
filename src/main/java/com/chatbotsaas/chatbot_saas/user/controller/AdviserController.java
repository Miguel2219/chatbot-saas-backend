package com.chatbotsaas.chatbot_saas.user.controller;

import com.chatbotsaas.chatbot_saas.user.dto.request.CreateAdviserRequestDto;
import com.chatbotsaas.chatbot_saas.user.dto.response.AdviserResponseDto;
import com.chatbotsaas.chatbot_saas.user.service.AdviserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/adviser/")
public class AdviserController {
    private final AdviserService adviserService;

    public AdviserController(AdviserService adviserService) {
        this.adviserService = adviserService;
    }

    @PostMapping("/adviser/{tenant_id}")
    public ResponseEntity<AdviserResponseDto> createBot(
            @Valid @RequestBody CreateAdviserRequestDto adviser,
            @PathVariable(value = "tenant_id") UUID tenantId
    ) {
        return new ResponseEntity<>(adviserService.createAdviser(tenantId, adviser), HttpStatus.CREATED);
    }

    @GetMapping("/get_advisers_by_tenant/{tenant_id}")
    public ResponseEntity<List<AdviserResponseDto>> get(
            @PathVariable(value = "tenant_id") UUID tenantId
    ) {
        return new ResponseEntity<>(adviserService.getAdviserByTenant(tenantId), HttpStatus.OK);
    }

    @DeleteMapping("/{adviser_id}")
    public ResponseEntity<Void> deleteBot(
            @PathVariable(name = "adviser_id") UUID adviserId
    ) {
        adviserService.deleteAdviser(adviserId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
