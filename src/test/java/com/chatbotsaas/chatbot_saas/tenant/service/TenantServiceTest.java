package com.chatbotsaas.chatbot_saas.tenant.service;

import com.chatbotsaas.chatbot_saas.notification.service.EmailService;
import com.chatbotsaas.chatbot_saas.quota.constant.SubscriptionPlanConstants;
import com.chatbotsaas.chatbot_saas.quota.entity.SubscriptionPlan;
import com.chatbotsaas.chatbot_saas.quota.repository.SubscriptionPlanRepository;
import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.role.entity.Role;
import com.chatbotsaas.chatbot_saas.role.repository.RoleRepository;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.tenant.dto.request.CreateTenantRequestDto;
import com.chatbotsaas.chatbot_saas.tenant.dto.request.UpdateTenantRequestDto;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.enums.ImplementationType;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import com.chatbotsaas.chatbot_saas.testutil.Entities;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import com.chatbotsaas.chatbot_saas.user.service.UserAccountFactory;
import com.chatbotsaas.chatbot_saas.user.service.UserAccountFactory.UserAccountInput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests de {@link TenantService}. Cubre:
 *  - {@code createTenant} fuerza rol TENANT_OWNER.
 *  - {@code updateTenant} patch-style (sólo toca campos no-null).
 *  - Swap de owner: casos normales + idempotencia + error de cross-tenant.
 *  - Preservación de roles custom en saliente y entrante.
 */
