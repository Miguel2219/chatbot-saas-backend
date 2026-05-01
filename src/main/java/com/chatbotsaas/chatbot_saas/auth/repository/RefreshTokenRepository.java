package com.chatbotsaas.chatbot_saas.auth.repository;

import com.chatbotsaas.chatbot_saas.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    /**
     * Lookup determinístico por hash. Es el único acceso por "token" que
     * tenemos — el string original nunca se guarda.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoca todos los RT activos de un usuario. Se dispara cuando detectamos
     * reuso de un RT ya revocado (posible robo) — cerramos el portón completo
     * y obligamos a re-loguear.
     *
     * @return cantidad de filas afectadas (útil para logging).
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true " +
            "WHERE rt.user.userId = :userId AND rt.revoked = false")
    int revokeAllForUser(@Param("userId") UUID userId);

    /**
     * Limpieza: borra RTs expirados hace mucho. El cutoff lo define el
     * scheduler según {@code jwt.refresh-token-cleanup-grace-days}.
     *
     * @return cantidad de filas borradas.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
