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

    public void notifyAdviser(User adviser, Lead lead, Bot bot) {
        NotificationChannel channel = adviser.getNotificationChannel();
        String adviserName = adviser.getPerson().getName();
        String adviserEmail = adviser.getEmail();

        if (channel == NotificationChannel.EMAIL || channel == NotificationChannel.BOTH) {
            emailService.sendLeadNotification(
                    adviserEmail,
                    adviserName,
                    lead,
                    bot.getName()
            );
        }

        if (channel == NotificationChannel.WHATSAPP || channel == NotificationChannel.BOTH) {
            //TODO: Whastapp integration (twilio)
        }
    }


}
