package com.chatbotsaas.chatbot_saas.permission.security;

import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("permissionChecker")
@RequiredArgsConstructor
public class PermissionChecker {

    private final UserRepository userRepository;

    public boolean hasPermission(String moduleRoute, String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        String email = auth.getName();

        if (userRepository.isAdminByEmail(email, RoleConstants.ADMIN_ROLE)) {
            return true;
        }

        return userRepository.hasPermissionForModule(email, moduleRoute, action);
    }
}
