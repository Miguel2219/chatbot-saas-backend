package com.chatbotsaas.chatbot_saas.shared.security;

import com.chatbotsaas.chatbot_saas.auth.service.AuthService;
import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Validador centralizado de aislamiento multi-tenant.
 *
 * <p>Encapsula la regla común de "el caller sólo puede tocar recursos de su
 * propio tenant (salvo que sea ADMIN)". Antes estaba duplicada —
 * y en varios servicios, ausente— ahora vive en un único lugar para que:
 *
 * <ul>
 *   <li>El mensaje y status de error sea consistente en toda la API.</li>
 *   <li>Sea trivial extender la regla (p.ej. añadir logs de auditoría o un
 *       día envolverla con {@code SET LOCAL app.tenant_id = ...} cuando
 *       migremos a Row Level Security).</li>
 *   <li>Los nuevos servicios que accedan a recursos tenant-scoped tengan un
 *       helper obvio que usar — y la auditoría futura sea un grep simple
 *       sobre los usos de este bean.</li>
 * </ul>
 *
 * <p>Mensaje fijo: {@code "No tienes permiso para acceder a este recurso"} —
 * no revelar el tenant del recurso ni identificadores: principio de no
 * filtrar información en los mensajes de error (usuario no autorizado no
 * debería poder hacer enumeración por diferencia de mensajes).
 */
@Component
public class TenantAccessValidator {

    /** Mensaje estándar de 403 — idéntico en todos los callers (anti-enumeración). */
    private static final String FORBIDDEN_MESSAGE = "No tienes permiso para acceder a este recurso";

    private final AuthService authService;

    public TenantAccessValidator(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Verifica que el usuario autenticado pueda operar sobre el {@code bot}
     * dado. ADMIN siempre pasa. Non-admin: sólo si el tenant del bot coincide
     * con el del usuario.
     *
     * @throws AppException 403 si el caller no tiene permiso.
     */
    public void assertCanAccessBot(Bot bot) {
        UUID botTenantId = bot.getTenant() != null ? bot.getTenant().getId() : null;
        assertCanAccessTenant(botTenantId);
    }

    /**
     * Verifica que el usuario autenticado pueda operar sobre recursos del
     * tenant dado. ADMIN siempre pasa. Non-admin: sólo si
     * {@code tenantId.equals(user.tenant.id)}.
     *
     * <p>Se usa directamente cuando el recurso no es un {@link Bot} pero
     * igual carga un {@code tenant_id} (p.ej. conversations / leads /
     * documents que pasan por el bot pero ya tienen el tenant resuelto
     * upstream).
     *
     * @throws AppException 403 si el caller no tiene permiso.
     */
    public void assertCanAccessTenant(UUID tenantId) {
        User user = authService.getUserAuthenticated();
        if (user == null) {
            throw new AppException("Usuario no autenticado", HttpStatus.UNAUTHORIZED);
        }
        if (user.isAdmin()) {
            return;
        }
        UUID myTenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        if (myTenantId == null || tenantId == null || !myTenantId.equals(tenantId)) {
            throw new AppException(FORBIDDEN_MESSAGE, HttpStatus.FORBIDDEN);
        }
    }
}
