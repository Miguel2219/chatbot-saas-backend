package com.chatbotsaas.chatbot_saas.tenant.service;

import com.chatbotsaas.chatbot_saas.notification.service.EmailService;
import com.chatbotsaas.chatbot_saas.quota.constant.SubscriptionPlanConstants;
import com.chatbotsaas.chatbot_saas.quota.entity.SubscriptionPlan;
import com.chatbotsaas.chatbot_saas.quota.repository.SubscriptionPlanRepository;
import com.chatbotsaas.chatbot_saas.quota.service.QuotaCycleCalculator;
import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.role.entity.Role;
import com.chatbotsaas.chatbot_saas.role.repository.RoleRepository;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.tenant.dto.request.CreateTenantRequestDto;
import com.chatbotsaas.chatbot_saas.tenant.dto.request.UpdateTenantRequestDto;
import com.chatbotsaas.chatbot_saas.tenant.dto.response.TenantResponseDto;
import com.chatbotsaas.chatbot_saas.tenant.dto.response.TenantResponseSelectDto;
import com.chatbotsaas.chatbot_saas.tenant.dto.response.TenantUserSelectDto;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import com.chatbotsaas.chatbot_saas.user.entity.Person;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import com.chatbotsaas.chatbot_saas.user.service.UserAccountFactory;
import com.chatbotsaas.chatbot_saas.user.service.UserAccountFactory.UserAccountInput;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class TenantService {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final EmailService emailService;
    private final UserAccountFactory userAccountFactory;

    public TenantService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            RoleRepository roleRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            EmailService emailService,
            UserAccountFactory userAccountFactory
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.emailService = emailService;
        this.userAccountFactory = userAccountFactory;
    }

    // ========================================================================
    // Create
    // ========================================================================

    /**
     * Crea un tenant nuevo junto con su primer User, al que se le asigna automáticamente
     * el rol {@code TENANT_OWNER}. Se envía un welcome email AFTER_COMMIT con la contraseña
     * temporal. El payload ya no acepta {@code existing_user_id}, {@code role_ids} ni los
     * campos snake_case viejos del user — ver {@link CreateTenantRequestDto}.
     */
    @Transactional
    public void createTenant(CreateTenantRequestDto request) {
        if (tenantRepository.findByEmail(request.getTenantEmail()).isPresent()) {
            throw new AppException("Tenant email already exists", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(request.getOwnerEmail())) {
            throw new AppException("Owner email already exists", HttpStatus.CONFLICT);
        }

        // Plan BASIC + ventana de ciclo. El UUID del plan se fija vía migración V6 +
        // SubscriptionPlanConstants para no acoplar el código al string 'BASIC'. Si el
        // plan no está en DB es un error de deployment, no del caller → 500.
        SubscriptionPlan basic = subscriptionPlanRepository
                .findById(SubscriptionPlanConstants.BASIC_PLAN)
                .orElseThrow(() -> new AppException(
                        "Plan BASIC no configurado en DB", HttpStatus.INTERNAL_SERVER_ERROR));
        LocalDate today = LocalDate.now(BOGOTA);
        short cycleDay = (short) QuotaCycleCalculator.clampCycleDay(today.getDayOfMonth());
        QuotaCycleCalculator.CycleWindow window = QuotaCycleCalculator.windowFor(today, cycleDay);

        Tenant tenant;
        try {
            tenant = Tenant.create(
                    request.getTenantName(),
                    request.getTenantEmail(),
                    request.getImplementationType()
            );
            tenant.setSubscriptionPlan(basic);
            tenant.setBillingCycleDay(cycleDay);
            tenant.setCurrentCycleStart(window.start());
            tenant.setCurrentCycleEnd(window.end());
            tenant = tenantRepository.saveAndFlush(tenant);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException("Tenant email already exists", HttpStatus.CONFLICT);
        }

        userAccountFactory.createUserWithPerson(new UserAccountInput(
                request.getOwnerEmail(),
                request.getOwnerName(),
                request.getOwnerLastname(),
                request.getOwnerPhone(),
                request.getOwnerNumberDocument(),
                request.getOwnerNotificationChannel(),
                tenant,
                List.of(RoleConstants.TENANT_OWNER_ROLE)
        ));
    }

    // ========================================================================
    // Update (incluye swap de owner)
    // ========================================================================

    /**
     * Patch-style update. Sólo toca los campos que vienen no-nulos en el DTO. Si viene
     * {@code owner_user_id}, dispara el swap de roles (TENANT_OWNER ↔ USER) entre el owner
     * saliente y el entrante, preservando cualquier rol custom que tuvieran. Idempotente
     * si el user ya es el owner actual.
     */
    @Transactional
    public void updateTenant(UUID tenantId, UpdateTenantRequestDto dto) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new AppException("Tenant no encontrado", HttpStatus.NOT_FOUND));

        if (dto.getTenantName() != null) {
            tenant.setName(dto.getTenantName());
        }
        if (dto.getTenantEmail() != null) {
            tenant.setEmail(dto.getTenantEmail());
        }
        if (dto.getImplementationType() != null) {
            tenant.setImplementationType(dto.getImplementationType());
        }

        if (dto.getOwnerUserId() != null) {
            swapTenantOwner(tenantId, dto.getOwnerUserId());
        }

        // El frontend envía owner_notification_channel sólo cuando el implementation_type
        // nuevo (WIDGET o BOTH) lo exige y el owner aún no lo tiene definido. Lo aplicamos
        // al owner resultante: si hubo swap, el entrante; si no, el TENANT_OWNER actual.
        // Si nadie cumple el rol (caso patológico de tenant sin owner) se ignora silenciosamente.
        if (dto.getOwnerNotificationChannel() != null) {
            UUID targetOwnerId = dto.getOwnerUserId() != null
                    ? dto.getOwnerUserId()
                    : userRepository.findByTenantIdAndRoleId(tenantId, RoleConstants.TENANT_OWNER_ROLE)
                            .stream().findFirst().map(User::getUserId).orElse(null);
            if (targetOwnerId != null) {
                User owner = userRepository.findById(targetOwnerId)
                        .orElseThrow(() -> new AppException("Owner no encontrado", HttpStatus.NOT_FOUND));
                owner.setNotificationChannel(dto.getOwnerNotificationChannel());
                userRepository.save(owner);
            }
        }

        tenantRepository.save(tenant);
    }

    /**
     * Swap atómico del TENANT_OWNER dentro de un tenant.
     * <p>
     * Reglas:
     *  - Si {@code newOwnerUserId} ya es TENANT_OWNER del tenant → no-op idempotente (sin email).
     *  - El entrante pierde USER (si lo tiene), gana TENANT_OWNER. Otros roles custom se preservan.
     *  - El/los saliente(s) pierden TENANT_OWNER, ganan USER si no lo tenían. Otros roles custom
     *    se preservan.
     *  - Nunca {@code .clear()} — siempre {@code removeIf(id == ...)}.
     *  - Email al entrante AFTER_COMMIT.
     */
    private void swapTenantOwner(UUID tenantId, UUID newOwnerUserId) {
        User newOwner = userRepository.findById(newOwnerUserId)
                .orElseThrow(() -> new AppException("Usuario no encontrado", HttpStatus.NOT_FOUND));

        if (newOwner.getTenant() == null || !newOwner.getTenant().getId().equals(tenantId)) {
            throw new AppException("El usuario no pertenece a este tenant", HttpStatus.BAD_REQUEST);
        }

        boolean newOwnerAlreadyTenantOwner = newOwner.getRoles().stream()
                .anyMatch(r -> r.getRoleId().equals(RoleConstants.TENANT_OWNER_ROLE));
        if (newOwnerAlreadyTenantOwner) {
            return; // idempotent no-op
        }

        Role tenantOwnerRole = roleRepository.findById(RoleConstants.TENANT_OWNER_ROLE)
                .orElseThrow(() -> new AppException(
                        "TENANT_OWNER role no encontrado", HttpStatus.INTERNAL_SERVER_ERROR));
        Role userRole = roleRepository.findById(RoleConstants.USER_ROLE)
                .orElseThrow(() -> new AppException(
                        "USER role no encontrado", HttpStatus.INTERNAL_SERVER_ERROR));

        // --- Saliente(s): quitar TENANT_OWNER, agregar USER si no lo tienen.
        List<User> currentOwners = userRepository.findByTenantIdAndRoleId(
                tenantId, RoleConstants.TENANT_OWNER_ROLE);
        for (User prev : currentOwners) {
            if (prev.getUserId().equals(newOwnerUserId)) continue; // seguridad extra
            prev.getRoles().removeIf(r -> r.getRoleId().equals(RoleConstants.TENANT_OWNER_ROLE));
            boolean hasUser = prev.getRoles().stream()
                    .anyMatch(r -> r.getRoleId().equals(RoleConstants.USER_ROLE));
            if (!hasUser) {
                prev.getRoles().add(userRole);
            }
            userRepository.save(prev);
        }

        // --- Entrante: quitar USER (si lo tiene), agregar TENANT_OWNER.
        newOwner.getRoles().removeIf(r -> r.getRoleId().equals(RoleConstants.USER_ROLE));
        newOwner.getRoles().add(tenantOwnerRole);
        userRepository.save(newOwner);

        // --- Notificar al entrante AFTER_COMMIT.
        final String emailTo = newOwner.getEmail();
        final String tenantName = newOwner.getTenant().getName();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailService.sendTenantOwnerPromotionEmail(emailTo, tenantName);
            }
        });
    }

    // ========================================================================
    // Queries
    // ========================================================================

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

    @Transactional(readOnly = true)
    public List<TenantResponseSelectDto> getTenantsSelect() {
        return tenantRepository.findAll().stream()
                .map(tenant -> TenantResponseSelectDto.builder()
                        .tenantId(tenant.getId())
                        .name(tenant.getName())
                        .build())
                .toList();
    }

    /**
     * Lista los users del tenant — input para el selector del modal "Editar tenant".
     * Marca al TENANT_OWNER actual con {@code is_current_owner=true} para que el frontend
     * pueda resaltarlo y pre-seleccionarlo.
     */
    @Transactional(readOnly = true)
    public List<TenantUserSelectDto> getTenantUsers(UUID tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new AppException("Tenant no encontrado", HttpStatus.NOT_FOUND);
        }
        return userRepository.getUsersByTenantId(tenantId).stream()
                .map(user -> {
                    Person person = user.getPerson();
                    String fullName = person != null
                            ? trimToNull(person.getName()) + " " + trimToNull(person.getLastname())
                            : user.getEmail();
                    return TenantUserSelectDto.builder()
                            .userId(user.getUserId())
                            .fullName(fullName.trim())
                            .email(user.getEmail())
                            .isCurrentOwner(user.isTenantOwner())
                            .notificationChannel(user.getNotificationChannel())
                            .build();
                })
                .toList();
    }

    private static String trimToNull(String s) {
        return s == null ? "" : s;
    }
}
