package com.chatbotsaas.chatbot_saas.auth.repository;

import com.chatbotsaas.chatbot_saas.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Lookup determinístico por hash SHA-256. Único acceso por "token" posible
     * — el valor raw nunca se guarda.
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Cantidad de tokens emitidos al user después de {@code cutoff}.
     * Alimenta el rate-limit de POST /forgot-password (ventana deslizante
     * definida por {@code app.password-reset.rate-limit-window-min}).
     *
     * No filtra por {@code used=false}: queremos contar también los que el
     * user ya consumió dentro de la ventana — impedir que haga spam del
     * endpoint incluso completando el reset entre medio.
     */
    int countByUser_UserIdAndCreatedAtAfter(UUID userId, LocalDateTime cutoff);

    /**
     * Marca como usados todos los tokens no consumidos del user. Se llama
     * justo antes de emitir uno nuevo para que solo el más reciente sea
     * válido — si el user pidió 2 emails, el primer link queda inutilizado.
     *
     * @return cantidad de filas afectadas.
     */
    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.used = true " +
            "WHERE p.user.userId = :userId AND p.used = false")
    int markAllUnusedAsUsedForUser(@Param("userId") UUID userId);

    /**
     * Borra tokens expirados hace más tiempo que la ventana de gracia. El
     * cutoff lo define {@code PasswordResetCleanupJob} según
     * {@code app.password-reset.cleanup-grace-days}.
     *
     * @return cantidad de filas borradas.
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
