package com.chatbotsaas.chatbot_saas.whatsapp.controller;

import com.chatbotsaas.chatbot_saas.whatsapp.dto.request.CreateWhatsappConfigRequest;
import com.chatbotsaas.chatbot_saas.whatsapp.dto.response.WhatsappConfigResponseDto;
import com.chatbotsaas.chatbot_saas.whatsapp.service.WhatsappConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/whatsapp-config")
public class WhatsappConfigController {
    private final WhatsappConfigService whatsappConfigService;

    @PostMapping("/{bot_id}")
    @PreAuthorize("@permissionChecker.hasPermission('whatsapp-config', 'create')")
    public ResponseEntity<Void> createConfig(
            @PathVariable(name = "bot_id") UUID botId,
            @RequestBody CreateWhatsappConfigRequest request
    ) {
        whatsappConfigService.createConfig(botId, request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/{bot_id}")
    @PreAuthorize("@permissionChecker.hasPermission('whatsapp-config', 'view')")
    public ResponseEntity<WhatsappConfigResponseDto> getConfigByBot(
            @PathVariable(name = "bot_id") UUID botId
    ) {
        return new ResponseEntity<>(whatsappConfigService.getConfigByBot(botId), HttpStatus.OK);
    }

    @PutMapping("/{config_id}")
    @PreAuthorize("@permissionChecker.hasPermission('whatsapp-config', 'edit')")
    public ResponseEntity<WhatsappConfigResponseDto> updateConfig(
            @PathVariable(name = "config_id") UUID configId,
            @RequestBody CreateWhatsappConfigRequest request
    ) {
        return new ResponseEntity<>(whatsappConfigService.updateConfig(configId, request), HttpStatus.OK);
    }

    @DeleteMapping("/{config_id}")
    @PreAuthorize("@permissionChecker.hasPermission('whatsapp-config', 'delete')")
    public ResponseEntity<WhatsappConfigResponseDto> deleteConfig(
            @PathVariable(name = "config_id") UUID configId
    ) {
        return new ResponseEntity<>(whatsappConfigService.deleteConfig(configId), HttpStatus.OK);
    }
}
