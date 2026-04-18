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

    public void sendWelcomeEmail(String toEmail, String tempPassword, String tenantName) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Bienvenido a " + tenantName + " — Tus credenciales de acceso");

            String body = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2>¡Bienvenido a la plataforma!</h2>
                    <p>Tu cuenta ha sido creada exitosamente para <strong>%s</strong>.</p>
                    <p>Aquí están tus credenciales de acceso:</p>
                    <div style="background: #f5f5f5; padding: 16px; border-radius: 8px; margin: 16px 0;">
                        <p><strong>Email:</strong> %s</p>
                        <p><strong>Contraseña temporal:</strong> %s</p>
                    </div>
                    <p style="color: #e74c3c;">
                        <strong>Importante:</strong> Al ingresar por primera vez se te pedirá 
                        cambiar tu contraseña.
                    </p>
                    <p>Si tienes algún problema para acceder, contacta al administrador.</p>
                </div>
                """.formatted(tenantName, toEmail, tempPassword);

            helper.setText(body, true);
            javaMailSender.send(message);

        } catch (MessagingException e) {
            System.err.println("Failed to send welcome email to " + toEmail + ": " + e.getMessage());
        }
    }
}
