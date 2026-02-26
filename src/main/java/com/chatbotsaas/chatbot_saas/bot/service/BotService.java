package com.chatbotsaas.chatbot_saas.bot.service;

import com.chatbotsaas.chatbot_saas.bot.dto.request.RegisterBotDto;
import com.chatbotsaas.chatbot_saas.bot.dto.response.ResponseBotDto;
import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.bot.repository.BotRepository;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BotService {
    private final BotRepository botRepository;
    private final TenantRepository tenantRepository;

    public BotService(BotRepository botRepository, TenantRepository tenantRepository) {
        this.botRepository = botRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public ResponseBotDto createBot(RegisterBotDto bot) {
        Tenant tenant = tenantRepository.findById(bot.getTenantID()).orElseThrow(
                () -> new IllegalArgumentException("La empresa no existe")
        );

        Bot botSave = botRepository.save(
                Bot.create(
                        bot.getName(),
                        bot.getDescription(),
                        tenant
                )
        );
        return new ResponseBotDto(
                botSave.getName(),
                botSave.getDescription(),
                botSave.getIsActive()
        );
    }

    @Transactional
    public List<ResponseBotDto> getBotByTenant(UUID tenantId) {
        tenantRepository.findById(tenantId).orElseThrow(
                () -> new IllegalArgumentException("La empresa no existe")
        );
        List<Bot> bots = botRepository.findByTenant_Id(tenantId);
        return bots.stream().map(bot ->
                new ResponseBotDto(
                        bot.getName(),
                        bot.getDescription(),
                        bot.getIsActive()
                )).toList();
    }

    @Transactional
    public void deleteBot(UUID botId) {
        botRepository.findById(botId).orElseThrow(
                () -> new IllegalArgumentException("El bot no existe")
        );
        botRepository.deleteById(botId);
    }
}