@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    private static final UUID CUSTOM_ROLE_GERENTE = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CUSTOM_ROLE_AUDITOR = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock UserRepository userRepository;
    @Mock TenantRepository tenantRepository;
    @Mock RoleRepository roleRepository;
    @Mock SubscriptionPlanRepository subscriptionPlanRepository;
    @Mock EmailService emailService;
    @Mock UserAccountFactory userAccountFactory;

    TenantService service;

    Role userRole;
    Role tenantOwnerRole;
    Role customGerente;
    Role customAuditor;

    @BeforeEach
    void setUp() {
        service = new TenantService(
                userRepository,
                tenantRepository,
                roleRepository,
                subscriptionPlanRepository,
                emailService,
                userAccountFactory);
        userRole = Entities.role(RoleConstants.USER_ROLE, "USER");
        tenantOwnerRole = Entities.role(RoleConstants.TENANT_OWNER_ROLE, "TENANT_OWNER");
        customGerente = Entities.role(CUSTOM_ROLE_GERENTE, "GERENTE_COMERCIAL");
        customAuditor = Entities.role(CUSTOM_ROLE_AUDITOR, "AUDITOR");
        // Activar sync manager para que swapTenantOwner pueda registrar el afterCommit.
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.initSynchronization();
        }
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // ========================================================================
    // createTenant
    // ========================================================================

    @Test
    @DisplayName("createTenant: fuerza TENANT_OWNER_ROLE en el user creado")
    void createTenant_assignsTenantOwnerRole() {
        CreateTenantRequestDto dto = CreateTenantRequestDto.builder()
                .tenantName("Salon de Rosa")
                .tenantEmail("contacto@rosa.com")
                .implementationType(ImplementationType.WIDGET)
                .ownerEmail("pedro@rosa.com")
                .ownerName("Pedro")
                .ownerLastname("Perez")
                .build();

        when(tenantRepository.findByEmail("contacto@rosa.com")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("pedro@rosa.com")).thenReturn(false);
        when(subscriptionPlanRepository.findById(SubscriptionPlanConstants.BASIC_PLAN))
                .thenReturn(Optional.of(mock(SubscriptionPlan.class)));
        when(tenantRepository.saveAndFlush(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createTenant(dto);

        ArgumentCaptor<UserAccountInput> captor = ArgumentCaptor.forClass(UserAccountInput.class);
        verify(userAccountFactory).createUserWithPerson(captor.capture());
        UserAccountInput input = captor.getValue();
        assertEquals(1, input.roleIds().size());
        assertEquals(RoleConstants.TENANT_OWNER_ROLE, input.roleIds().get(0));
        assertEquals("pedro@rosa.com", input.email());
        assertEquals("Pedro", input.name());
    }

    @Test
    @DisplayName("createTenant: asigna plan BASIC + ventana de ciclo al tenant persistido")
    void createTenant_assignsBasicPlanAndCycleWindow() {
        CreateTenantRequestDto dto = CreateTenantRequestDto.builder()
                .tenantName("Salon de Rosa")
                .tenantEmail("contacto@rosa.com")
                .implementationType(ImplementationType.WIDGET)
                .ownerEmail("pedro@rosa.com")
                .ownerName("Pedro")
                .ownerLastname("Perez")
                .build();
        SubscriptionPlan basic = mock(SubscriptionPlan.class);

        when(tenantRepository.findByEmail("contacto@rosa.com")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("pedro@rosa.com")).thenReturn(false);
        when(subscriptionPlanRepository.findById(SubscriptionPlanConstants.BASIC_PLAN))
                .thenReturn(Optional.of(basic));
        when(tenantRepository.saveAndFlush(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createTenant(dto);

        ArgumentCaptor<Tenant> captor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).saveAndFlush(captor.capture());
        Tenant persisted = captor.getValue();
        assertSame(basic, persisted.getSubscriptionPlan());
        assertNotNull(persisted.getBillingCycleDay());
        assertTrue(persisted.getBillingCycleDay() >= 1 && persisted.getBillingCycleDay() <= 28,
                "billingCycleDay debe estar clampeado a 1..28");
        assertNotNull(persisted.getCurrentCycleStart());
        assertNotNull(persisted.getCurrentCycleEnd());
        // end = start + 1 mes - 1 día.
        assertEquals(persisted.getCurrentCycleStart().plusMonths(1).minusDays(1),
                persisted.getCurrentCycleEnd());
        // El día-del-mes del start debe coincidir con billingCycleDay (modulo clamp al fin del mes corto).
        LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
        int expectedDay = Math.min((int) persisted.getBillingCycleDay(),
                persisted.getCurrentCycleStart().lengthOfMonth());
        assertEquals(expectedDay, persisted.getCurrentCycleStart().getDayOfMonth());
        // Coherencia: today ∈ [start, end].
        assertFalse(today.isBefore(persisted.getCurrentCycleStart()));
        assertFalse(today.isAfter(persisted.getCurrentCycleEnd()));
    }

    @Test
    @DisplayName("createTenant: plan BASIC ausente en DB → 500")
    void createTenant_failsWhenBasicPlanMissing() {
        CreateTenantRequestDto dto = CreateTenantRequestDto.builder()
                .tenantName("X")
                .tenantEmail("x@x.com")
                .implementationType(ImplementationType.WIDGET)
                .ownerEmail("pedro@x.com")
                .ownerName("Pedro")
                .ownerLastname("Perez")
                .build();

        when(tenantRepository.findByEmail("x@x.com")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("pedro@x.com")).thenReturn(false);
        when(subscriptionPlanRepository.findById(SubscriptionPlanConstants.BASIC_PLAN))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.createTenant(dto));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        verify(tenantRepository, never()).saveAndFlush(any(Tenant.class));
        verify(userAccountFactory, never()).createUserWithPerson(any());
    }

    @Test
    @DisplayName("createTenant: email de tenant duplicado → 409")
    void createTenant_duplicateTenantEmail_conflict() {
        CreateTenantRequestDto dto = CreateTenantRequestDto.builder()
                .tenantName("X")
                .tenantEmail("dup@x.com")
                .implementationType(ImplementationType.WIDGET)
                .ownerEmail("pedro@x.com")
                .ownerName("Pedro")
                .ownerLastname("Perez")
                .build();

        when(tenantRepository.findByEmail("dup@x.com"))
                .thenReturn(Optional.of(Entities.tenant(UUID.randomUUID(), "existing", ImplementationType.WIDGET)));

        AppException ex = assertThrows(AppException.class, () -> service.createTenant(dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // ========================================================================
    // updateTenant (patch)
    // ========================================================================

    @Test
    @DisplayName("updateTenant: sin owner_user_id → sólo patch de campos simples; no toca roles")
    void updateTenant_patchOnly() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Entities.tenant(tenantId, "original", ImplementationType.WIDGET);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        UpdateTenantRequestDto dto = UpdateTenantRequestDto.builder()
                .tenantName("nuevo nombre")
                .implementationType(ImplementationType.BOTH)
                .build();

        service.updateTenant(tenantId, dto);

        assertEquals("nuevo nombre", tenant.getName());
        assertEquals(ImplementationType.BOTH, tenant.getImplementationType());
        verify(userRepository, never()).findByTenantIdAndRoleId(any(), any());
    }

    @Test
    @DisplayName("updateTenant: owner_user_id = owner actual → idempotente no-op (sin email, sin cambios)")
    void updateTenant_idempotentOwnerSwap() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Entities.tenant(tenantId, "t", ImplementationType.WIDGET);
        UUID currentOwnerId = UUID.randomUUID();
        User currentOwner = Entities.user(currentOwnerId, "pedro@t.com", tenant, tenantOwnerRole);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findById(currentOwnerId)).thenReturn(Optional.of(currentOwner));

        UpdateTenantRequestDto dto = UpdateTenantRequestDto.builder()
                .ownerUserId(currentOwnerId)
                .build();

        service.updateTenant(tenantId, dto);

        // No se consultaron los owners actuales (short-circuit antes).
        verify(userRepository, never()).findByTenantIdAndRoleId(any(), any());
        verify(emailService, never()).sendTenantOwnerPromotionEmail(any(), any());
        // Sigue siendo TENANT_OWNER.
        assertTrue(currentOwner.getRoles().stream()
                .anyMatch(r -> r.getRoleId().equals(RoleConstants.TENANT_OWNER_ROLE)));
    }

    @Test
    @DisplayName("updateTenant: swap normal — saliente gana USER, entrante gana TENANT_OWNER, email AFTER_COMMIT-no-op simulado")
    void updateTenant_swapOwner_preservesCustomRoles() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Entities.tenant(tenantId, "t", ImplementationType.WIDGET);

        UUID pedroId = UUID.randomUUID();
        User pedro = Entities.user(pedroId, "pedro@t.com", tenant, tenantOwnerRole, customGerente);

        UUID mariaId = UUID.randomUUID();
        User maria = Entities.user(mariaId, "maria@t.com", tenant, userRole, customAuditor);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findById(mariaId)).thenReturn(Optional.of(maria));
        when(userRepository.findByTenantIdAndRoleId(tenantId, RoleConstants.TENANT_OWNER_ROLE))
                .thenReturn(List.of(pedro));
        when(roleRepository.findById(RoleConstants.TENANT_OWNER_ROLE)).thenReturn(Optional.of(tenantOwnerRole));
        when(roleRepository.findById(RoleConstants.USER_ROLE)).thenReturn(Optional.of(userRole));

        UpdateTenantRequestDto dto = UpdateTenantRequestDto.builder()
                .ownerUserId(mariaId)
                .build();

        service.updateTenant(tenantId, dto);

        // Pedro: ya no es TENANT_OWNER, ahora tiene USER, preserva GERENTE_COMERCIAL.
        assertFalse(pedro.getRoles().stream().anyMatch(r -> r.getRoleId().equals(RoleConstants.TENANT_OWNER_ROLE)));
        assertTrue(pedro.getRoles().stream().anyMatch(r -> r.getRoleId().equals(RoleConstants.USER_ROLE)));
        assertTrue(pedro.getRoles().stream().anyMatch(r -> r.getRoleId().equals(CUSTOM_ROLE_GERENTE)));

        // María: ya no tiene USER, ahora es TENANT_OWNER, preserva AUDITOR.
        assertFalse(maria.getRoles().stream().anyMatch(r -> r.getRoleId().equals(RoleConstants.USER_ROLE)));
        assertTrue(maria.getRoles().stream().anyMatch(r -> r.getRoleId().equals(RoleConstants.TENANT_OWNER_ROLE)));
        assertTrue(maria.getRoles().stream().anyMatch(r -> r.getRoleId().equals(CUSTOM_ROLE_AUDITOR)));
    }

    @Test
    @DisplayName("updateTenant: nuevo owner que ya tenía USER pero no custom → gana TENANT_OWNER, pierde USER, sin roles extra")
    void updateTenant_swapOwner_newOwnerOnlyUser() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Entities.tenant(tenantId, "t", ImplementationType.WIDGET);

        UUID pedroId = UUID.randomUUID();
        User pedro = Entities.user(pedroId, "pedro@t.com", tenant, tenantOwnerRole);

        UUID mariaId = UUID.randomUUID();
        User maria = Entities.user(mariaId, "maria@t.com", tenant, userRole);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findById(mariaId)).thenReturn(Optional.of(maria));
        when(userRepository.findByTenantIdAndRoleId(tenantId, RoleConstants.TENANT_OWNER_ROLE))
                .thenReturn(List.of(pedro));
        when(roleRepository.findById(RoleConstants.TENANT_OWNER_ROLE)).thenReturn(Optional.of(tenantOwnerRole));
        when(roleRepository.findById(RoleConstants.USER_ROLE)).thenReturn(Optional.of(userRole));

        service.updateTenant(tenantId, UpdateTenantRequestDto.builder().ownerUserId(mariaId).build());

        assertEquals(1, maria.getRoles().size());
        assertEquals(RoleConstants.TENANT_OWNER_ROLE, maria.getRoles().get(0).getRoleId());
        assertEquals(1, pedro.getRoles().size());
        assertEquals(RoleConstants.USER_ROLE, pedro.getRoles().get(0).getRoleId());
    }

    @Test
    @DisplayName("updateTenant: owner_user_id de otro tenant → 400")
    void updateTenant_crossTenantOwner_badRequest() {
        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();
        Tenant tenant = Entities.tenant(tenantId, "t", ImplementationType.WIDGET);
        Tenant otherTenant = Entities.tenant(otherTenantId, "o", ImplementationType.WIDGET);
        UUID outsiderId = UUID.randomUUID();
        User outsider = Entities.user(outsiderId, "outsider@o.com", otherTenant, userRole);

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findById(outsiderId)).thenReturn(Optional.of(outsider));

        AppException ex = assertThrows(AppException.class, () ->
                service.updateTenant(tenantId, UpdateTenantRequestDto.builder().ownerUserId(outsiderId).build()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("updateTenant: owner_user_id inexistente → 404")
    void updateTenant_ownerNotFound() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Entities.tenant(tenantId, "t", ImplementationType.WIDGET);
        UUID ghostId = UUID.randomUUID();

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findById(ghostId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                service.updateTenant(tenantId, UpdateTenantRequestDto.builder().ownerUserId(ghostId).build()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("updateTenant: tenant inexistente → 404")
    void updateTenant_tenantNotFound() {
        UUID tenantId = UUID.randomUUID();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                service.updateTenant(tenantId, UpdateTenantRequestDto.builder().tenantName("x").build()));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
