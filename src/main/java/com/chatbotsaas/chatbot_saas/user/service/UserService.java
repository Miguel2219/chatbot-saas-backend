package com.chatbotsaas.chatbot_saas.user.service;

import com.chatbotsaas.chatbot_saas.auth.service.AuthService;
import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.role.entity.Role;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import com.chatbotsaas.chatbot_saas.user.dto.response.UserResponse;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.repository.PersonRepository;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PersonRepository personRepository;
    private final AuthService authService;

    public UserService(UserRepository userRepository, TenantRepository tenantRepository, PersonRepository personRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.personRepository = personRepository;
        this.authService = authService;
    }

    @Transactional
    public Page<UserResponse> getUsers(Pageable pageable) {
        User userAuthenticated = authService.getUserAuthenticated();

        boolean isAdmin = userAuthenticated.getRoles().stream()
                .anyMatch(role -> role.getRoleId().equals(RoleConstants.ADMIN_ROLE));

        Page<User> users = isAdmin
                ? userRepository.findAll(pageable)
                : userRepository.findByTenant_Id(userAuthenticated.getTenant().getId(), pageable);

        return users.map(user -> UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getPerson().getName())
                .lastname(user.getPerson().getLastname())
                .notificationChannel(user.getNotificationChannel())
                .tenantName(user.getTenant() != null ? user.getTenant().getName() : null)
                .mustChangePassword(user.getMustChangePassword())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .build());
    }
}
