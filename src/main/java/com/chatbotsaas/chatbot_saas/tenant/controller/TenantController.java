package com.chatbotsaas.chatbot_saas.tenant.controller;

import com.chatbotsaas.chatbot_saas.tenant.dto.request.CreateTenantRequestDto;
import com.chatbotsaas.chatbot_saas.tenant.dto.request.UpdateTenantRequestDto;
import com.chatbotsaas.chatbot_saas.tenant.dto.response.TenantResponseDto;
import com.chatbotsaas.chatbot_saas.tenant.dto.response.TenantResponseSelectDto;
import com.chatbotsaas.chatbot_saas.tenant.dto.response.TenantUserSelectDto;
import com.chatbotsaas.chatbot_saas.tenant.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('tenants', 'create')")
    public ResponseEntity<Void> createTenant(
            @Valid @RequestBody CreateTenantRequestDto request) {
        tenantService.createTenant(request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/get_tenants")
    @PreAuthorize("@permissionChecker.hasPermission('tenants', 'view')")
    public ResponseEntity<Page<TenantResponseDto>> getTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String orderBy,
            @RequestParam(defaultValue = "desc") String order) {

        Pageable pageable = order.equalsIgnoreCase("desc")
                ? PageRequest.of(page, size, Sort.by(orderBy).descending())
                : PageRequest.of(page, size, Sort.by(orderBy).ascending());

        return new ResponseEntity<>(tenantService.getTenants(pageable), HttpStatus.OK);
    }

    @GetMapping("/get_tenants_select")
    @PreAuthorize("@permissionChecker.hasPermission('tenants', 'view')")
    public ResponseEntity<List<TenantResponseSelectDto>> getTenantsSelect() {
        return new ResponseEntity<>(tenantService.getTenantsSelect(), HttpStatus.OK);
    }

    /**
     * Patch-style update del tenant. Acepta {@code owner_user_id} opcional — dispara el swap
     * de TENANT_OWNER en el service.
     */
    @PutMapping("/{tenantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateTenant(
            @PathVariable UUID tenantId,
            @Valid @RequestBody UpdateTenantRequestDto dto) {
        tenantService.updateTenant(tenantId, dto);
        return ResponseEntity.ok().build();
    }

    /**
     * Lista los users del tenant para poblar el selector del modal "Editar tenant" en el
     * frontend. Marca al TENANT_OWNER actual con {@code is_current_owner=true}.
     */
    @GetMapping("/{tenantId}/users")
    @PreAuthorize("hasRole('ADMIN') or @permissionChecker.hasPermission('bots', 'create') or @permissionChecker.hasPermission('bots', 'edit')")
    public ResponseEntity<List<TenantUserSelectDto>> getTenantUsers(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantService.getTenantUsers(tenantId));
    }

}
