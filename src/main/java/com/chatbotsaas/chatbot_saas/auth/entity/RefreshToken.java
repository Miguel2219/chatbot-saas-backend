package com.chatbotsaas.chatbot_saas.auth.entity;

import com.chatbotsaas.chatbot_saas.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Refresh token persistido (rotación + revocación).
 *
 * Nunca almacenamos el token en claro: solo el SHA-256 hex del string que el
 * cliente conoce. SHA-256 se eligió sobre BCrypt porque es determinístico
 * (indexable) y el input ya tiene 256 bits de entropía — no hace falta salt
 * contra diccionarios. Ver {@code RefreshTokenService.sha256Hex}.
 *
 * Ciclo de vida:
 * - Se emite uno en cada /login y en cada /refresh exitoso.
 * - Al validar, se marca {@code revoked=true} (rotación) y se emite uno nuevo.
 * - Si llega uno con {@code revoked=true}, es señal de reuso → revocamos
 *   TODOS los del usuario (posible robo).
 * - Un job diario borra filas con {@code expires_at} muy atrás en el tiempo.
 */
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_refresh_tokens_token_hash", columnList = "token_hash", unique = true),
                @Index(name = "idx_refresh_tokens_user_id",    columnList = "user_id"),
                @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SHA-256 hex del token opaco (64 chars). Único, indexado. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
