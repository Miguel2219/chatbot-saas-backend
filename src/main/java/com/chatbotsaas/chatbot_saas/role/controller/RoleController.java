package com.chatbotsaas.chatbot_saas.role.controller;

import com.chatbotsaas.chatbot_saas.role.dto.request.UpdateRoleRequestDto;
import com.chatbotsaas.chatbot_saas.role.dto.response.RoleResponseDto;
import com.chatbotsaas.chatbot_saas.role.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('roles', 'view')")
    public ResponseEntity<List<RoleResponseDto>> getAllRoles() {
        return new ResponseEntity<>(roleService.getAllRoles(), HttpStatus.OK);
    }

    @PutMapping("/{role_id}")
    @PreAuthorize("@permissionChecker.hasPermission('roles', 'edit')")
    public ResponseEntity<RoleResponseDto> updateRole(
            @PathVariable(value = "role_id") UUID roleId,
            @Valid @RequestBody UpdateRoleRequestDto request) {
        return new ResponseEntity<>(
                roleService.updateRole(roleId, request),
                HttpStatus.OK
        );
    }
}
