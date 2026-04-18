package com.chatbotsaas.chatbot_saas.tenant.controller;

import com.chatbotsaas.chatbot_saas.tenant.dto.request.CreateTenantRequestDto;
import com.chatbotsaas.chatbot_saas.tenant.dto.response.TenantResponseDto;
import com.chatbotsaas.chatbot_saas.tenant.dto.response.TenantResponseSelectDto;
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

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> createTenant(
            @Valid @RequestBody CreateTenantRequestDto request) {
        tenantService.createTenant(request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/get_tenants")
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TenantResponseSelectDto>> getTenantsSelect() {
        return new ResponseEntity<>(tenantService.getTenantsSelect(), HttpStatus.OK);
    }

}
