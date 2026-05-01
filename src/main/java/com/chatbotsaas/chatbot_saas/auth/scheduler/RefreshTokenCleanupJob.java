package com.chatbotsaas.chatbot_saas.auth.scheduler;

import com.chatbotsaas.chatbot_saas.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Job diario que borra refresh tokens expirados hace más tiempo que el
 * {@code jwt.refresh-token-cleanup-grace-days} configurado.
 *
 * La ventana de gracia (por defecto 30 días) permite auditar durante un
 * tiempo antes de borrar — útil para investigar un incidente ("¿este RT
 * estaba revocado cuando el atacante lo usó?"). Expirado ≠ borrable al
 * instante.
 *
 * Cron: {@code 0 15 0 * * *} en zona América/Bogotá (00:15 AM local). El
 * shift de 10 minutos respecto a {@code QuotaCycleLogJob} (00:05) evita
 * que los dos jobs toquen DB al mismo tiempo.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-cleanup-grace-days}")
    private int graceDays;

    @Scheduled(cron = "0 15 0 * * *", zone = "America/Bogota")
    @Transactional
    public void cleanup() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(graceDays);
        int deleted = refreshTokenRepository.deleteExpiredBefore(cutoff);
        log.info("RefreshTokenCleanupJob: deleted {} expired refresh tokens (older than {})",
                deleted, cutoff);
    }
}
