package com.chatbotsaas.chatbot_saas.notification.service;

import com.chatbotsaas.chatbot_saas.lead.entity.Lead;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender javaMailSender;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendLeadNotification(String toEmail, String adviserName, Lead lead, String botName) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Nuevo cliente asignado - " + botName);
            String body = """
                    <h2>Hola %s,</h2>
                    <p>Tienes un nuevo lead asignado:</p>
                    <ul>
                        <li><strong>Nombre:</strong> %s</li>
                        <li><strong>Teléfono:</strong> %s</li>
                        <li><strong>Email:</strong> %s</li>
                        <li><strong>Bot:</strong> %s</li>
                        <li><strong>Sesión:</strong> %s</li>
                    </ul>
                    <p>Por favor contacta al cliente lo antes posible.</p>
                    """.formatted(adviserName, lead.getName(), lead.getPhone(), lead.getEmail(), botName, lead.getSessionId());
            helper.setText(body, true);
            javaMailSender.send(message);


        } catch (MessagingException e) {
            System.err.println("Failed to send email to "+ toEmail + ": " + e.getMessage());
        }
    }
}
