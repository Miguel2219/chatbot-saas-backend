package com.chatbotsaas.chatbot_saas.user.service;

import com.chatbotsaas.chatbot_saas.notification.service.EmailService;
import com.chatbotsaas.chatbot_saas.role.entity.Role;
import com.chatbotsaas.chatbot_saas.role.repository.RoleRepository;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.user.entity.Person;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.enums.NotificationChannel;
import com.chatbotsaas.chatbot_saas.user.repository.PersonRepository;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

/**
 * Helper compartido entre {@code TenantService.createTenant} y {@code UserService.createUser}.
 * Centraliza la creación del par User+Person con contraseña temporal, el registro del email de
 * bienvenida {@code AFTER_COMMIT}, y la validación de email único. Evita drift entre los dos
 * call sites (antes el patrón estaba duplicado en TenantService y no existía todavía en
 * UserService).
 */
@Component
public class UserAccountFactory {

    private final UserRepository userRepository;
    private final PersonRepository personRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserAccountFactory(
            UserRepository userRepository,
            PersonRepository personRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.personRepository = personRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Crea User+Person con contraseña temporal + welcome email AFTER_COMMIT. El caller se
     * encarga de la validación de autorización, de escoger los {@code roleIds} y de asociar
     * extras como lead-assignees. Devuelve el {@code User} ya persistido.
     */
    public User createUserWithPerson(UserAccountInput input) {
        if (userRepository.existsByEmail(input.email())) {
            throw new AppException("Ya existe un usuario con ese email", HttpStatus.CONFLICT);
        }

        List<Role> roles = roleRepository.findAllById(input.roleIds());
        if (roles.size() != input.roleIds().size()) {
            throw new AppException("Uno o más roles no existen", HttpStatus.BAD_REQUEST);
        }

        String tempPassword = generateTempPassword();
        String hashedPassword = passwordEncoder.encode(tempPassword);

        User user;
        try {
            user = userRepository.saveAndFlush(
                    User.create(
                            input.email(),
                            hashedPassword,
                            Boolean.TRUE,
                            roles,
                            input.tenant(),
                            input.notificationChannel()
                    )
            );

            personRepository.save(
                    Person.builder()
                            .name(input.name())
                            .lastname(input.lastname())
                            .phone(input.phone())
                            .numberDocument(input.numberDocument())
                            .available(Boolean.TRUE)
                            .user(user)
                            .build()
            );
        } catch (DataIntegrityViolationException ex) {
            // La unique del email o del number_document disparan esto.
            throw new AppException("Email o documento duplicado", HttpStatus.CONFLICT);
        }

        // El email va AFTER_COMMIT: si la transacción se revierte no se manda un welcome mentiroso.
        final String emailTo = input.email();
        final String tenantName = input.tenant() != null ? input.tenant().getName() : "";
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailService.sendWelcomeEmail(emailTo, tempPassword, tenantName);
            }
        });

        return user;
    }

    private String generateTempPassword() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Entrada agnóstica del caller — los services construyen este record a partir de su propio
     * DTO para no acoplar el factory a ninguna forma específica de payload.
     */
    public record UserAccountInput(
            String email,
            String name,
            String lastname,
            String phone,
            String numberDocument,
            NotificationChannel notificationChannel,
            Tenant tenant,
            List<UUID> roleIds
    ) {}
}
