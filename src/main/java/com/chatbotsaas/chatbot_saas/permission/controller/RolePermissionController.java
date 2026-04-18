package com.chatbotsaas.chatbot_saas.permission.controller;

import com.chatbotsaas.chatbot_saas.module.dto.ModulePermissionsDto;
import com.chatbotsaas.chatbot_saas.permission.dto.UpdateRolePermissionsRequestDto;
import com.chatbotsaas.chatbot_saas.permission.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/role-permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @GetMapping("/{role_id}")
    public ResponseEntity<List<ModulePermissionsDto>> getPermissionsByRole(
            @PathVariable(value = "role_id") UUID roleId
            ) {
        return new ResponseEntity<>(
                rolePermissionService.getPermissionsByRole(roleId),
                HttpStatus.OK);
    }

    @PutMapping("/{role_id}")
    public ResponseEntity<Void> updateRolePermissions(
            @PathVariable(value = "role_id") UUID roleId,
            @RequestBody UpdateRolePermissionsRequestDto request) {
        rolePermissionService.updateRolePermissions(roleId, request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
