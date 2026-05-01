package com.chatbotsaas.chatbot_saas.quota.listener;

import com.chatbotsaas.chatbot_saas.notification.service.EmailService;
import com.chatbotsaas.chatbot_saas.quota.event.QuotaThresholdCrossedEvent;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía el correo de notificación cuando el tenant cruza 80% / 100% / 150% del límite del ciclo.
 * <p>
 * <b>AFTER_COMMIT</b>: si la transacción del chat hizo rollback, no se manda correo fantasma.<br>
 * <b>@Async</b>: el envío SMTP no bloquea el request del chat.
 * <p>
 * Destinatario: {@code Tenant.email} directamente. Si está vacío → log warn + no envío
 * (no hay fallback a USER). 150% agrega CC al ADMIN de Zolvion, resuelto dentro de
 * {@link EmailService#sendQuotaThresholdNotification}.
 */
@Component
public class QuotaThresholdEmailListener {

    private static final Logger log = LoggerFactory.getLogger(QuotaThresholdEmailListener.class);

    private final EmailService emailService;
    private final TenantRepository tenantRepo;

    public QuotaThresholdEmailListener(EmailService emailService, TenantRepository tenantRepo) {
        this.emailService = emailService;
        this.tenantRepo = tenantRepo;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onThresholdCrossed(QuotaThresholdCrossedEvent event) {
        Tenant tenant = tenantRepo.findById(event.tenantId()).orElse(null);
        if (tenant == null) {
            log.warn("Quota threshold event for unknown tenantId={}, threshold={}",
                    event.tenantId(), event.threshold());
            return;
        }

        String to = tenant.getEmail();
        if (to == null || to.isBlank()) {
            log.warn("Tenant {} ({}) has no email; skipping quota notification threshold={}",
                    tenant.getId(), tenant.getName(), event.threshold());
            return;
        }

        boolean copyAdmin = event.threshold() == 150;
        log.info("Sending quota threshold={} notification to tenant={} email={} copyAdmin={}",
                event.threshold(), tenant.getId(), to, copyAdmin);

        try {
            emailService.sendQuotaThresholdNotification(tenant, to, event, copyAdmin);
        } catch (Exception e) {
            // No retornamos ni re-tiramos — el chat ya comiteó. Fallo de email no rompe nada.
            log.error("Failed to send quota threshold={} notification to tenant={}: {}",
                    event.threshold(), tenant.getId(), e.getMessage(), e);
        }
    }
}
