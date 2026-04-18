package com.chatbotsaas.chatbot_saas.bot.controller;

import com.chatbotsaas.chatbot_saas.bot.dto.request.RegisterBotDto;
import com.chatbotsaas.chatbot_saas.bot.dto.response.ResponseBotDto;
import com.chatbotsaas.chatbot_saas.bot.dto.response.ResponseBotSelectDto;
import com.chatbotsaas.chatbot_saas.bot.dto.response.ResponseBotTableDto;
import com.chatbotsaas.chatbot_saas.bot.service.BotService;
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

    @GetMapping("/get_bots_by_tenant/")
    public ResponseEntity<Page<ResponseBotTableDto>> getBotsByTenantId(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "createdAt") String order_by,
            @RequestParam(defaultValue = "desc") String order) {

        Pageable pageable = order.equalsIgnoreCase("desc")
                ? PageRequest.of(offset, limit, Sort.by(order_by).descending())
                : PageRequest.of(offset, limit, Sort.by(order_by).ascending());

        return new ResponseEntity<>(botService.getBotByTenant(pageable), HttpStatus.OK);
    }

    @GetMapping("/get_bots_by_tenant_select/")
    public ResponseEntity<List<ResponseBotSelectDto>> getBotsByTenantSelect() {
        return new ResponseEntity<>(botService.getBotByTenantSelect(), HttpStatus.OK);
    }

    @GetMapping("/{bot_id}")
    public ResponseEntity<ResponseBotDto> getBot(
            @PathVariable(name = "bot_id") UUID botId
    ) {
        return new ResponseEntity<>(botService.getBotById(botId), HttpStatus.OK);
    }
}
