package com.chatbotsaas.chatbot_saas.auth.entity;

import com.chatbotsaas.chatbot_saas.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Token de reset de contraseña (flujo "forgot password").
 *
 * Mismo patrón que {@link RefreshToken}: nunca almacenamos el token en claro,
 * solo el SHA-256 hex (64 chars) — determinístico e indexable. El string raw
 * únicamente viaja en la URL del email y nunca vuelve a vivir en el server.
 *
 * Ciclo de vida:
 * - Se emite uno en POST /auth/forgot-password. Antes de insertar, todos los
 *   tokens previos del user no consumidos se marcan {@code used=true} para
 *   que solo el más reciente sea válido.
 * - Al confirmar el reset (POST /auth/reset-password) se pone {@code used=true}
 *   y además se revocan TODOS los refresh tokens del user (cierre de sesiones
 *   activas) — esa parte la hace el service, no esta entity.
 * - Un job diario borra filas con {@code expires_at} anterior al grace-days
 *   configurado (por defecto 7 días) — ver {@code PasswordResetCleanupJob}.
 *
 * El índice compuesto (user_id, created_at) existe para el rate-limit: la
 * query cuenta filas del user en los últimos N minutos antes de emitir.
 */
@Entity
@Table(
        name = "password_reset_tokens",
        indexes = {
                @Index(name = "idx_prt_token_hash",       columnList = "token_hash", unique = true),
                @Index(name = "idx_prt_user_id_created",  columnList = "user_id, created_at"),
                @Index(name = "idx_prt_expires_at",       columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

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

    @Column(name = "used", nullable = false)
    private boolean used;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    }
}
