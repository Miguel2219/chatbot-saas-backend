package com.chatbotsaas.chatbot_saas.bot.controller;

import com.chatbotsaas.chatbot_saas.bot.dto.request.RegisterBotDto;
import com.chatbotsaas.chatbot_saas.bot.dto.request.UpdateBotDto;
import com.chatbotsaas.chatbot_saas.bot.dto.request.UpdateSystemPromptDto;
import com.chatbotsaas.chatbot_saas.bot.dto.response.*;
import com.chatbotsaas.chatbot_saas.bot.service.BotService;
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
@RequestMapping("/api/bot/")
@RequiredArgsConstructor
public class BotController {
    private final BotService botService;

    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('bots', 'create')")
    public ResponseEntity<ResponseBotDto> createBot(
            @Valid @RequestBody RegisterBotDto bot) {
        return new ResponseEntity<>(botService.createBot(bot), HttpStatus.CREATED);
    }

    @DeleteMapping("/{bot_id}")
    @PreAuthorize("@permissionChecker.hasPermission('bots', 'delete')")
    public ResponseEntity<Void> deleteBot(
            @PathVariable(name = "bot_id") UUID botId
    ) {
       botService.deleteBot(botId);
       return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/get_bots_by_tenant/")
    @PreAuthorize("@permissionChecker.hasPermission('bots', 'view')")
    public ResponseEntity<Page<ResponseBotTableDto>> getBotsByTenantId(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String order_by,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(name = "tenantId", required = false) UUID tenantId) {

        Pageable pageable = order.equalsIgnoreCase("desc")
                ? PageRequest.of(offset, limit, Sort.by(order_by).descending())
                : PageRequest.of(offset, limit, Sort.by(order_by).ascending());

        return new ResponseEntity<>(botService.getBotByTenant(tenantId, pageable), HttpStatus.OK);
    }

    @GetMapping("/get_bots_by_tenant_select/")
    @PreAuthorize("@permissionChecker.hasPermission('bots', 'view')")
    public ResponseEntity<List<ResponseBotSelectDto>> getBotsByTenantSelect() {
        return new ResponseEntity<>(botService.getBotByTenantSelect(), HttpStatus.OK);
    }

    @GetMapping("/get_bots_by_tenant_select/{tenant_id}")
    @PreAuthorize("@permissionChecker.hasPermission('bots', 'view')")
    public ResponseEntity<List<ResponseBotSelectDto>> getBotsByTenantIdSelect(
            @PathVariable(name = "tenant_id") UUID tenantId
    ) {
        return new ResponseEntity<>(botService.getBotsByTenantIdSelect(tenantId), HttpStatus.OK);
    }

    @GetMapping("/{bot_id}")
    @PreAuthorize("@permissionChecker.hasPermission('bots', 'view')")
    public ResponseEntity<ResponseBotDto> getBot(
            @PathVariable(name = "bot_id") UUID botId
    ) {
        return new ResponseEntity<>(botService.getBotById(botId), HttpStatus.OK);
    }

    /**
     * Endpoint público — invocado por el widget embebido en sitios de
     * clientes para renderizar su UI inicial. Sin {@code @PreAuthorize}
     * y registrado en {@code permitAll()} de SecurityConfig. Devuelve
     * un DTO mínimo (solo el nombre) que NO expone datos sensibles.
     */
    @GetMapping("/get_widget_bot/{bot_id}")
    public ResponseEntity<ResponseBotWidget> getWidgetBot(
            @PathVariable(name = "bot_id") UUID botId
    ) {
        return new ResponseEntity<>(botService.getWidgetBot(botId), HttpStatus.OK);
    }

    @GetMapping("/{bot_id}/system-prompt")
    @PreAuthorize("@permissionChecker.hasPermission('bots', 'view')")
    public ResponseEntity<SystemPromptDto> systemPrompt(@PathVariable UUID bot_id) {
        return new ResponseEntity<>(botService.getSystemPrompt(bot_id), HttpStatus.OK);
    }

    @PutMapping("/{bot_id}")
    @PreAuthorize("@permissionChecker.hasPermission('bots', 'edit')")
    public ResponseEntity<ResponseBotDto> updateBot(
            @PathVariable(name = "bot_id") UUID botId,
            @Valid @RequestBody UpdateBotDto dto
    ) {
        return new ResponseEntity<>(botService.updateBot(botId, dto), HttpStatus.OK);
    }

    @PutMapping("/{bot_id}/system-prompt")
    @PreAuthorize("@permissionChecker.hasPermission('bots', 'edit')")
    public ResponseEntity<SystemPromptDto> updateSystemPrompt(
            @PathVariable(name = "bot_id") UUID botId,
            @Valid @RequestBody UpdateSystemPromptDto dto
    ) {
        return new ResponseEntity<>(botService.updateSystemPrompt(botId, dto), HttpStatus.OK);
    }
}
