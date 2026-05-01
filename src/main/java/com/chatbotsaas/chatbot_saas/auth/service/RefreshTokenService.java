package com.chatbotsaas.chatbot_saas.auth.service;

import com.chatbotsaas.chatbot_saas.auth.entity.RefreshToken;
import com.chatbotsaas.chatbot_saas.auth.repository.RefreshTokenRepository;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Emisión, validación y revocación de refresh tokens.
 *
 * Diseño:
 * - El token "opaco" que viaja al cliente son 32 bytes aleatorios en
 *   base64url (256 bits de entropía, suficiente contra fuerza bruta).
 * - En DB guardamos solo el SHA-256 hex (determinístico → indexable). Nunca
 *   se persiste ni se loggea el valor en claro.
 * - Rotación: {@link #validateAndRotate(String)} marca el RT consumido como
 *   revoked=true y el caller emite uno nuevo con {@link #issueFor(User)}.
 * - Reuso detectado = revocación masiva del user. Si un RT ya revocado
 *   vuelve a llegar, asumimos que se lo copiaron del storage de la víctima
 *   y que el atacante lo usó; revocamos todos los RTs del user para botarlo.
 *
 * Transaccionalidad: cada método público corre en su propia Tx para que el
 * DELETE/UPDATE se commite aunque el caller falle después.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final String ERROR_CODE = "INVALID_REFRESH_TOKEN";
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    /**
     * Emite un refresh token nuevo para el user. Devuelve el string EN CLARO
     * — es la única oportunidad de verlo, el caller lo debe mandar al
     * cliente y olvidarlo. La DB solo guarda el hash.
     */
    @Transactional
    public String issueFor(User user) {
        byte[] random = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(random);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(random);

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(sha256Hex(raw))
                .expiresAt(LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000L))
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);

        return raw;
    }

    /**
     * Valida un refresh token opaco y lo marca como consumido (rotación).
     *
     * Posibles salidas:
     * - token inexistente / expirado → 401 INVALID_REFRESH_TOKEN.
     * - token ya revocado → reuso detectado → revocamos TODOS los del user
     *   y lanzamos 401 INVALID_REFRESH_TOKEN.
     * - token válido → marca revoked=true y devuelve la entidad (con el User
     *   cargado) para que el caller emita los tokens nuevos.
     */
    @Transactional
    public RefreshToken validateAndRotate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw invalid();
        }
        String hash = sha256Hex(raw);
        RefreshToken entity = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(this::invalid);

        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw invalid();
        }

        if (entity.isRevoked()) {
            // Reuso: alguien está usando un RT que ya consumimos (rotamos).
            // Cortamos la sesión por completo para el usuario.
            int affected = refreshTokenRepository.revokeAllForUser(entity.getUser().getUserId());
            log.warn("refresh_token reuse detected — user={} revoked_count={}",
                    entity.getUser().getUserId(), affected);
            throw invalid();
        }

        entity.setRevoked(true);
        refreshTokenRepository.save(entity);
        return entity;
    }

    /**
     * Marca el RT como revocado si existe. Idempotente: si no existe o ya
     * estaba revocado, no hace nada y no lanza — el cliente solo quiere
     * "olvídate de esta sesión", no le importa si el server ya la olvidó.
     */
    @Transactional
    public void revoke(String raw) {
        if (raw == null || raw.isBlank()) return;
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenHash(sha256Hex(raw));
        found.ifPresent(rt -> {
            if (!rt.isRevoked()) {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            }
        });
    }

    // TODO(MVP): wrapper público de revokeAllForUser para el endpoint
    // "logout-all" (cerrar sesión en todos los dispositivos). Dejado
    // comentado hasta que se reactive ese flujo. Mientras tanto, el
    // único consumer de la revocación masiva es PasswordResetService,
    // que usa el repository directo.
    //
    // /**
    //  * Revoca todos los refresh tokens activos del usuario. Expuesto
    //  * para el endpoint POST /api/auth/logout-all y reusable también
    //  * desde PasswordResetService al confirmar un reset.
    //  *
    //  * Idempotente: si no hay tokens activos, retorna 0.
    //  */
    // @Transactional
    // public int revokeAllForUserId(UUID userId) {
    //     int affected = refreshTokenRepository.revokeAllForUser(userId);
    //     log.info("refresh_tokens revoked in bulk — user={} count={}", userId, affected);
    //     return affected;
    // }

    private AppException invalid() {
        return new AppException("Invalid or expired refresh token", HttpStatus.UNAUTHORIZED, ERROR_CODE);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es parte del JRE estándar — si falla acá algo está muy roto.
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
