package com.chatbotsaas.chatbot_saas.auth.service;

import com.chatbotsaas.chatbot_saas.auth.dto.LoginRequest;
import com.chatbotsaas.chatbot_saas.auth.dto.LoginResponse;
import com.chatbotsaas.chatbot_saas.auth.dto.RegisterRequest;
import com.chatbotsaas.chatbot_saas.auth.security.JwtTokenProvider;
import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final TenantRepository tenantRepository;

    public void register(RegisterRequest request) {
        Tenant tenant = tenantRepository.findById(request.getTenantId()).orElseThrow();
        User userCreated = new User();
        userCreated.setEmail(request.getEmail());
        userCreated.setPassword(passwordEncoder.encode(request.getPassword()));
        userCreated.setRole(RoleConstants.USER);
        userCreated.setTenant(tenant);
        userRepository.save(userCreated);
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        String token = jwtTokenProvider.generateToken(request.getEmail());
        return new LoginResponse(token);
    }
}
