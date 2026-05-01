package com.chatbotsaas.chatbot_saas.user.service;

import com.chatbotsaas.chatbot_saas.auth.service.AuthService;
import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.bot.repository.BotRepository;
import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.role.entity.Role;
import com.chatbotsaas.chatbot_saas.role.repository.RoleRepository;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.enums.ImplementationType;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import com.chatbotsaas.chatbot_saas.user.dto.request.CreateUserRequestDto;
import com.chatbotsaas.chatbot_saas.user.dto.request.UpdateUserRequestDto;
import com.chatbotsaas.chatbot_saas.user.dto.response.UserResponse;
import com.chatbotsaas.chatbot_saas.user.entity.Person;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.repository.PersonRepository;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import com.chatbotsaas.chatbot_saas.user.service.UserAccountFactory.UserAccountInput;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PersonRepository personRepository;
    private final BotRepository botRepository;
    private final RoleRepository roleRepository;
    private final AuthService authService;
    private final UserAccountFactory userAccountFactory;

    public UserService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            PersonRepository personRepository,
            BotRepository botRepository,
            RoleRepository roleRepository,
            AuthService authService,
            UserAccountFactory userAccountFactory
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.personRepository = personRepository;
        this.botRepository = botRepository;
        this.roleRepository = roleRepository;
        this.authService = authService;
        this.userAccountFactory = userAccountFactory;
    }

    @Transactional
    public void createUser(CreateUserRequestDto dto) {
        User caller = authService.getUserAuthenticated();
        if (caller == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        boolean callerIsAdmin = caller.isAdmin();
        boolean callerIsTenantOwner = caller.isTenantOwner();
        if (!callerIsAdmin && !callerIsTenantOwner) {
            throw new AppException("No autorizado para crear usuarios", HttpStatus.FORBIDDEN);
        }

        // 1. Resolver tenant destino.
        UUID targetTenantId;
        if (callerIsAdmin) {
            targetTenantId = dto.getTenantId(); // puede ser null (admin sin tenant)
        } else {
            if (caller.getTenant() == null) {
                throw new AppException("TENANT_OWNER sin tenant asignado", HttpStatus.FORBIDDEN);
            }
            targetTenantId = caller.getTenant().getId();
        }

        // 2. Resolver roles (y bloquear privilege escalation para TENANT_OWNER).
        List<UUID> roleIds;
        if (callerIsAdmin) {
            roleIds = (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty())
                    ? dto.getRoleIds()
                    : List.of(RoleConstants.USER_ROLE);
        } else {
            if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
                boolean triedEscalation = dto.getRoleIds().stream().anyMatch(r ->
                        r.equals(RoleConstants.TENANT_OWNER_ROLE)
                     || r.equals(RoleConstants.ADMIN_ROLE));
                if (triedEscalation) {
                    throw new AppException(
                            "No puedes crear usuarios con rol TENANT_OWNER o ADMIN",
                            HttpStatus.FORBIDDEN);
                }
            }
            roleIds = List.of(RoleConstants.USER_ROLE);
        }

        // 3. Cargar el tenant (si se resolvió uno).
        Tenant tenant = null;
        if (targetTenantId != null) {
            tenant = tenantRepository.findById(targetTenantId)
                    .orElseThrow(() -> new AppException("Tenant no encontrado", HttpStatus.NOT_FOUND));
        }

        // 4. Crear User + Person + welcome email AFTER_COMMIT.
        User created = userAccountFactory.createUserWithPerson(new UserAccountInput(
                dto.getEmail(),
                dto.getName(),
                dto.getLastname(),
                dto.getPhone(),
                dto.getNumberDocument(),
                dto.getNotificationChannel(),
                tenant,
                roleIds
        ));

        // 5. Lead-assignees condicional.
        if (tenant != null
                && dto.getLeadAssigneeBotIds() != null
                && !dto.getLeadAssigneeBotIds().isEmpty()) {
            ImplementationType impl = tenant.getImplementationType();
            if (impl == ImplementationType.WIDGET || impl == ImplementationType.BOTH) {
                List<Bot> bots = botRepository.findByTenant_IdAndBotIdIn(
                        targetTenantId, dto.getLeadAssigneeBotIds());
                for (Bot bot : bots) {
                    // Evitar duplicados en caso de re-envío.
                    boolean already = bot.getLeadAssignees().stream()
                            .anyMatch(u -> u.getUserId().equals(created.getUserId()));
                    if (!already) {
                        bot.getLeadAssignees().add(created);
                    }
                }
                botRepository.saveAll(bots);
            }
            // impl == WHATSAPP → ignorar silenciosamente. En el plan WhatsApp-only
            // los bots no tienen lead_assignees: el handoff a humano se gestiona
            // dentro del flow del webhook Meta enviándole al cliente el mensaje
            // "🔴 ASESOR REQUERIDO" (ver WhatsappService.processIncomingMessage).
        }
    }

    @Transactional
    public void updateUser(UUID userId, UpdateUserRequestDto dto) {
        User caller = authService.getUserAuthenticated();
        if (caller == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        boolean callerIsAdmin = caller.isAdmin();

        // 1. Scope: TENANT_OWNER sólo edita users de su propio tenant.
        if (!callerIsAdmin) {
            if (caller.getTenant() == null
                    || target.getTenant() == null
                    || !caller.getTenant().getId().equals(target.getTenant().getId())) {
                throw new AppException("No autorizado", HttpStatus.FORBIDDEN);
            }
        }

        // 2. Person fields (name/lastname/phone/number_document).
        Person person = target.getPerson();
        if (person != null) {
            if (dto.getName() != null)           person.setName(dto.getName());
            if (dto.getLastname() != null)       person.setLastname(dto.getLastname());
            if (dto.getPhone() != null)          person.setPhone(dto.getPhone());
            if (dto.getNumberDocument() != null) person.setNumberDocument(dto.getNumberDocument());
            personRepository.save(person);
        }

        // 3. notification_channel (viene del DTO, setea directo; null significa
        //    "no tocar" porque todos los campos son opcionales).
        if (dto.getNotificationChannel() != null) {
            target.setNotificationChannel(dto.getNotificationChannel());
        }

        // 4. Roles (reemplazo total, con validaciones).
        if (dto.getRoleIds() != null) {
            if (dto.getRoleIds().isEmpty()) {
                throw new AppException("Debe asignar al menos un rol", HttpStatus.BAD_REQUEST);
            }
            // Privilege escalation: TENANT_OWNER no puede asignar TENANT_OWNER ni ADMIN.
            if (!callerIsAdmin) {
                boolean tried = dto.getRoleIds().stream().anyMatch(r ->
                        r.equals(RoleConstants.TENANT_OWNER_ROLE)
                     || r.equals(RoleConstants.ADMIN_ROLE));
                if (tried) {
                    throw new AppException(
                            "No puedes asignar roles TENANT_OWNER o ADMIN",
                            HttpStatus.FORBIDDEN);
                }
            }

            // Si el target era el único TENANT_OWNER y los nuevos roles no lo incluyen
            // → rechazar (dejaría al tenant sin administrador, análogo al caso del delete).
            boolean wasOnlyOwner = target.isTenantOwner() && target.getTenant() != null
                    && userRepository.countByTenantIdAndRoleId(
                            target.getTenant().getId(), RoleConstants.TENANT_OWNER_ROLE) <= 1;
            boolean keepsOwner = dto.getRoleIds().contains(RoleConstants.TENANT_OWNER_ROLE);
            if (wasOnlyOwner && !keepsOwner) {
                throw new AppException(
                        "No puedes quitar el rol TENANT_OWNER al único administrador del tenant. "
                      + "Cambia el administrador desde el módulo de tenants primero.",
                        HttpStatus.BAD_REQUEST);
            }

            List<Role> newRoles = roleRepository.findAllById(dto.getRoleIds());
            if (newRoles.size() != dto.getRoleIds().size()) {
                throw new AppException("Alguno de los roles indicados no existe",
                        HttpStatus.BAD_REQUEST);
            }
            target.getRoles().clear();
            target.getRoles().addAll(newRoles);
        }

        userRepository.save(target);

        // 5. Lead-assignees: reemplazo total, sólo si tenant es WIDGET/BOTH.
        //    Null significa "no tocar"; lista vacía significa "quitar de todos".
        if (dto.getLeadAssigneeBotIds() != null && target.getTenant() != null) {
            ImplementationType impl = target.getTenant().getImplementationType();
            if (impl == ImplementationType.WIDGET || impl == ImplementationType.BOTH) {
                // 5a. Limpiar las asignaciones anteriores del user (mismo patrón que
                //     deleteUser líneas 204-210).
                List<Bot> previouslyAssigned = botRepository.findByLeadAssigneesContaining(target);
                for (Bot bot : previouslyAssigned) {
                    bot.getLeadAssignees().removeIf(u -> u.getUserId().equals(target.getUserId()));
                }
                if (!previouslyAssigned.isEmpty()) {
                    botRepository.saveAll(previouslyAssigned);
                }

                // 5b. Agregar a los nuevos. Todos los ids deben pertenecer al mismo tenant
                //     del user (evita asignación cruzada entre tenants).
                if (!dto.getLeadAssigneeBotIds().isEmpty()) {
                    List<Bot> newBots = botRepository.findByTenant_IdAndBotIdIn(
                            target.getTenant().getId(), dto.getLeadAssigneeBotIds());
                    if (newBots.size() != dto.getLeadAssigneeBotIds().size()) {
                        throw new AppException(
                                "Alguno de los bots indicados no pertenece al tenant del usuario",
                                HttpStatus.BAD_REQUEST);
                    }
                    for (Bot bot : newBots) {
                        boolean already = bot.getLeadAssignees().stream()
                                .anyMatch(u -> u.getUserId().equals(target.getUserId()));
                        if (!already) {
                            bot.getLeadAssignees().add(target);
                        }
                    }
                    botRepository.saveAll(newBots);
                }
            }
            // impl == WHATSAPP → ignorar silenciosamente (consistente con createUser).
        }
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User caller = authService.getUserAuthenticated();
        if (caller == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        // 1. Scope: TENANT_OWNER sólo borra users de su propio tenant.
        if (!caller.isAdmin()) {
            if (caller.getTenant() == null
                    || target.getTenant() == null
                    || !caller.getTenant().getId().equals(target.getTenant().getId())) {
                throw new AppException("No autorizado", HttpStatus.FORBIDDEN);
            }
        }

        // 2. No self-delete.
        if (caller.getUserId().equals(userId)) {
            throw new AppException("No puedes eliminar tu propia cuenta", HttpStatus.BAD_REQUEST);
        }

        // 3. Si el target es el único TENANT_OWNER del tenant → rechazar.
        if (target.isTenantOwner() && target.getTenant() != null) {
            long ownerCount = userRepository.countByTenantIdAndRoleId(
                    target.getTenant().getId(), RoleConstants.TENANT_OWNER_ROLE);
            if (ownerCount <= 1) {
                throw new AppException(
                        "No puedes eliminar al único administrador del tenant. "
                      + "Cambia el administrador desde el módulo de tenants antes de eliminar este usuario.",
                        HttpStatus.BAD_REQUEST);
            }
        }

        // 4. Limpiar lead-assignees para no dejar FKs colgando.
        List<Bot> assignedBots = botRepository.findByLeadAssigneesContaining(target);
        for (Bot bot : assignedBots) {
            bot.getLeadAssignees().removeIf(u -> u.getUserId().equals(userId));
        }
        if (!assignedBots.isEmpty()) {
            botRepository.saveAll(assignedBots);
        }

        // 5. Borrar Person (OneToOne con @MapsId) si existe, luego el User.
        if (target.getPerson() != null) {
            personRepository.delete(target.getPerson());
        }
        userRepository.delete(target);
    }

    // ========================================================================
    // Queries (existentes)
    // ========================================================================

    @Transactional
    public Page<UserResponse> getUsers(UUID tenantIdFilter, Pageable pageable) {
        User userAuthenticated = authService.getUserAuthenticated();

        boolean isAdmin = userAuthenticated.getRoles().stream()
                .anyMatch(role -> role.getRoleId().equals(RoleConstants.ADMIN_ROLE));

        UUID effectiveTenantId;
        if (isAdmin) {
            effectiveTenantId = tenantIdFilter;
        } else {
            effectiveTenantId = userAuthenticated.getTenant() != null ? userAuthenticated.getTenant().getId() : null;
        }

        Page<User> users = (effectiveTenantId == null)
                ? userRepository.findAll(pageable)
                : userRepository.findByTenant_Id(effectiveTenantId, pageable);

        return users.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersWithoutTenant() {
        return userRepository.findByTenantIsNull().stream()
                .map(this::toResponse)
                .toList();
    }


    private UserResponse toResponse(User user) {
        Person person = user.getPerson();
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(person != null ? person.getName() : null)
                .lastname(person != null ? person.getLastname() : null)
                .phone(person != null ? person.getPhone() : null)
                .numberDocument(person != null ? person.getNumberDocument() : null)
                .notificationChannel(user.getNotificationChannel())
                .tenantId(user.getTenant() != null ? user.getTenant().getId() : null)
                .tenantName(user.getTenant() != null ? user.getTenant().getName() : null)
                .mustChangePassword(user.getMustChangePassword())
                .roles(user.getRoles().stream().map(Role::getName).toList())
                .roleIds(user.getRoles().stream().map(Role::getRoleId).toList())
                .leadAssigneeBots(user.getBots() == null ? List.of() : user.getBots().stream()
                        .map(b -> UserResponse.UserBotRefDto.builder()
                                .botId(b.getBotId())
                                .name(b.getName())
                                .build())
                        .toList())
                .build();
    }
}
