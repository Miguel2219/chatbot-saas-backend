package com.chatbotsaas.chatbot_saas.dashboard.controller;

import com.chatbotsaas.chatbot_saas.auth.service.AuthService;
import com.chatbotsaas.chatbot_saas.dashboard.dto.response.DashboardSummaryDto;
import com.chatbotsaas.chatbot_saas.dashboard.service.DashboardService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthService authService;

    public DashboardController(DashboardService dashboardService, AuthService authService) {
        this.dashboardService = dashboardService;
        this.authService = authService;
    }

    @GetMapping("/summary")
    @PreAuthorize("@permissionChecker.hasPermission('dashboard', 'view')")
    public ResponseEntity<DashboardSummaryDto> getSummary(
            @RequestParam(value = "tenantId", required = false) UUID tenantIdParam
    ) {
        UUID effectiveTenantId = resolveTenantScoped(tenantIdParam);
        return ResponseEntity.ok(dashboardService.getSummary(effectiveTenantId));
    }

    // ------------------------------------------------------------------------
    // Helpers internos — replica el patrón de QuotaController.resolveTenantScoped
    // ------------------------------------------------------------------------

    /**
     * Resuelve qué tenantId usar según el rol del caller. Ver reglas en el javadoc de clase.
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
