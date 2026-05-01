package com.chatbotsaas.chatbot_saas.whatsapp.service;

import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.bot.repository.BotRepository;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.shared.security.TenantAccessValidator;
import com.chatbotsaas.chatbot_saas.whatsapp.dto.request.CreateWhatsappConfigRequest;
import com.chatbotsaas.chatbot_saas.whatsapp.dto.response.WhatsappConfigResponseDto;
import com.chatbotsaas.chatbot_saas.whatsapp.entity.WhatsappConfig;
import com.chatbotsaas.chatbot_saas.whatsapp.repository.WhatsappConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WhatsappConfigService {
    private final WhatsappConfigRepository whatsappConfigRepository;
    private final BotRepository botRepository;
    private final TenantAccessValidator tenantAccessValidator;


    public WhatsappConfigService(WhatsappConfigRepository whatsappConfigRepository, BotRepository botRepository, TenantAccessValidator tenantAccessValidator) {
        this.whatsappConfigRepository = whatsappConfigRepository;
        this.botRepository = botRepository;
        this.tenantAccessValidator = tenantAccessValidator;
    }

    private WhatsappConfigResponseDto toResponseDto(WhatsappConfig whatsappConfig) {
        return WhatsappConfigResponseDto.builder()
                .id(whatsappConfig.getId())
                .botId(whatsappConfig.getBot().getBotId())
                .phoneNumberId(whatsappConfig.getPhoneNumberId())
                .isActive(whatsappConfig.getIsActive())
                .build();
    }

    @Transactional
    public void createConfig(UUID botId, CreateWhatsappConfigRequest request) {
        Bot bot = botRepository.findById(botId).orElseThrow(
                () -> new AppException("Bot not found", HttpStatus.NOT_FOUND)
        );
        // Aislamiento multi-tenant — sin este guard un user del tenant A
        // podía crear una config WhatsApp apuntando al bot del tenant B
        // (y con ello enrutar mensajes ajenos a su número).
        tenantAccessValidator.assertCanAccessBot(bot);

        whatsappConfigRepository.save(
                WhatsappConfig.create(
                        bot, request.getPhoneNumberId(), request.getAccessToken()
                )
        );
    }

    @Transactional
    public WhatsappConfigResponseDto getConfigByBot(UUID botId) {
        Bot bot = botRepository.findById(botId).orElseThrow(
                () -> new AppException("Bot not found", HttpStatus.NOT_FOUND)
        );
        // La config contiene accessToken (secreto) — validar tenant antes de leer.
        tenantAccessValidator.assertCanAccessBot(bot);
        WhatsappConfig config = whatsappConfigRepository.findByBot_BotId(bot.getBotId()).orElseThrow(
                () -> new AppException("Config not found", HttpStatus.NOT_FOUND)
        );
        return toResponseDto(config);
    }

    @Transactional
    public WhatsappConfigResponseDto updateConfig(UUID configId, CreateWhatsappConfigRequest request) {
        WhatsappConfig config = whatsappConfigRepository.findById(configId).orElseThrow(
                () -> new AppException("Config not found", HttpStatus.NOT_FOUND)
        );
        // Resolver el bot vía FK y validar — sin esto cualquier user con
        // `whatsapp-config:edit` podía modificar la config de otro tenant.
        tenantAccessValidator.assertCanAccessBot(config.getBot());

        config.update(request.getPhoneNumberId(), request.getAccessToken());
        whatsappConfigRepository.save(config);

        return toResponseDto(config);
    }

    @Transactional
    public WhatsappConfigResponseDto deleteConfig(UUID configId) {
        WhatsappConfig config = whatsappConfigRepository.findById(configId).orElseThrow(
                () -> new AppException("Config not found", HttpStatus.NOT_FOUND)
        );
        tenantAccessValidator.assertCanAccessBot(config.getBot());
        whatsappConfigRepository.delete(config);

        return toResponseDto(config);
    }
}
