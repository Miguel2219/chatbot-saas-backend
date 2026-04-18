package com.chatbotsaas.chatbot_saas.bot.service;

import com.chatbotsaas.chatbot_saas.auth.service.AuthService;
import com.chatbotsaas.chatbot_saas.bot.dto.request.RegisterBotDto;
import com.chatbotsaas.chatbot_saas.bot.dto.response.ResponseBotDto;
import com.chatbotsaas.chatbot_saas.bot.dto.response.ResponseBotSelectDto;
import com.chatbotsaas.chatbot_saas.bot.dto.response.ResponseBotTableDto;
import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.bot.repository.BotRepository;
import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BotService {
    private final BotRepository botRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    public BotService(BotRepository botRepository, TenantRepository tenantRepository, UserRepository userRepository, AuthService authService) {
        this.botRepository = botRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional
    public ResponseBotDto createBot(RegisterBotDto bot) {
        User user = authService.getUserAuthenticated();

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleId().equals(RoleConstants.ADMIN_ROLE));
        UUID tenantId;

        if (isAdmin) {
            if (bot.getTenantID() == null) {
                throw new AppException("Tenant is required", HttpStatus.BAD_REQUEST);
            }
            tenantId = bot.getTenantID();
        } else {
            tenantId = user.getTenant().getId();
        }

        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(
                () -> new IllegalArgumentException("Tenant not found")
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
    public Page<ResponseBotTableDto> getBotByTenant(Pageable pageable) {
        User user = authService.getUserAuthenticated();

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleId().equals(RoleConstants.ADMIN_ROLE));

        Page<Bot> bots = isAdmin
                ? botRepository.findAll(pageable)
                : botRepository.findByTenant_Id(user.getTenant().getId(), pageable);

        return bots.map(bot -> ResponseBotTableDto.builder()
                .botId(bot.getBotId())
                .name(bot.getName())
                .description(bot.getDescription())
                .isActive(bot.getIsActive())
                .createdAt(bot.getCreatedAt())
                .build()
        );
    }

    @Transactional
    public List<ResponseBotSelectDto> getBotByTenantSelect() {
        User user = authService.getUserAuthenticated();

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleId().equals(RoleConstants.ADMIN_ROLE));

        List<Bot> bots = isAdmin
                ? botRepository.findAll()
                : botRepository.findByTenant_Id(user.getTenant().getId());

        return bots.stream()
                .map(bot -> ResponseBotSelectDto.builder()
                        .botId(bot.getBotId())
                        .name(bot.getName())
                        .build()
                ).toList();
    }

    @Transactional
    public void deleteBot(UUID botId) {
        botRepository.findById(botId).orElseThrow(
                () -> new IllegalArgumentException("El bot no existe")
        );
        botRepository.deleteById(botId);
    }

    @Transactional
    public ResponseBotDto getBotById(UUID botId) {
        Bot bot = botRepository.findById(botId).orElseThrow(
                () -> new AppException("Bot not found", HttpStatus.NOT_FOUND)
        );

        return new ResponseBotDto(
                bot.getName(),
                bot.getDescription(),
                bot.getIsActive()
        );
    }

}