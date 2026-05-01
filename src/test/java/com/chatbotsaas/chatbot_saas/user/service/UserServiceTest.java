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
import com.chatbotsaas.chatbot_saas.testutil.Entities;
import com.chatbotsaas.chatbot_saas.user.dto.request.CreateUserRequestDto;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.repository.PersonRepository;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import com.chatbotsaas.chatbot_saas.user.service.UserAccountFactory.UserAccountInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests de {@link UserService}. Cubre:
 *  - createUser: matriz de autorización ADMIN / TENANT_OWNER (privilege escalation) + lead-assignees
 *    condicional según {@code implementation_type} del tenant.
 *  - deleteUser: scope, self-delete, sole-owner, limpieza de bot_lead_assignees.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock TenantRepository tenantRepository;
    @Mock PersonRepository personRepository;
    @Mock BotRepository botRepository;
    @Mock RoleRepository roleRepository;
    @Mock AuthService authService;
    @Mock UserAccountFactory userAccountFactory;

    UserService service;

    Role adminRole;
    Role userRole;
    Role tenantOwnerRole;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, tenantRepository, personRepository,
                botRepository, roleRepository, authService, userAccountFactory);
        adminRole = Entities.role(RoleConstants.ADMIN_ROLE, "ADMIN");
        userRole = Entities.role(RoleConstants.USER_ROLE, "USER");
        tenantOwnerRole = Entities.role(RoleConstants.TENANT_OWNER_ROLE, "TENANT_OWNER");
    }

    // ========================================================================
    // createUser — autorización
    // ========================================================================

    @Test
    @DisplayName("createUser: ADMIN puede asignar role_ids y tenant_id arbitrarios")
    void createUser_admin_freeToAssignAnything() {
        UUID adminId = UUID.randomUUID();
        User admin = Entities.user(adminId, "admin@zolvion.com", null, adminRole);
        when(authService.getUserAuthenticated()).thenReturn(admin);

        UUID targetTenantId = UUID.randomUUID();
        Tenant targetTenant = Entities.tenant(targetTenantId, "Target", ImplementationType.WIDGET);
        when(tenantRepository.findById(targetTenantId)).thenReturn(Optional.of(targetTenant));

        CreateUserRequestDto dto = CreateUserRequestDto.builder()
                .email("new@t.com")
                .name("Nuevo")
                .lastname("User")
                .tenantId(targetTenantId)
                .roleIds(List.of(RoleConstants.TENANT_OWNER_ROLE))
                .build();

        service.createUser(dto);

        ArgumentCaptor<UserAccountInput> cap = ArgumentCaptor.forClass(UserAccountInput.class);
        verify(userAccountFactory).createUserWithPerson(cap.capture());
        assertEquals(List.of(RoleConstants.TENANT_OWNER_ROLE), cap.getValue().roleIds());
        assertEquals(targetTenantId, cap.getValue().tenant().getId());
    }

    @Test
    @DisplayName("createUser: TENANT_OWNER fuerza USER_ROLE y su propio tenant, ignora tenant_id del payload")
    void createUser_tenantOwner_forcesOwnTenantAndUserRole() {
        UUID ownerTenantId = UUID.randomUUID();
        Tenant ownerTenant = Entities.tenant(ownerTenantId, "Pedro's Tenant", ImplementationType.WIDGET);
        UUID ownerId = UUID.randomUUID();
        User owner = Entities.user(ownerId, "pedro@t.com", ownerTenant, tenantOwnerRole);
        when(authService.getUserAuthenticated()).thenReturn(owner);
        when(tenantRepository.findById(ownerTenantId)).thenReturn(Optional.of(ownerTenant));

        UUID otherTenantId = UUID.randomUUID(); // intento de asignar a otro tenant

        CreateUserRequestDto dto = CreateUserRequestDto.builder()
                .email("maria@t.com")
                .name("Maria")
                .lastname("Lopez")
                .tenantId(otherTenantId)
                .build();

        service.createUser(dto);

        ArgumentCaptor<UserAccountInput> cap = ArgumentCaptor.forClass(UserAccountInput.class);
        verify(userAccountFactory).createUserWithPerson(cap.capture());
        assertEquals(List.of(RoleConstants.USER_ROLE), cap.getValue().roleIds());
        assertEquals(ownerTenantId, cap.getValue().tenant().getId(), "debe usar el tenant del owner, no el del payload");
    }

    @Test
    @DisplayName("createUser: TENANT_OWNER con role_ids=[TENANT_OWNER_ROLE] → 403 privilege escalation")
    void createUser_tenantOwner_escalationBlocked() {
        UUID ownerTenantId = UUID.randomUUID();
        Tenant ownerTenant = Entities.tenant(ownerTenantId, "T", ImplementationType.WIDGET);
        User owner = Entities.user(UUID.randomUUID(), "pedro@t.com", ownerTenant, tenantOwnerRole);
        when(authService.getUserAuthenticated()).thenReturn(owner);

        CreateUserRequestDto dto = CreateUserRequestDto.builder()
                .email("x@t.com").name("X").lastname("X")
                .roleIds(List.of(RoleConstants.TENANT_OWNER_ROLE))
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.createUser(dto));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("createUser: TENANT_OWNER con role_ids=[ADMIN_ROLE] → 403 privilege escalation")
    void createUser_tenantOwner_adminEscalationBlocked() {
        UUID ownerTenantId = UUID.randomUUID();
        Tenant ownerTenant = Entities.tenant(ownerTenantId, "T", ImplementationType.WIDGET);
        User owner = Entities.user(UUID.randomUUID(), "pedro@t.com", ownerTenant, tenantOwnerRole);
        when(authService.getUserAuthenticated()).thenReturn(owner);

        CreateUserRequestDto dto = CreateUserRequestDto.builder()
                .email("x@t.com").name("X").lastname("X")
                .roleIds(List.of(RoleConstants.ADMIN_ROLE))
                .build();

        AppException ex = assertThrows(AppException.class, () -> service.createUser(dto));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("createUser: caller USER plano (sin TENANT_OWNER ni ADMIN) → 403")
    void createUser_plainUser_forbidden() {
        User plain = Entities.user(UUID.randomUUID(), "joe@t.com",
                Entities.tenant(UUID.randomUUID(), "t", ImplementationType.WIDGET), userRole);
        when(authService.getUserAuthenticated()).thenReturn(plain);

        CreateUserRequestDto dto = CreateUserRequestDto.builder()
                .email("x@t.com").name("X").lastname("X").build();

        AppException ex = assertThrows(AppException.class, () -> service.createUser(dto));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // ========================================================================
    // createUser — lead-assignees condicional
    // ========================================================================

    @Test
    @DisplayName("createUser: tenant BOTH + lead_assignee_bot_ids → user queda en bot.leadAssignees")
    void createUser_tenantBoth_addsLeadAssignees() {
        UUID ownerTenantId = UUID.randomUUID();
        Tenant ownerTenant = Entities.tenant(ownerTenantId, "T", ImplementationType.BOTH);
        User owner = Entities.user(UUID.randomUUID(), "pedro@t.com", ownerTenant, tenantOwnerRole);
        when(authService.getUserAuthenticated()).thenReturn(owner);
        when(tenantRepository.findById(ownerTenantId)).thenReturn(Optional.of(ownerTenant));

        UUID botId = UUID.randomUUID();
        Bot bot = Entities.bot(botId, ownerTenant);
        when(botRepository.findByTenant_IdAndBotIdIn(eq(ownerTenantId), any(Collection.class)))
                .thenReturn(List.of(bot));

        UUID createdUserId = UUID.randomUUID();
        User created = Entities.user(createdUserId, "maria@t.com", ownerTenant, userRole);
        when(userAccountFactory.createUserWithPerson(any())).thenReturn(created);

        CreateUserRequestDto dto = CreateUserRequestDto.builder()
                .email("maria@t.com").name("Maria").lastname("L")
                .leadAssigneeBotIds(List.of(botId))
                .build();

        service.createUser(dto);

        assertTrue(bot.getLeadAssignees().stream().anyMatch(u -> u.getUserId().equals(createdUserId)));
        verify(botRepository).saveAll(any());
    }

    @Test
    @DisplayName("createUser: tenant WHATSAPP + lead_assignee_bot_ids → se ignora silenciosamente")
    void createUser_tenantWhatsapp_ignoresLeadAssignees() {
        UUID ownerTenantId = UUID.randomUUID();
        Tenant ownerTenant = Entities.tenant(ownerTenantId, "T", ImplementationType.WHATSAPP);
        User owner = Entities.user(UUID.randomUUID(), "pedro@t.com", ownerTenant, tenantOwnerRole);
        when(authService.getUserAuthenticated()).thenReturn(owner);
        when(tenantRepository.findById(ownerTenantId)).thenReturn(Optional.of(ownerTenant));

        UUID createdUserId = UUID.randomUUID();
        User created = Entities.user(createdUserId, "maria@t.com", ownerTenant, userRole);
        when(userAccountFactory.createUserWithPerson(any())).thenReturn(created);

        CreateUserRequestDto dto = CreateUserRequestDto.builder()
                .email("maria@t.com").name("Maria").lastname("L")
                .leadAssigneeBotIds(List.of(UUID.randomUUID()))
                .build();

        service.createUser(dto);

        // Ni siquiera se consultó el repo de bots.
        verify(botRepository, never()).findByTenant_IdAndBotIdIn(any(), any());
        verify(botRepository, never()).saveAll(any());
    }

    // ========================================================================
    // deleteUser
    // ========================================================================

    @Test
    @DisplayName("deleteUser: caller TENANT_OWNER intenta borrar user de OTRO tenant → 403")
    void deleteUser_tenantOwner_crossTenant_forbidden() {
        UUID t1 = UUID.randomUUID();
        UUID t2 = UUID.randomUUID();
        Tenant tenant1 = Entities.tenant(t1, "A", ImplementationType.WIDGET);
        Tenant tenant2 = Entities.tenant(t2, "B", ImplementationType.WIDGET);
        User caller = Entities.user(UUID.randomUUID(), "pedro@a.com", tenant1, tenantOwnerRole);
        UUID targetId = UUID.randomUUID();
        User target = Entities.user(targetId, "joe@b.com", tenant2, userRole);

        when(authService.getUserAuthenticated()).thenReturn(caller);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));

        AppException ex = assertThrows(AppException.class, () -> service.deleteUser(targetId));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("deleteUser: self-delete → 400")
    void deleteUser_selfDelete_badRequest() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Entities.tenant(tenantId, "t", ImplementationType.WIDGET);
        UUID callerId = UUID.randomUUID();
        User caller = Entities.user(callerId, "pedro@t.com", tenant, tenantOwnerRole);

        when(authService.getUserAuthenticated()).thenReturn(caller);
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));

        AppException ex = assertThrows(AppException.class, () -> service.deleteUser(callerId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("deleteUser: target es único TENANT_OWNER del tenant → 400 con mensaje específico")
    void deleteUser_soleTenantOwner_badRequest() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Entities.tenant(tenantId, "t", ImplementationType.WIDGET);
        User admin = Entities.user(UUID.randomUUID(), "admin@zolvion.com", null, adminRole);
        UUID targetId = UUID.randomUUID();
        User target = Entities.user(targetId, "pedro@t.com", tenant, tenantOwnerRole);

        when(authService.getUserAuthenticated()).thenReturn(admin);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(userRepository.countByTenantIdAndRoleId(tenantId, RoleConstants.TENANT_OWNER_ROLE))
                .thenReturn(1L);

        AppException ex = assertThrows(AppException.class, () -> service.deleteUser(targetId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getMessage().contains("administrador"), "mensaje debe referenciar al administrador");
    }

    @Test
    @DisplayName("deleteUser: target común + entradas en bot_lead_assignees → se limpian antes del delete")
    void deleteUser_cleansLeadAssignees() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Entities.tenant(tenantId, "t", ImplementationType.WIDGET);
        User admin = Entities.user(UUID.randomUUID(), "admin@zolvion.com", null, adminRole);
        UUID targetId = UUID.randomUUID();
        User target = Entities.user(targetId, "joe@t.com", tenant, userRole);

        // Bot que tiene al target como assignee.
        Bot bot = Entities.bot(UUID.randomUUID(), tenant);
        bot.getLeadAssignees().add(target);

        when(authService.getUserAuthenticated()).thenReturn(admin);
        when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
        when(botRepository.findByLeadAssigneesContaining(target)).thenReturn(List.of(bot));

        service.deleteUser(targetId);

        assertTrue(bot.getLeadAssignees().stream().noneMatch(u -> u.getUserId().equals(targetId)));
        verify(botRepository).saveAll(any());
        verify(userRepository).delete(target);
    }
}
