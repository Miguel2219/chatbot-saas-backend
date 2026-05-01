package com.chatbotsaas.chatbot_saas.quota.controller;

import com.chatbotsaas.chatbot_saas.auth.service.AuthService;
import com.chatbotsaas.chatbot_saas.quota.dto.response.QuotaAdminRowDto;
import com.chatbotsaas.chatbot_saas.quota.dto.response.QuotaCurrentDto;
import com.chatbotsaas.chatbot_saas.quota.dto.response.QuotaCycleHistoryDto;
import com.chatbotsaas.chatbot_saas.quota.service.QuotaService;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de consulta del sistema de cuotas.
 * <p>
 * Protegidos por {@code quotas:view} (módulo propio, separado de {@code dashboard}).
 * Por decisión de negocio, ese permiso se otorga sólo al rol ADMIN vía V4 —
 * USER/ADVISER no pueden ver el módulo de cuotas.
 * <p>
 * Reglas de tenant scoping adicionales:
 * <ul>
 *   <li>{@code /current}, {@code /history}: USER siempre usa su propio tenant (se ignora {@code ?tenantId}).
 *       ADMIN con su propio tenant puede pasar {@code ?tenantId} para ver el de otro; ADMIN sin tenant
 *       DEBE pasar {@code ?tenantId} (si no → 400).</li>
 *   <li>{@code /admin/all}: solo ADMIN (doble puerta — permiso + check {@code isAdmin()}).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/quota")
public class QuotaController {

    private final QuotaService quotaService;
    private final AuthService authService;

    public QuotaController(QuotaService quotaService, AuthService authService) {
        this.quotaService = quotaService;
        this.authService = authService;
    }

    @GetMapping("/current")
    @PreAuthorize("@permissionChecker.hasPermission('quotas', 'view')")
    public ResponseEntity<QuotaCurrentDto> getCurrent(
            @RequestParam(value = "tenantId", required = false) UUID tenantIdParam
    ) {
        UUID effectiveTenantId = resolveTenantScoped(tenantIdParam);
        return ResponseEntity.ok(quotaService.getCurrent(effectiveTenantId));
    }

    @GetMapping("/history")
    @PreAuthorize("@permissionChecker.hasPermission('quotas', 'view')")
    public ResponseEntity<List<QuotaCycleHistoryDto>> getHistory(
            @RequestParam(value = "tenantId", required = false) UUID tenantIdParam
    ) {
        UUID effectiveTenantId = resolveTenantScoped(tenantIdParam);
        return ResponseEntity.ok(quotaService.getHistory(effectiveTenantId));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("@permissionChecker.hasPermission('quotas', 'view')")
    public ResponseEntity<List<QuotaAdminRowDto>> getAdminAll() {
        User me = requireUser();
        if (!me.isAdmin()) {
            throw new AppException("Solo administradores pueden ver el consumo global", HttpStatus.FORBIDDEN);
        }
        return ResponseEntity.ok(quotaService.getAllCurrent());
    }

    // ------------------------------------------------------------------------
    // Helpers internos
    // ------------------------------------------------------------------------

    /**
     * Resuelve qué tenantId usar según el rol del caller:
     * <ul>
     *   <li>USER: siempre su propio tenant (ignora {@code tenantIdParam} si viene).</li>
     *   <li>ADMIN con tenant: si no pasa {@code tenantIdParam}, usa el suyo; si pasa, usa el del param.</li>
     *   <li>ADMIN sin tenant: {@code tenantIdParam} es obligatorio → 400 si falta.</li>
     * </ul>
     * Además bloquea el USER que intenta consultar un tenant distinto al suyo (403).
     */
    private UUID resolveTenantScoped(UUID tenantIdParam) {
        User me = requireUser();
        boolean admin = me.isAdmin();
        Tenant myTenant = me.getTenant();

        if (admin) {
            if (tenantIdParam != null) {
                return tenantIdParam;
            }
            if (myTenant != null) {
                return myTenant.getId();
            }
            throw new AppException("tenantId requerido", HttpStatus.BAD_REQUEST);
        }

        // USER normal — debe tener tenant.
        if (myTenant == null) {
            throw new AppException("Usuario sin tenant asociado", HttpStatus.FORBIDDEN);
        }

        if (tenantIdParam != null && !tenantIdParam.equals(myTenant.getId())) {
            throw new AppException("No puedes consultar el tenant de otro usuario", HttpStatus.FORBIDDEN);
        }

        return myTenant.getId();
    }

    private User requireUser() {
        User me = authService.getUserAuthenticated();
        if (me == null) {
            throw new AppException("Usuario no autenticado", HttpStatus.UNAUTHORIZED);
        }
        return me;
    }
}
