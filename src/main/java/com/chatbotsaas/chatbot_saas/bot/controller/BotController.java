package com.chatbotsaas.chatbot_saas.bot.controller;

import com.chatbotsaas.chatbot_saas.bot.dto.request.RegisterBotDto;
import com.chatbotsaas.chatbot_saas.bot.dto.response.ResponseBotDto;
import com.chatbotsaas.chatbot_saas.bot.service.BotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bot/")
@RequiredArgsConstructor
public class BotController {
    private final BotService botService;

    @PostMapping
    public ResponseEntity<ResponseBotDto> createBot(
            @Valid @RequestBody RegisterBotDto bot) {
        return new ResponseEntity<>(botService.createBot(bot), HttpStatus.CREATED);
    }

    @DeleteMapping("/{bot_id}")
    public ResponseEntity<Void> deleteBot(
            @PathVariable(name = "bot_id") UUID botId
    ) {
       botService.deleteBot(botId);
       return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/get_bots_by_tenant/{tenant_id}")
    public ResponseEntity<List<ResponseBotDto>> getBotsByTenantId(
            @PathVariable(name = "tenant_id") UUID tenantId
    ) {
        return new ResponseEntity<>(botService.getBotByTenant(tenantId), HttpStatus.OK);
    }

    @GetMapping("/{bot_id}")
    public ResponseEntity<ResponseBotDto> getBot(
            @PathVariable(name = "bot_id") UUID botId
    ) {
        return new ResponseEntity<>(botService.getBotById(botId), HttpStatus.OK);
    }
}
