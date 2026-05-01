package com.chatbotsaas.chatbot_saas.notification.service;

import com.chatbotsaas.chatbot_saas.lead.entity.Lead;
import com.chatbotsaas.chatbot_saas.quota.event.QuotaThresholdCrossedEvent;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter CYCLE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat COP_FMT = NumberFormat.getInstance(Locale.forLanguageTag("es-CO"));

    private final Resend resend;
    private final String zolvionAdminEmail;

    public EmailService(
            @Value("${spring.resend.api-key}") String apiKey,
            @Value("${zolvion.admin-email:}") String zolvionAdminEmail
    ) {
        this.resend = new Resend(apiKey);
        this.zolvionAdminEmail = zolvionAdminEmail;
    }

    public void sendLeadNotification(String toEmail, String adviserName, Lead lead, String botName) {
        try {
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

            CreateEmailOptions request = CreateEmailOptions.builder()
                    .from("Zolvion <hola@zolvion.com>")
                    .to(List.of(toEmail))
                    .subject("Nuevo cliente asignado - " + botName)
                    .html(body)
                    .build();

            resend.emails().send(request);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendTenantOwnerPromotionEmail(String toEmail, String tenantName) {
        try {
            String body = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                        <h2>¡Felicitaciones!</h2>
                        <p>Has sido designado como <strong>administrador</strong> de <strong>%s</strong>.</p>
                        <p>A partir de ahora podrás:</p>
                        <ul>
                            <li>Invitar nuevos usuarios al tenant</li>
                            <li>Administrar los bots, conversaciones y leads</li>
                            <li>Gestionar la configuración del tenant</li>
                        </ul>
                        <p>Inicia sesión con tu email y contraseña habitual para acceder al panel.</p>
                        <p style="color:#7f8c8d;">— Equipo Zolvion</p>
                    </div>
                    """.formatted(tenantName);

            CreateEmailOptions request = CreateEmailOptions.builder()
                    .from("Zolvion <hola@zolvion.com>")
                    .to(List.of(toEmail))
                    .subject("Has sido designado administrador de "+ tenantName)
                    .html(body)
                    .build();
            resend.emails().send(request);
        } catch (Exception e) {
            log.error("Failed to send tenant-owner promotion email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendWelcomeEmail(String toEmail, String tempPassword, String tenantName) {
        try {
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

            CreateEmailOptions request = CreateEmailOptions.builder()
                    .from("Zolvion <hola@zolvion.com>")
                    .to(List.of(toEmail))
                    .subject("Bienvenido a " + tenantName + " — Tus credenciales de acceso")
                    .html(body)
                    .build();

            resend.emails().send(request);

        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendQuotaThresholdNotification(
            Tenant tenant,
            String to,
            QuotaThresholdCrossedEvent event,
            boolean copyAdmin
    ) {
        try {
            String cycleRange = CYCLE_FMT.format(event.cycleStart()) + " – " + CYCLE_FMT.format(event.cycleEnd());
            String subject = buildSubject(event.threshold(), tenant.getName());
            String body = buildBody(event.threshold(), tenant.getName(), cycleRange,
                    event.conversationsCount(), event.limit(), event.excessCount(), event.excessCostCop());

            CreateEmailOptions.Builder builder = CreateEmailOptions.builder()
                    .from("Zolvion <hola@zolvion.com>")
                    .to(List.of(to))
                    .subject(subject)
                    .html(body);

            if (copyAdmin && zolvionAdminEmail != null && !zolvionAdminEmail.isBlank()) {
                builder.cc(List.of(zolvionAdminEmail));
            } else if (copyAdmin) {
                log.warn("copyAdmin=true but zolvion.admin-email is not configured; skipping CC to admin.");
            }

            resend.emails().send(builder.build());

        } catch (Exception e) {
            log.error("Failed to send quota threshold={} email to {}: {}",
                    event.threshold(), to, e.getMessage(), e);
        }
    }

    private String buildSubject(int threshold, String tenantName) {
        return switch (threshold) {
            case 80 -> "Aviso: has usado el 80% de tu plan — " + tenantName;
            case 100 -> "Has alcanzado el límite de tu plan — " + tenantName;
            case 150 -> "Tu consumo está en 150% — " + tenantName;
            default -> "Notificación de uso — " + tenantName;
        };
    }

    private String buildBody(
            int threshold,
            String tenantName,
            String cycleRange,
            int count,
            int limit,
            int excess,
            int excessCostCop
    ) {
        return switch (threshold) {
            case 80 -> template80(tenantName, cycleRange, count, limit);
            case 100 -> template100(tenantName, cycleRange, count, limit);
            case 150 -> template150(tenantName, cycleRange, count, limit, excess, excessCostCop);
            default -> templateDefault(tenantName, cycleRange, count, limit);
        };
    }

    private String template80(String tenantName, String cycleRange, int count, int limit) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color:#e67e22;">Estás cerca de tu límite mensual</h2>
                    <p>Hola <strong>%s</strong>,</p>
                    <p>Has usado el <strong>80%%</strong> de tu plan para el ciclo <strong>%s</strong>.</p>
                    <div style="background:#fff7e6; padding:16px; border-radius:8px; margin:16px 0;">
                        <p><strong>Conversaciones usadas:</strong> %d / %d</p>
                    </div>
                    <p>Aún estás dentro del plan, pero al alcanzar el límite, cada conversación adicional se facturará como excedente.</p>
                    <p style="color:#7f8c8d;">— Equipo Zolvion</p>
                </div>
                """.formatted(tenantName, cycleRange, count, limit);
    }

    private String template100(String tenantName, String cycleRange, int count, int limit) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color:#c0392b;">Alcanzaste el límite de tu plan</h2>
                    <p>Hola <strong>%s</strong>,</p>
                    <p>Has alcanzado el <strong>100%%</strong> del límite de conversaciones del ciclo <strong>%s</strong>.</p>
                    <div style="background:#fdecea; padding:16px; border-radius:8px; margin:16px 0;">
                        <p><strong>Conversaciones usadas:</strong> %d / %d</p>
                    </div>
                    <p>Tu servicio sigue funcionando sin interrupciones. Cada conversación por encima de este límite se facturará como excedente hasta el cierre del ciclo.</p>
                    <p style="color:#7f8c8d;">— Equipo Zolvion</p>
                </div>
                """.formatted(tenantName, cycleRange, count, limit);
    }

    private String template150(String tenantName, String cycleRange, int count, int limit, int excess, int excessCostCop) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2 style="color:#8e44ad;">Consumo del 150%% alcanzado</h2>
                    <p>Hola <strong>%s</strong>,</p>
                    <p>Tu cuenta ha alcanzado el <strong>150%%</strong> del plan para el ciclo <strong>%s</strong>.</p>
                    <div style="background:#f4ecf7; padding:16px; border-radius:8px; margin:16px 0;">
                        <p><strong>Conversaciones usadas:</strong> %d / %d</p>
                        <p><strong>Conversaciones excedentes:</strong> %d</p>
                        <p><strong>Costo estimado del excedente:</strong> COP $%s</p>
                    </div>
                    <p>El servicio continúa activo. Si esperas mantener este nivel de uso, te recomendamos contactar a Zolvion para evaluar un plan superior.</p>
                    <p style="color:#7f8c8d;">— Equipo Zolvion</p>
                </div>
                """.formatted(tenantName, cycleRange, count, limit, excess, COP_FMT.format(excessCostCop));
    }

    private String templateDefault(String tenantName, String cycleRange, int count, int limit) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <h2>Actualización de consumo</h2>
                    <p>Hola <strong>%s</strong>,</p>
                    <p>Tu consumo para el ciclo <strong>%s</strong> es de %d / %d conversaciones.</p>
                    <p style="color:#7f8c8d;">— Equipo Zolvion</p>
                </div>
                """.formatted(tenantName, cycleRange, count, limit);
    }

    // ------------------------------------------------------------------------
    // Password reset (forgot password + confirmación post-cambio)
    // ------------------------------------------------------------------------

    private static final DateTimeFormatter RESET_TIME_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("es-CO"));

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetLink, String userName) {
        try {
            String greeting = (userName == null || userName.isBlank()) ? "Hola," : "Hola " + userName + ",";
            String body = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background:#1a1a1a; color:#ffffff; padding: 32px; border-radius: 12px;">
                <h2 style="color:#ff6b35; margin-top:0;">Recupera tu contraseña</h2>
                <p style="font-size:15px; line-height:1.6;">%s</p>
                <p style="font-size:15px; line-height:1.6;">
                    Recibimos una solicitud para restablecer la contraseña de tu cuenta Zolvion.
                    Hacé clic en el botón para elegir una nueva.
                </p>
                <div style="text-align:center; margin: 28px 0;">
                    <a href="%s"
                       style="display:inline-block; background:#ff6b35; color:#ffffff; text-decoration:none;
                              padding:14px 28px; border-radius:8px; font-weight:600; font-size:15px;">
                        Recuperar contraseña
                    </a>
                </div>
                <p style="font-size:13px; color:#b0b0b0; line-height:1.5;">
                    Este enlace expira en <strong>1 hora</strong>. Si no solicitaste este cambio,
                    podés ignorar este correo — tu contraseña seguirá igual.
                </p>
                <p style="font-size:13px; color:#b0b0b0; line-height:1.5; word-break:break-all;">
                    ¿El botón no funciona? Copiá este link:<br>
                    <span style="color:#ff6b35;">%s</span>
                </p>
                <hr style="border:none; border-top:1px solid #333; margin: 24px 0;">
                <p style="font-size:12px; color:#888;">
                    ¿Necesitás ayuda? Escribinos a <a href="mailto:hola@zolvion.com" style="color:#ff6b35;">hola@zolvion.com</a><br>
                    — Equipo Zolvion
                </p>
            </div>
            """.formatted(greeting, resetLink, resetLink);

            CreateEmailOptions request = CreateEmailOptions.builder()
                    .from("Zolvion <hola@zolvion.com>")
                    .to(List.of(toEmail))
                    .subject("Recupera tu contraseña de Zolvion")
                    .html(body)
                    .build();

            resend.emails().send(request);

        } catch (Exception e) {
            log.error("Failed to send password-reset email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async
    public void sendPasswordChangedConfirmation(String toEmail, String userName) {
        try {
            String greeting = (userName == null || userName.isBlank()) ? "Hola," : "Hola " + userName + ",";
            String when = RESET_TIME_FMT.format(LocalDateTime.now()) + " (Bogotá)";

            String body = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; background:#1a1a1a; color:#ffffff; padding: 32px; border-radius: 12px;">
                <h2 style="color:#ff6b35; margin-top:0;">Contraseña actualizada</h2>
                <p style="font-size:15px; line-height:1.6;">%s</p>
                <p style="font-size:15px; line-height:1.6;">
                    Tu contraseña de Zolvion se cambió el <strong>%s</strong>.
                </p>
                <p style="font-size:15px; line-height:1.6;">
                    Por seguridad, cerramos todas tus sesiones activas. Vas a tener que
                    volver a iniciar sesión en cada dispositivo.
                </p>
                <div style="background:#2a1a1a; border-left:3px solid #ff6b35; padding:14px 18px; margin:20px 0; border-radius:4px;">
                    <p style="margin:0; font-size:14px; color:#ffd7c2;">
                        <strong>¿No fuiste vos?</strong> Contactanos inmediatamente a
                        <a href="mailto:hola@zolvion.com" style="color:#ff6b35;">hola@zolvion.com</a>
                        para asegurar tu cuenta.
                    </p>
                </div>
                <hr style="border:none; border-top:1px solid #333; margin: 24px 0;">
                <p style="font-size:12px; color:#888;">
                    — Equipo Zolvion
                </p>
            </div>
            """.formatted(greeting, when);

            CreateEmailOptions request = CreateEmailOptions.builder()
                    .from("Zolvion <hola@zolvion.com>")
                    .to(List.of(toEmail))
                    .subject("Tu contraseña de Zolvion fue cambiada")
                    .html(body)
                    .build();

            resend.emails().send(request);

        } catch (Exception e) {
            log.error("Failed to send password-changed confirmation to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
