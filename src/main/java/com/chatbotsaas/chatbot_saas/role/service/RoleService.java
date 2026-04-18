package com.chatbotsaas.chatbot_saas.role.service;

import com.chatbotsaas.chatbot_saas.role.dto.response.RoleResponseDto;
import com.chatbotsaas.chatbot_saas.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

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
}
