package com.chatbotsaas.chatbot_saas.tenant.service;

import com.chatbotsaas.chatbot_saas.notification.service.EmailService;
import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.role.entity.Role;
import com.chatbotsaas.chatbot_saas.role.repository.RoleRepository;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.tenant.dto.request.CreateTenantRequestDto;
import com.chatbotsaas.chatbot_saas.tenant.dto.response.TenantResponseDto;
import com.chatbotsaas.chatbot_saas.tenant.dto.response.TenantResponseSelectDto;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import com.chatbotsaas.chatbot_saas.user.entity.Person;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.repository.PersonRepository;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TenantService {
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final PersonRepository personRepository;
    private final EmailService emailService;

    public TenantService(UserRepository userRepository, TenantRepository tenantRepository, RoleRepository roleRepository, PersonRepository personRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.personRepository = personRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void createTenant(CreateTenantRequestDto request) {

        if (userRepository.findByEmail(request.getEmailUser()).isPresent() || tenantRepository.findByEmail(request.getTenantEmail()).isPresent()) {
            throw new AppException("Email already exists", HttpStatus.CONFLICT);
        }

        Tenant tenant = tenantRepository.save(
                Tenant.create(
                        request.getTenantName(),
                        request.getTenantEmail(),
                        request.getImplementationType()
                )
        );

        List<Role> roles = roleRepository.findAllById(List.of(RoleConstants.USER_ROLE));
        if (roles.isEmpty()) {
            throw new AppException("At least one role is required", HttpStatus.BAD_REQUEST);
        }

        String tempPassword = generateTempPassword();

        User user = userRepository.save(
                User.create(
                        request.getEmailUser(),
                        tempPassword,
                        Boolean.TRUE,
                        roles,
                        tenant,
                        request.getNotificationChannel()
                        )
        );

        personRepository.save(
                Person.builder()
                        .name(request.getName())
                        .lastname(request.getLastname())
                        .phone(request.getPhone())
                        .numberDocument(request.getNumberDocument())
                        .available(Boolean.TRUE)
                        .user(user)
                        .build()
        );

        emailService.sendWelcomeEmail(request.getEmailUser(), tempPassword, tenant.getName());
    }

    @Transactional(readOnly = true)
    public Page<TenantResponseDto> getTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable)
                .map(tenant -> TenantResponseDto.builder()
                        .tenantId(tenant.getId())
                        .name(tenant.getName())
                        .email(tenant.getEmail())
                        .isActive(tenant.getIsActive())
                        .implementationType(tenant.getImplementationType().name())
                        .createdAt(tenant.getCreatedAt())
                        .build()
                );
    }

    @Transactional
    public List<TenantResponseSelectDto> getTenantsSelect() {
        return tenantRepository.findAll()
                .stream().map(
                        tenant -> TenantResponseSelectDto.builder()
                                .tenantId(tenant.getId())
                                .name(tenant.getName())
                                .build()
                ).toList();
    }


    private String generateTempPassword() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

}
