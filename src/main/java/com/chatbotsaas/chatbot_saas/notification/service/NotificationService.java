package com.chatbotsaas.chatbot_saas.notification.service;

import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.lead.entity.Lead;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.enums.NotificationChannel;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final EmailService emailService;

    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void notifyLeadAssignee(User assignee, Lead lead, Bot bot) {
        NotificationChannel channel = assignee.getNotificationChannel();
        String assigneeName = assignee.getPerson().getName();
        String assigneeEmail = assignee.getEmail();

        if (channel == NotificationChannel.EMAIL) {
            emailService.sendLeadNotification(
                    assigneeEmail,
                    assigneeName,
                    lead,
                    bot.getName()
            );
        }

        if (channel == NotificationChannel.WHATSAPP) {
            //TODO: Whastapp integration (twilio)
        }
    }


}
