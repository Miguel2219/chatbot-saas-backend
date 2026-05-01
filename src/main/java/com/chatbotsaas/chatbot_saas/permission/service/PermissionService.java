package com.chatbotsaas.chatbot_saas.permission.service;

import com.chatbotsaas.chatbot_saas.module.constant.ModuleConstants;
import com.chatbotsaas.chatbot_saas.module.dto.ModulePermissionDto;
import com.chatbotsaas.chatbot_saas.module.repository.ModuleRepository;
import com.chatbotsaas.chatbot_saas.permission.entity.Permission;
import com.chatbotsaas.chatbot_saas.role.entity.Role;
import com.chatbotsaas.chatbot_saas.tenant.enums.ImplementationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.chatbotsaas.chatbot_saas.module.entity.Module;


import java.util.*;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final ModuleRepository moduleRepository;

    public List<ModulePermissionDto> getModulesForUser(
            List<Role> roles,
            ImplementationType implementationType
    ) {
        Map<UUID, Set<String>> permissionsByModule = new HashMap<>();

        for (Role role : roles) {
            for (Permission permission : role.getPermissions()) {
                UUID moduleId = permission.getModule().getModuleId();
                permissionsByModule
                        .computeIfAbsent(moduleId, k -> new HashSet<>())
                        .add(permission.getAction().toLowerCase());
            }
        }

        List<Module> allModules = moduleRepository.findAllByOrderByDisplayOrderAsc();
        List<ModulePermissionDto> result = new ArrayList<>();

        for (Module module : allModules) {
            Set<String> userActions = permissionsByModule.getOrDefault(
                    module.getModuleId(), new HashSet<>()
            );

            if (!userActions.contains("view")) continue;

            if (module.getModuleId().equals(ModuleConstants.WHATSAPP_MODULE) && implementationType == ImplementationType.WIDGET) continue;

            Map<String, Boolean> permissions = new LinkedHashMap<>();

            module.getPermissions().forEach(p -> {
                String action = p.getAction().toLowerCase();
                permissions.put(action, userActions.contains(action));
            });

            result.add(ModulePermissionDto.builder()
                    .moduleId(module.getModuleId())
                    .name(module.getName())
                    .route(module.getRoute())
                    .icon(module.getIcon())
                    .displayOrder(module.getDisplayOrder())
                    .permissions(permissions)
                    .build());

        }

        return result;
    }
}
