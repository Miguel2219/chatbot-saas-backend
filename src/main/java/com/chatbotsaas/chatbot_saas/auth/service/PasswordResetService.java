package com.chatbotsaas.chatbot_saas.auth.service;

import com.chatbotsaas.chatbot_saas.auth.dto.ValidateResetTokenResponse;
import com.chatbotsaas.chatbot_saas.auth.entity.PasswordResetToken;
import com.chatbotsaas.chatbot_saas.auth.repository.PasswordResetTokenRepository;
import com.chatbotsaas.chatbot_saas.auth.repository.RefreshTokenRepository;
import com.chatbotsaas.chatbot_saas.notification.service.EmailService;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Flujo "forgot password" + "reset password".
 *
 * 3 puntos de entrada, todos públicos (sin sesión):
 * <ol>
 *   <li>{@link #requestReset(String)} — genera un token opaco, lo persiste
 *       como SHA-256 hex y dispara el email con el link. Rate-limited por
 *       user + ventana para evitar spam de correo.</li>
 *   <li>{@link #validate(String)} — chequea que el token exista, no haya
 *       expirado ni sido usado. Devuelve el email enmascarado para que el
 *       form muestre "a quién va a resetear".</li>
 *   <li>{@link #confirmReset(String, String)} — consume el token, cambia la
 *       password, revoca todos los refresh tokens activos del user (cierre
 *       global de sesiones) y manda email de confirmación.</li>
 * </ol>
 *
 * Anti-enumeración
 * ----------------
 * {@code requestReset} <b>nunca</b> lanza: si el email no existe, retorna
 * en silencio. El controller responde siempre 200 con el mismo mensaje
 * genérico. Para equiparar CPU entre ambos paths se hace un
 * {@code sha256Hex("dummy")} en el branch "not found". El envío de email
 * es {@code @Async} (ver {@link EmailService}) → response rápida idéntica
 * exista o no el user.
 *
 * Patrón de tokens
 * ----------------
 * Mismo esquema que {@link RefreshTokenService}: 32 bytes SecureRandom en
 * base64url (≈256 bits de entropía) → SHA-256 hex en DB. El string raw
 * solo vive en la URL del email.
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final String CODE_INVALID = "INVALID_RESET_TOKEN";
    private static final String CODE_WEAK    = "WEAK_PASSWORD";
    private static final int TOKEN_BYTES = 32;

    /** Mínimo 8 chars, al menos 1 letra y al menos 1 número. */
    private static final Pattern PASSWORD_REGEX = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-reset.token-expiration-ms}")
    private long tokenExpirationMs;

    @Value("${app.password-reset.rate-limit-window-min}")
    private int rateLimitWindowMin;

    @Value("${app.password-reset.rate-limit-max-attempts}")
    private int rateLimitMaxAttempts;

    @Value("${settings.url.front}")
    private String frontUrl;

    // ─── 1. Solicitar reset ──────────────────────────────────────────────

    /**
     * Emite un token y dispara el email. <b>No lanza nunca</b>: el controller
     * responde siempre 200 con mensaje genérico (anti-enumeración).
     *
     * Ramas:
     * <ul>
     *   <li><b>user no existe</b>: dummy hash + log + return.</li>
     *   <li><b>rate-limited</b>: log + return (no se manda email).</li>
     *   <li><b>OK</b>: marca tokens previos como usados, emite uno nuevo,
     *       manda email async, log.</li>
     * </ul>
     */
    @Transactional
    public void requestReset(String rawEmail) {
        User user = userRepository.findByEmail(rawEmail).orElse(null);
        if (user == null) {
            // CPU-parity dummy: el path "no existe" no debería ser
            // observablemente más rápido que el path "existe".
            sha256Hex("dummy");
            log.info("password_reset_requested email_mask={} result=not_found", maskEmail(rawEmail));
            return;
        }

        LocalDateTime windowCutoff = LocalDateTime.now().minusMinutes(rateLimitWindowMin);
        int recent = passwordResetTokenRepository
                .countByUser_UserIdAndCreatedAtAfter(user.getUserId(), windowCutoff);
        if (recent >= rateLimitMaxAttempts) {
            log.info("password_reset_requested email_mask={} result=rate_limited",
                    maskEmail(rawEmail));
            return;
        }

        // Invalidamos tokens previos no consumidos del user — si pidió 2
        // emails, solo el último link funciona.
        passwordResetTokenRepository.markAllUnusedAsUsedForUser(user.getUserId());

        byte[] random = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(random);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(random);

        PasswordResetToken entity = PasswordResetToken.builder()
                .user(user)
                .tokenHash(sha256Hex(raw))
                .expiresAt(LocalDateTime.now().plusNanos(tokenExpirationMs * 1_000_000L))
                .used(false)
                .build();
        passwordResetTokenRepository.save(entity);

        String resetLink = buildResetLink(raw);
        String userName = resolveUserName(user);
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink, userName);

        log.info("password_reset_requested email_mask={} result=queued", maskEmail(rawEmail));
    }

    // ─── 2. Validar token ────────────────────────────────────────────────

    /**
     * Chequea que el token sea válido y devuelve el email enmascarado. El
     * front lo llama al cargar /reset-password?token=… para mostrarle al
     * user a qué cuenta apunta el link antes de pedirle la nueva password.
     * <p>
     * Lanza {@code 400 INVALID_RESET_TOKEN} si el token no existe, expiró
     * o ya fue usado — todos los casos colapsan en la misma respuesta para
     * no dar pistas al atacante.
     */
    @Transactional(readOnly = true)
    public ValidateResetTokenResponse validate(String raw) {
        PasswordResetToken token = loadValid(raw);
        return new ValidateResetTokenResponse(maskEmail(token.getUser().getEmail()));
    }

    // ─── 3. Confirmar reset ──────────────────────────────────────────────

    /**
     * Consume el token y cambia la password. Además:
     * <ul>
     *   <li>Revoca <b>todos</b> los refresh tokens activos del user →
     *       cualquier sesión abierta en otro dispositivo muere al siguiente
     *       /refresh (que devolverá 401).</li>
     *   <li>Pone {@code must_change_password=false}.</li>
     *   <li>Dispara email de confirmación (async).</li>
     * </ul>
     * Lanza {@code 400 INVALID_RESET_TOKEN} si el token no es válido o
     * {@code 400 WEAK_PASSWORD} si la nueva password no cumple la regla
     * (≥8, letra, número).
     */
    @Transactional
    public void confirmReset(String raw, String newPassword) {
        if (!isPasswordValid(newPassword)) {
            throw new AppException(
                    "La contraseña debe tener al menos 8 caracteres e incluir letras y números.",
                    HttpStatus.BAD_REQUEST,
                    CODE_WEAK
            );
        }

        PasswordResetToken token = loadValid(raw);
        User user = token.getUser();

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        int revoked = refreshTokenRepository.revokeAllForUser(user.getUserId());

        String userName = resolveUserName(user);
        emailService.sendPasswordChangedConfirmation(user.getEmail(), userName);

        log.info("password_reset_success user={} refresh_tokens_revoked={}",
                user.getUserId(), revoked);
    }

    // ─── helpers privados ────────────────────────────────────────────────

    /**
     * Trae y valida el token. Lanza {@code 400 INVALID_RESET_TOKEN} para
     * cualquier fallo (no existe / expirado / consumido) — no queremos
     * discriminar casos al cliente.
     */
    private PasswordResetToken loadValid(String raw) {
        if (raw == null || raw.isBlank()) {
            throw invalidToken();
        }
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHash(sha256Hex(raw))
                .orElseThrow(this::invalidToken);
        if (token.isUsed() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw invalidToken();
        }
        return token;
    }

    private AppException invalidToken() {
        return new AppException(
                "El enlace de recuperación no es válido o ha expirado.",
                HttpStatus.BAD_REQUEST,
                CODE_INVALID
        );
    }

    private boolean isPasswordValid(String password) {
        return password != null && PASSWORD_REGEX.matcher(password).matches();
    }

    private String buildResetLink(String rawToken) {
        String base = (frontUrl == null || frontUrl.isBlank()) ? "" : frontUrl.replaceAll("/+$", "");
        return base + "/reset-password?token=" + rawToken;
    }

    private String resolveUserName(User user) {
        if (user.getPerson() != null && user.getPerson().getName() != null) {
            return user.getPerson().getName();
        }
        return "";
    }

    /**
     * Enmascara {@code local@domain} → {@code p***o@domain}. Si el local
     * tiene 1-2 chars, se reemplaza entero por {@code ***}.
     */
    static String maskEmail(String email) {
        if (email == null || email.isBlank()) return "***";
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at); // incluye '@'
        if (local.length() <= 2) {
            return "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
