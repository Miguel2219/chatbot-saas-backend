package com.chatbotsaas.chatbot_saas.auth.scheduler;

import com.chatbotsaas.chatbot_saas.auth.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Job diario que borra password reset tokens expirados hace más tiempo
 * que el {@code app.password-reset.cleanup-grace-days} configurado
 * (default 7 días).
 *
 * La ventana de gracia permite auditar incidentes durante un tiempo
 * antes de perder el dato. Expirado ≠ borrable al instante — puede
 * servir para correlacionar logs si hay sospecha de abuso del endpoint
 * /forgot-password.
 *
 * Cron: {@code 0 30 0 * * *} en zona América/Bogotá (00:30 AM local).
 * Corre 15 min después de {@code RefreshTokenCleanupJob} (00:15) y 25
 * min después de {@code QuotaCycleLogJob} (00:05) para que los tres
 * jobs de mantenimiento no toquen DB al mismo tiempo.
 */
@Component
@RequiredArgsConstructor
public class PasswordResetCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetCleanupJob.class);

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.password-reset.cleanup-grace-days}")
    private int graceDays;

    @Scheduled(cron = "0 30 0 * * *", zone = "America/Bogota")
    @Transactional
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(graceDays);
        int deleted = passwordResetTokenRepository.deleteExpiredBefore(cutoff);
        log.info("PasswordResetCleanupJob: deleted {} expired password-reset tokens (older than {})",
                deleted, cutoff);
    }
}
