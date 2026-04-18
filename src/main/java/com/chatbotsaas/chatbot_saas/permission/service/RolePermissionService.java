package com.chatbotsaas.chatbot_saas.permission.service;

import com.chatbotsaas.chatbot_saas.module.dto.ModulePermissionsDto;
import com.chatbotsaas.chatbot_saas.module.entity.Module;
import com.chatbotsaas.chatbot_saas.module.repository.ModuleRepository;
import com.chatbotsaas.chatbot_saas.permission.dto.PermissionCheckDto;
import com.chatbotsaas.chatbot_saas.permission.dto.UpdateRolePermissionsRequestDto;
import com.chatbotsaas.chatbot_saas.permission.entity.Permission;
import com.chatbotsaas.chatbot_saas.permission.repository.PermissionRepository;
import com.chatbotsaas.chatbot_saas.role.entity.Role;
import com.chatbotsaas.chatbot_saas.role.repository.RoleRepository;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RolePermissionService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final ModuleRepository moduleRepository;

    @Transactional
    public List<ModulePermissionsDto> getPermissionsByRole(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException("Role not found", HttpStatus.NOT_FOUND));

        List<Module> allModules = moduleRepository.findAllByOrderByDisplayOrderAsc();

        Set<UUID> rolePermissionsIds = role.getPermissions()
                .stream()
                .map(Permission::getPermissionId)
                .collect(Collectors.toSet());

        return allModules.stream().map(module -> {
            List<Permission> modulePermissions = permissionRepository.findByModule_ModuleId(module.getModuleId());

            List<PermissionCheckDto> checks = modulePermissions.stream()
                    .map(permission -> PermissionCheckDto.builder()
                            .permissionId(permission.getPermissionId())
                            .action(permission.getAction())
                            .granted(rolePermissionsIds.contains(permission.getPermissionId()))
                            .build()
                    ).toList();
            return ModulePermissionsDto.builder()
                    .moduleId(module.getModuleId())
                    .moduleName(module.getName())
                    .icon(module.getIcon())
                    .permissions(checks)
                    .build();
        }).toList();
    }

    @Transactional
    public void updateRolePermissions(UUID roleId, UpdateRolePermissionsRequestDto request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new AppException("Role not found", HttpStatus.NOT_FOUND));

        List<Permission> newPermissions = permissionRepository.findAllById(request.getPermissionsIds());

        role.getPermissions().clear();
        role.getPermissions().addAll(newPermissions);
        roleRepository.save(role);
    }

}
