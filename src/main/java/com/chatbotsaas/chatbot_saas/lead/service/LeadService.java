package com.chatbotsaas.chatbot_saas.lead.service;

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
import com.chatbotsaas.chatbot_saas.user.entity.User;
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

    public LeadService(LeadRepository leadRepository, BotRepository botRepository, NotificationService notificationService) {
        this.leadRepository = leadRepository;
        this.botRepository = botRepository;
        this.notificationService = notificationService;
    }

    private LeadResponseDto leadToResponseDto (Lead lead) {
        return LeadResponseDto.builder()
                .id(lead.getLeadId())
                .name(lead.getName())
                .phone(lead.getPhone())
                .email(lead.getEmail())
                .status(lead.getStatus())
                .assignedAdviserId(lead.getAssignedAdviserId())
                .createdAt(lead.getCreatedAt())
                .build();
    }

    @Transactional
    public LeadResponseDto saveLead(LeadRequestDto requestDto) {
        Bot bot = botRepository.findById(requestDto.getBotId()).orElseThrow(
                () -> new AppException("Bot not found", HttpStatus.NOT_FOUND)
        );

        //Find all advisers of that bot
        List<User> advisers = bot.getAdvisers();
        if (advisers.isEmpty()) {
            throw new AppException("Bot has no advisers assigned",HttpStatus.CONFLICT);
        }

        //Apply Round Robin to pick one adviser
        int nextIndex = (bot.getLastAdviserIndex() + 1) % advisers.size();
        User assignedAdviser = advisers.get(nextIndex);

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

        bot.setLastAdviserIndex(nextIndex);
        botRepository.save(bot);

        // Send notification to adviser
        notificationService.notifyAdviser(assignedAdviser, leadSaved, bot);

        return leadToResponseDto(leadSaved);
    }

    @Transactional
    public List<LeadResponseDto> getLeadsByBot(UUID botId) {
        List<Lead> leads = leadRepository.findByBotId(botId);
        return leads.stream().map(
                this::leadToResponseDto
        ).toList();
    }

    @Transactional
    public void updateLeadStatus(UUID leadId, LeadStatus status) {
        Lead lead = leadRepository.findById(leadId).orElseThrow(
                () -> new AppException("Lead not found", HttpStatus.NOT_FOUND)
        );
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
