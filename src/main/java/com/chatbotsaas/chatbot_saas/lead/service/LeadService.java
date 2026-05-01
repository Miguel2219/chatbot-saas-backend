package com.chatbotsaas.chatbot_saas.lead.service;

import com.chatbotsaas.chatbot_saas.auth.service.AuthService;
import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.bot.repository.BotRepository;
import com.chatbotsaas.chatbot_saas.lead.dto.request.LeadRequestDto;
import com.chatbotsaas.chatbot_saas.lead.dto.response.LeadDataDto;
import com.chatbotsaas.chatbot_saas.lead.dto.response.LeadResponseDto;
import com.chatbotsaas.chatbot_saas.lead.entity.Lead;
import com.chatbotsaas.chatbot_saas.lead.enums.LeadChannel;
import com.chatbotsaas.chatbot_saas.lead.enums.LeadStatus;
import com.chatbotsaas.chatbot_saas.lead.repository.LeadRepository;
import com.chatbotsaas.chatbot_saas.notification.service.NotificationService;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.shared.security.TenantAccessValidator;
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
public class LeadService {
    private final LeadRepository leadRepository;
    private final BotRepository botRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final TenantAccessValidator tenantAccessValidator;

    public LeadService(LeadRepository leadRepository, BotRepository botRepository, NotificationService notificationService, UserRepository userRepository, AuthService authService, TenantAccessValidator tenantAccessValidator) {
        this.leadRepository = leadRepository;
        this.botRepository = botRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.authService = authService;
        this.tenantAccessValidator = tenantAccessValidator;
    }

    private LeadResponseDto leadToResponseDto (Lead lead) {
        String adviserName = null;

        if (lead.getAssignedAdviserId() != null) {
            adviserName = userRepository.findById(lead.getAssignedAdviserId())
                    .map(user -> user.getPerson().getName())
                    .orElse(null);
        }
        return LeadResponseDto.builder()
                .id(lead.getLeadId())
                .name(lead.getName())
                .phone(lead.getPhone())
                .email(lead.getEmail())
                .status(lead.getStatus())
                .channel(lead.getLeadChannel())
                // Sin este id el cliente no sabe a que user esta asignado el
                // lead — el nombre solo sirve para mostrar en la tabla.
                .assignedAdviserId(lead.getAssignedAdviserId())
                .assignedAdviser(adviserName)
                .createdAt(lead.getCreatedAt())
                .build();
    }

    @Transactional
    public LeadResponseDto saveLead(LeadRequestDto requestDto) {
        Bot bot = botRepository.findById(requestDto.getBotId()).orElseThrow(
                () -> new AppException("Bot not found", HttpStatus.NOT_FOUND)
        );

        //Find all lead assignees of that bot
        List<User> assignees = bot.getLeadAssignees();
        if (assignees.isEmpty()) {
            throw new AppException("Bot has no lead assignees configured", HttpStatus.CONFLICT);
        }

        //Apply Round Robin to pick one assignee
        int nextIndex = (bot.getLastAssigneeIndex() + 1) % assignees.size();
        User assignedAdviser = assignees.get(nextIndex);

        Lead leadSaved = leadRepository.save(
            Lead.builder()
                    .botId(requestDto.getBotId())
                    .sessionId(requestDto.getSessionId())
                    .name(requestDto.getName())
                    .phone(requestDto.getPhone())
                    .email(requestDto.getEmail())
                    .assignedAdviserId(assignedAdviser.getUserId())
                    .status(LeadStatus.PENDING)
                    .requestDetail(requestDto.getRequestDetail())
                    .leadChannel(requestDto.getLeadChannel())
                    .build()
        );

        bot.setLastAssigneeIndex(nextIndex);
        botRepository.save(bot);

        // Send notification to lead assignee
        notificationService.notifyLeadAssignee(assignedAdviser, leadSaved, bot);

        return leadToResponseDto(leadSaved);
    }

    @Transactional(readOnly = true)
    public Page<LeadResponseDto> getLeads(UUID botId, UUID tenantId, Pageable pageable) {
        User user = authService.getUserAuthenticated();
        if (user == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
        boolean isAdmin = user.isAdmin();
        UUID myTenantId = user.getTenant() != null ? user.getTenant().getId() : null;

        if (!isAdmin && tenantId != null && !tenantId.equals(myTenantId)) {
            throw new AppException("Forbidden", HttpStatus.FORBIDDEN);
        }

        Page<Lead> leads;
        if (botId != null) {
            Bot bot = botRepository.findById(botId)
                    .orElseThrow(() -> new AppException("Bot not found", HttpStatus.NOT_FOUND));
            if (!isAdmin && !bot.getTenant().getId().equals(myTenantId)) {
                throw new AppException("Forbidden", HttpStatus.FORBIDDEN);
            }
            leads = leadRepository.findByBotId(botId, pageable);
        } else if (tenantId != null) {
            leads = leadRepository.findByTenantId(tenantId, pageable);
        } else if (isAdmin) {
            leads = leadRepository.findAll(pageable);
        } else {
            leads = leadRepository.findByTenantId(myTenantId, pageable);
        }
        return leads.map(this::leadToResponseDto);
    }

    @Transactional
    public void updateLeadStatus(UUID leadId, LeadStatus status) {
        Lead lead = leadRepository.findById(leadId).orElseThrow(
                () -> new AppException("Lead not found", HttpStatus.NOT_FOUND)
        );
        // Aislamiento multi-tenant — {@link Lead} no tiene tenant_id directo
        // (solo bot_id plano), así que resolvemos el bot y delegamos al
        // validator. Sin esto cualquier caller con `leads:edit` podía mover
        // el status de leads ajenos.
        Bot leadBot = botRepository.findById(lead.getBotId()).orElseThrow(
                () -> new AppException("Bot not found", HttpStatus.NOT_FOUND)
        );
        tenantAccessValidator.assertCanAccessBot(leadBot);

        lead.setStatus(status);
        leadRepository.save(lead);
    }

    @Transactional
    public void saveLeadWhatsapp(UUID botId, String sessionId, LeadDataDto leadData, String requestDetail) {
        leadRepository.save(
                Lead.builder()
                        .botId(botId)
                        .sessionId(sessionId)
                        .name(leadData.getName())
                        .phone(leadData.getPhone())
                        .email(leadData.getEmail())
                        .assignedAdviserId(null)
                        .status(LeadStatus.PENDING)
                        .requestDetail(requestDetail)
                        .leadChannel(LeadChannel.WHATSAPP)
                        .build()
        );
    }
}
