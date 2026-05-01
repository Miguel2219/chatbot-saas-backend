package com.chatbotsaas.chatbot_saas.auth.service;

import com.chatbotsaas.chatbot_saas.auth.dto.*;
import com.chatbotsaas.chatbot_saas.auth.entity.RefreshToken;
import com.chatbotsaas.chatbot_saas.auth.security.JwtTokenProvider;
import com.chatbotsaas.chatbot_saas.module.dto.ModulePermissionDto;
import com.chatbotsaas.chatbot_saas.permission.service.PermissionService;
import com.chatbotsaas.chatbot_saas.role.entity.Role;
import com.chatbotsaas.chatbot_saas.role.repository.RoleRepository;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import com.chatbotsaas.chatbot_saas.user.dto.response.UserResponse;
import com.chatbotsaas.chatbot_saas.user.entity.Person;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.repository.PersonRepository;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final TenantRepository tenantRepository;
    private final PermissionService permissionService;
    private final RoleRepository roleRepository;
    private final PersonRepository personRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void register(RegisterRequest request) {
        List<Role> roles = roleRepository.findAllById(request.getRoleIds());
        User user = userRepository.save(
                User.create(
                        request.getEmail(),
                        passwordEncoder.encode(request.getPassword()),
                        Boolean.FALSE,
                        roles,
                        null,
                        null

                )
        );

        personRepository.save(
                Person.builder()
                        .user(user)
                        .name(request.getName())
                        .lastname(request.getLastname())
                        .phone(request.getPhone())
                        .numberDocument(request.getNumberDocument())
                        .available(Boolean.TRUE)
                        .build()
        );
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(
                        () -> new AppException("User not found", HttpStatus.NOT_FOUND)
                );

        return buildLoginResponse(user);
    }

    @Transactional
    public LoginResponse refreshAccessToken(RefreshTokenRequest request) {
        RefreshToken rotated = refreshTokenService.validateAndRotate(request.getRefreshToken());
        return buildLoginResponse(rotated.getUser());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }

    // TODO(MVP): "Cerrar sesión en todos los dispositivos". Dejado comentado
    // junto con el endpoint en AuthController. Reactivar ambos a la vez.
    //
    // /**
    //  * Endpoint autenticado POST /api/auth/logout-all. Revoca TODOS los
    //  * refresh tokens activos del usuario que hizo la request. Úsese cuando
    //  * el user sospecha compromiso de sesión (ej. perdió un dispositivo).
    //  * El user actual también queda fuera — el frontend limpia su storage
    //  * local tras el 204.
    //  *
    //  * Requiere sesión: si no hay un User en el SecurityContext tiramos
    //  * 401 UNAUTHORIZED, que el interceptor del cliente interpreta como
    //  * "tu sesión ya murió, vete a login".
    //  */
    // @Transactional
    // public void logoutAll() {
    //     User user = getUserAuthenticated();
    //     if (user == null) {
    //         throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
    //     }
    //     refreshTokenService.revokeAllForUserId(user.getUserId());
    // }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getUserAuthenticated();
        if (user == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }

    public User getUserAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            return userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        }
        return null;
    }

    private LoginResponse buildLoginResponse(User user) {
        UserResponse userResponse = getUserResponse(user);
        String accessToken = jwtTokenProvider.generateToken(userResponse.getEmail());
        String refreshToken = refreshTokenService.issueFor(user);
        boolean hasTenant = !user.isAdmin();
        List<ModulePermissionDto> modules = permissionService.getModulesForUser(
                user.getRoles(),
                hasTenant ? user.getTenant().getImplementationType() : null
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000L)
                .type("Bearer")
                .user(userResponse)
                .implementationType(hasTenant ? user.getTenant().getImplementationType().name() : null)
                .modules(modules)
                .build();
    }

    private UserResponse getUserResponse(User user) {
        List<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .toList();
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .mustChangePassword(user.getMustChangePassword())
                .notificationChannel(user.getNotificationChannel())
                .name(user.getPerson().getName())
                .lastname(user.getPerson().getLastname())
                .tenantId(user.getTenant() != null ? user.getTenant().getId() : null)
                .roles(roles)
                .build();
    }
}
