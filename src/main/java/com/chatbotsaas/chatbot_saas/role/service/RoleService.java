package com.chatbotsaas.chatbot_saas.role.service;

import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.role.dto.request.UpdateRoleRequestDto;
import com.chatbotsaas.chatbot_saas.role.dto.response.RoleResponseDto;
import com.chatbotsaas.chatbot_saas.role.entity.Role;
import com.chatbotsaas.chatbot_saas.role.repository.RoleRepository;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    // Roles de sistema cuyos nombres están cableados en la capa de seguridad
    // (AuthService#getRoles, @PreAuthorize hasRole('ADMIN'), etc.). Renombrarlos
    // rompería el login/autorización de todos los tenants, así que el update
    // los bloquea con 403.
    private static final Set<UUID> SYSTEM_ROLE_IDS = Set.of(
            RoleConstants.ADMIN_ROLE,
            RoleConstants.USER_ROLE,
            RoleConstants.TENANT_OWNER_ROLE
    );

    @Transactional(readOnly = true)
    public List<RoleResponseDto> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(role -> RoleResponseDto.builder()
                        .roleId(role.getRoleId())
                        .name(role.getName())
                        .description(role.getDescription())
                        .build())
                .toList();
    }

    @Transactional
    public RoleResponseDto updateRole(UUID roleId, UpdateRoleRequestDto request) {
        Role role = roleRepository.findById(roleId).orElseThrow(
                () -> new AppException("Rol no encontrado", HttpStatus.NOT_FOUND)
        );

        // Los roles de sistema son inmutables — renombrarlos rompe la lógica
        // de autorización basada en nombres (hasRole('ADMIN'), etc.).
        if (SYSTEM_ROLE_IDS.contains(role.getRoleId())) {
            throw new AppException(
                    "No se pueden modificar roles del sistema",
                    HttpStatus.FORBIDDEN
            );
        }

        // Nombre único (excluyendo el propio rol).
        if (roleRepository.existsByNameAndRoleIdNot(request.getName(), roleId)) {
            throw new AppException(
                    "Ya existe un rol con ese nombre",
                    HttpStatus.CONFLICT
            );
        }

        role.update(request.getName(), request.getDescription());
        roleRepository.save(role);

        return RoleResponseDto.builder()
                .roleId(role.getRoleId())
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }
}
