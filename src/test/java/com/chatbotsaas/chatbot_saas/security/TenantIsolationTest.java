package com.chatbotsaas.chatbot_saas.security;

import com.chatbotsaas.chatbot_saas.auth.service.AuthService;
import com.chatbotsaas.chatbot_saas.bot.dto.request.RegisterBotDto;
import com.chatbotsaas.chatbot_saas.bot.dto.request.UpdateBotDto;
import com.chatbotsaas.chatbot_saas.bot.dto.request.UpdateSystemPromptDto;
import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.bot.repository.BotRepository;
import com.chatbotsaas.chatbot_saas.bot.service.BotService;
import com.chatbotsaas.chatbot_saas.conversation.entity.Conversation;
import com.chatbotsaas.chatbot_saas.conversation.repository.ConversationRepository;
import com.chatbotsaas.chatbot_saas.conversation.service.ConversationService;
import com.chatbotsaas.chatbot_saas.document.entity.Document;
import com.chatbotsaas.chatbot_saas.document.repository.DocumentRepository;
import com.chatbotsaas.chatbot_saas.document.service.DocumentService;
import com.chatbotsaas.chatbot_saas.integration.PythonRagClient;
import com.chatbotsaas.chatbot_saas.lead.entity.Lead;
import com.chatbotsaas.chatbot_saas.lead.enums.LeadStatus;
import com.chatbotsaas.chatbot_saas.lead.repository.LeadRepository;
import com.chatbotsaas.chatbot_saas.lead.service.LeadService;
import com.chatbotsaas.chatbot_saas.notification.service.NotificationService;
import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.role.entity.Role;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.shared.security.TenantAccessValidator;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.enums.ImplementationType;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import com.chatbotsaas.chatbot_saas.testutil.Entities;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import com.chatbotsaas.chatbot_saas.whatsapp.dto.request.CreateWhatsappConfigRequest;
import com.chatbotsaas.chatbot_saas.whatsapp.entity.WhatsappConfig;
import com.chatbotsaas.chatbot_saas.whatsapp.repository.WhatsappConfigRepository;
import com.chatbotsaas.chatbot_saas.whatsapp.service.WhatsappConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Suite de aislamiento multi-tenant.
 *
 * <p>Prepara el código para una eventual migración a Row Level Security (RLS).
 * Cada test prueba un escenario donde un usuario autenticado del tenant A
 * intenta operar sobre un recurso del tenant B — la expectativa es que la
 * capa de aplicación lo rechace con {@link HttpStatus#FORBIDDEN}. Tests
 * adicionales verifican el bypass ADMIN legítimo y que el {@code tenant_id}
 * forzado en el body sea ignorado cuando proviene de un usuario no-admin.
 *
 * <p>Todos los services usan el bean real {@link TenantAccessValidator}
 * (construido con un {@link AuthService} mockeado) — eso ejercita tanto la
 * lógica del validator como su integración en cada service. Estrategia:
 * subir la mayor cantidad de cobertura efectiva con el menor volumen de
 * mocks posible.
 *
 * <p>Cobertura:
 * <ul>
 *   <li>bots — list / update (name + system prompt) / delete / getById /
 *       getSystemPrompt / create (tenant_id ignorado para non-admin)</li>
 *   <li>conversations — list / getBySession</li>
 *   <li>leads — list / updateStatus</li>
 *   <li>documents — list / upload / delete</li>
 *   <li>whatsapp-config — create / getByBot / update / delete</li>
 *   <li>admin bypass explícito para cada familia</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TenantIsolationTest {

    // Repos compartidos
    @Mock BotRepository botRepository;
    @Mock TenantRepository tenantRepository;
    @Mock UserRepository userRepository;
    @Mock AuthService authService;

    // Conversation / Lead / Document
    @Mock ConversationRepository conversationRepository;
    @Mock LeadRepository leadRepository;
    @Mock NotificationService notificationService;
    @Mock DocumentRepository documentRepository;
    @Mock PythonRagClient pythonRagClient;

    // WhatsApp config
    @Mock WhatsappConfigRepository whatsappConfigRepository;

    TenantAccessValidator tenantAccessValidator;
    BotService botService;
    ConversationService conversationService;
    LeadService leadService;
    DocumentService documentService;
    WhatsappConfigService whatsappConfigService;

    // Actores
    Tenant tenantA;
    Tenant tenantB;
    Role adminRole;
    Role userRole;
    User userA;       // non-admin, tenant A
    User adminUser;   // admin, sin tenant fijo

    UUID tenantAId;
    UUID tenantBId;
    UUID botAId;
    UUID botBId;
    Bot botTenantA;
    Bot botTenantB;

    @BeforeEach
    void setUp() {
        // Usamos el validator REAL (no mockeado) para ejercitar la integración
        // completa — cualquier change en el mensaje o en el flujo del
        // validator se detecta acá, no en un test-double que miente.
        tenantAccessValidator = new TenantAccessValidator(authService);

        botService = new BotService(botRepository, tenantRepository, userRepository, authService, tenantAccessValidator);
        conversationService = new ConversationService(botRepository, conversationRepository, authService, tenantAccessValidator);
        leadService = new LeadService(leadRepository, botRepository, notificationService, userRepository, authService, tenantAccessValidator);
        documentService = new DocumentService(documentRepository, botRepository, pythonRagClient, authService, tenantAccessValidator);
        whatsappConfigService = new WhatsappConfigService(whatsappConfigRepository, botRepository, tenantAccessValidator);

        adminRole = Entities.role(RoleConstants.ADMIN_ROLE, "ADMIN");
        userRole = Entities.role(RoleConstants.USER_ROLE, "USER");

        tenantAId = UUID.randomUUID();
        tenantBId = UUID.randomUUID();
        tenantA = Entities.tenant(tenantAId, "TenantA", ImplementationType.WIDGET);
        tenantB = Entities.tenant(tenantBId, "TenantB", ImplementationType.WIDGET);

        userA = Entities.user(UUID.randomUUID(), "u-a@example.com", tenantA, userRole);
        // El admin puede o no tener tenant propio — dejamos null para probar el bypass puro.
        adminUser = Entities.user(UUID.randomUUID(), "admin@example.com", null, adminRole);

        botAId = UUID.randomUUID();
        botBId = UUID.randomUUID();
        botTenantA = Entities.bot(botAId, tenantA);
        botTenantB = Entities.bot(botBId, tenantB);
    }

    // =======================================================================
    // Caso 1 — USER tenantA NO puede LISTAR ni LEER bots del tenantB (GET)
    // =======================================================================

    @Nested
    @DisplayName("Caso 1 — leer/listar bots de otro tenant (GET)")
    class ReadBotsOfOtherTenant {

        @Test
        @DisplayName("USER tenantA pidiendo el select de bots del tenantB → 403")
        void userCannotListBotsOfOtherTenant() {
            when(authService.getUserAuthenticated()).thenReturn(userA);

            AppException ex = assertThrows(AppException.class,
                    () -> botService.getBotsByTenantIdSelect(tenantBId));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(botRepository, never()).findByTenant_Id(any(UUID.class));
        }

        @Test
        @DisplayName("USER tenantA en getBotByTenant con tenantFilter del tenantB → sólo ve su propio tenant")
        void userIgnoresForeignTenantFilter() {
            // El handler acepta un tenantFilter como param. Non-admin: se
            // cierra al tenant propio (defense-in-depth frente a un front que
            // arme la URL con el tenant equivocado).
            when(authService.getUserAuthenticated()).thenReturn(userA);
            Pageable pageable = PageRequest.of(0, 10);
            when(botRepository.findByTenant_Id(eq(tenantAId), eq(pageable)))
                    .thenReturn(Page.empty(pageable));

            botService.getBotByTenant(/* tenantIdFilter = */ tenantBId, pageable);

            verify(botRepository).findByTenant_Id(eq(tenantAId), eq(pageable));
            verify(botRepository, never()).findByTenant_Id(eq(tenantBId), any(Pageable.class));
        }

        @Test
        @DisplayName("USER tenantA con getBotById de bot del tenantB → 403 (FIX 1)")
        void userCannotGetBotByIdOfOtherTenant() {
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));
            when(authService.getUserAuthenticated()).thenReturn(userA);

            AppException ex = assertThrows(AppException.class,
                    () -> botService.getBotById(botBId));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        }

        @Test
        @DisplayName("USER tenantA con getSystemPrompt de bot del tenantB → 403 (FIX 2)")
        void userCannotGetSystemPromptOfOtherTenant() {
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));
            when(authService.getUserAuthenticated()).thenReturn(userA);

            AppException ex = assertThrows(AppException.class,
                    () -> botService.getSystemPrompt(botBId));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        }

        @Test
        @DisplayName("USER tenantA con getBotById de su propio bot → pasa")
        void userCanGetBotByIdOfOwnTenant() {
            when(botRepository.findById(botAId)).thenReturn(Optional.of(botTenantA));
            when(authService.getUserAuthenticated()).thenReturn(userA);

            // No debe tirar.
            botService.getBotById(botAId);
        }
    }

    // =======================================================================
    // Caso 2 — USER tenantA NO puede EDITAR bots del tenantB (PUT)
    // =======================================================================

    @Nested
    @DisplayName("Caso 2 — editar bot de otro tenant (PUT)")
    class EditBotOfOtherTenant {

        @Test
        @DisplayName("USER tenantA haciendo updateBot sobre bot del tenantB → 403")
        void userCannotUpdateBotOfOtherTenant() {
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));
            when(authService.getUserAuthenticated()).thenReturn(userA);

            UpdateBotDto dto = new UpdateBotDto();
            Entities.setField(dto, "name", "hacked"); // al menos un campo (BotService exige ≥1)

            AppException ex = assertThrows(AppException.class,
                    () -> botService.updateBot(botBId, dto));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(botRepository, never()).save(any(Bot.class));
        }

        @Test
        @DisplayName("USER tenantA haciendo updateSystemPrompt sobre bot del tenantB → 403")
        void userCannotUpdateSystemPromptOfOtherTenant() {
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));
            when(authService.getUserAuthenticated()).thenReturn(userA);

            UpdateSystemPromptDto dto = new UpdateSystemPromptDto();
            Entities.setField(dto, "systemPrompt", "pwned");

            AppException ex = assertThrows(AppException.class,
                    () -> botService.updateSystemPrompt(botBId, dto));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(botRepository, never()).save(any(Bot.class));
        }
    }

    // =======================================================================
    // Caso 3 — USER tenantA NO puede ELIMINAR bots del tenantB (DELETE)
    // =======================================================================

    @Test
    @DisplayName("Caso 3 — USER tenantA haciendo deleteBot sobre bot del tenantB → 403")
    void userCannotDeleteBotOfOtherTenant() {
        when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));
        when(authService.getUserAuthenticated()).thenReturn(userA);

        AppException ex = assertThrows(AppException.class,
                () -> botService.deleteBot(botBId));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(botRepository, never()).deleteById(any(UUID.class));
    }

    // =======================================================================
    // Caso 4 — USER tenantA NO puede VER/MODIFICAR recursos child
    //           (conversations / leads / documents) del tenantB
    // =======================================================================

    @Nested
    @DisplayName("Caso 4 — recursos child de otro tenant")
    class ChildResourcesOfOtherTenant {

        // ---- Conversations

        @Test
        @DisplayName("USER tenantA con getConversations(tenantId=B) → 403")
        void userCannotListConversationsOfOtherTenant() {
            when(authService.getUserAuthenticated()).thenReturn(userA);

            AppException ex = assertThrows(AppException.class,
                    () -> conversationService.getConversations(null, tenantBId, PageRequest.of(0, 10)));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(conversationRepository, never()).findByTenantId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("USER tenantA con getConversations(botId de tenantB) → 403")
        void userCannotListConversationsOfBotInOtherTenant() {
            when(authService.getUserAuthenticated()).thenReturn(userA);
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));

            AppException ex = assertThrows(AppException.class,
                    () -> conversationService.getConversations(botBId, null, PageRequest.of(0, 10)));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(conversationRepository, never()).findByBotId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("USER tenantA con getConversationBySession de conversación del tenantB → 403 (FIX 3)")
        void userCannotReadConversationBySessionOfOtherTenant() {
            String sessionId = "sess-pwn-1234";
            Conversation conv = newConversationForBot(botBId, sessionId);
            when(conversationRepository.existsBySessionId(sessionId)).thenReturn(true);
            when(conversationRepository.findBySessionIdOrderByCreatedAtAsc(sessionId))
                    .thenReturn(List.of(conv));
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));
            when(authService.getUserAuthenticated()).thenReturn(userA);

            AppException ex = assertThrows(AppException.class,
                    () -> conversationService.getConversationBySession(sessionId));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        }

        // ---- Leads

        @Test
        @DisplayName("USER tenantA con getLeads(tenantId=B) → 403")
        void userCannotListLeadsOfOtherTenant() {
            when(authService.getUserAuthenticated()).thenReturn(userA);

            AppException ex = assertThrows(AppException.class,
                    () -> leadService.getLeads(null, tenantBId, PageRequest.of(0, 10)));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(leadRepository, never()).findByTenantId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("USER tenantA con getLeads(botId de tenantB) → 403")
        void userCannotListLeadsOfBotInOtherTenant() {
            when(authService.getUserAuthenticated()).thenReturn(userA);
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));

            AppException ex = assertThrows(AppException.class,
                    () -> leadService.getLeads(botBId, null, PageRequest.of(0, 10)));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(leadRepository, never()).findByBotId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("USER tenantA con updateLeadStatus de lead del tenantB → 403 (FIX 7)")
        void userCannotUpdateLeadStatusOfOtherTenant() {
            UUID leadId = UUID.randomUUID();
            Lead lead = Lead.builder().botId(botBId).status(LeadStatus.PENDING).build();
            when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));
            when(authService.getUserAuthenticated()).thenReturn(userA);

            AppException ex = assertThrows(AppException.class,
                    () -> leadService.updateLeadStatus(leadId, LeadStatus.CONTACTED));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(leadRepository, never()).save(any(Lead.class));
        }

        // ---- Documents

        @Test
        @DisplayName("USER tenantA con getDocuments(tenantId=B) → 403")
        void userCannotListDocumentsOfOtherTenant() {
            when(authService.getUserAuthenticated()).thenReturn(userA);

            AppException ex = assertThrows(AppException.class,
                    () -> documentService.getDocuments(null, tenantBId, PageRequest.of(0, 10)));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(documentRepository, never()).findByTenantId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("USER tenantA con getDocuments(botId de tenantB) → 403")
        void userCannotListDocumentsOfBotInOtherTenant() {
            when(authService.getUserAuthenticated()).thenReturn(userA);
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));

            AppException ex = assertThrows(AppException.class,
                    () -> documentService.getDocuments(botBId, null, PageRequest.of(0, 10)));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(documentRepository, never()).findByBotId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("USER tenantA con uploadDocument a bot del tenantB → 403 (FIX 5)")
        void userCannotUploadDocumentToBotInOtherTenant() {
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));
            when(authService.getUserAuthenticated()).thenReturn(userA);

            AppException ex = assertThrows(AppException.class,
                    () -> documentService.uploadDocument(botBId, List.of()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(documentRepository, never()).save(any(Document.class));
        }

        @Test
        @DisplayName("USER tenantA con deleteDocument de documento del tenantB → 403 (FIX 6)")
        void userCannotDeleteDocumentOfOtherTenant() {
            UUID documentId = UUID.randomUUID();
            Document doc = Document.builder().botId(botBId).filePath("/tmp/no").build();
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));
            when(authService.getUserAuthenticated()).thenReturn(userA);

            AppException ex = assertThrows(AppException.class,
                    () -> documentService.deleteDocument(documentId));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            // Ni siquiera debe haber intentado hablar con el Python RAG ni borrar
            // la fila — el guard va antes de cualquier side-effect.
            verify(documentRepository, never()).deleteById(any(UUID.class));
        }
    }

    // =======================================================================
    // Caso 5 — ADMIN ve/opera sobre todos los tenants (bypass legítimo)
    // =======================================================================

    @Nested
    @DisplayName("Caso 5 — ADMIN bypass legítimo")
    class AdminBypass {

        @Test
        @DisplayName("ADMIN puede listar bots de cualquier tenant (getBotsByTenantIdSelect)")
        void adminCanListBotsOfAnyTenant() {
            when(authService.getUserAuthenticated()).thenReturn(adminUser);
            when(botRepository.findByTenant_Id(tenantBId)).thenReturn(List.of(botTenantB));

            botService.getBotsByTenantIdSelect(tenantBId);

            verify(botRepository).findByTenant_Id(tenantBId);
        }

        @Test
        @DisplayName("ADMIN puede leer cualquier bot (getBotById)")
        void adminCanGetAnyBotById() {
            when(authService.getUserAuthenticated()).thenReturn(adminUser);
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));

            botService.getBotById(botBId);
            // No debe tirar.
        }

        @Test
        @DisplayName("ADMIN puede leer el system prompt de cualquier bot")
        void adminCanGetAnySystemPrompt() {
            when(authService.getUserAuthenticated()).thenReturn(adminUser);
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));

            botService.getSystemPrompt(botBId);
        }

        @Test
        @DisplayName("ADMIN puede eliminar bots de cualquier tenant")
        void adminCanDeleteBotOfAnyTenant() {
            when(authService.getUserAuthenticated()).thenReturn(adminUser);
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));

            botService.deleteBot(botBId);

            verify(botRepository).deleteById(botBId);
        }

        @Test
        @DisplayName("ADMIN puede editar bots de cualquier tenant")
        void adminCanUpdateBotOfAnyTenant() {
            when(authService.getUserAuthenticated()).thenReturn(adminUser);
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));
            when(botRepository.save(any(Bot.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateBotDto dto = new UpdateBotDto();
            Entities.setField(dto, "name", "renombrado-por-admin");

            botService.updateBot(botBId, dto);

            verify(botRepository).save(any(Bot.class));
            assertEquals("renombrado-por-admin", botTenantB.getName());
        }

        @Test
        @DisplayName("ADMIN con getConversations sin params ve todos los tenants (findAll)")
        void adminWithoutTenantIdSeesAllConversations() {
            when(authService.getUserAuthenticated()).thenReturn(adminUser);
            Pageable pageable = PageRequest.of(0, 10);
            when(conversationRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            conversationService.getConversations(null, null, pageable);

            verify(conversationRepository).findAll(pageable);
            verify(conversationRepository, never()).findByTenantId(any(UUID.class), any(Pageable.class));
        }

        @Test
        @DisplayName("ADMIN puede actualizar status de leads de cualquier tenant")
        void adminCanUpdateLeadStatusOfAnyTenant() {
            UUID leadId = UUID.randomUUID();
            Lead lead = Lead.builder().botId(botBId).status(LeadStatus.PENDING).build();
            when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));
            when(authService.getUserAuthenticated()).thenReturn(adminUser);

            leadService.updateLeadStatus(leadId, LeadStatus.CONTACTED);

            verify(leadRepository).save(lead);
            assertEquals(LeadStatus.CONTACTED, lead.getStatus());
        }

        @Test
        @DisplayName("ADMIN puede borrar documentos de cualquier tenant (bypass — no hay side-effect real en unit test)")
        void adminCanDeleteDocumentOfAnyTenant() {
            UUID documentId = UUID.randomUUID();
            Document doc = Document.builder()
                    .botId(botBId)
                    // Archivo que no existe — deleteIfExists() retorna false silenciosamente,
                    // así que el service no rompe en unit test.
                    .filePath("this-file-definitely-does-not-exist.bin")
                    .build();
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(doc));
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));
            when(authService.getUserAuthenticated()).thenReturn(adminUser);

            documentService.deleteDocument(documentId);

            verify(documentRepository).deleteById(documentId);
        }
    }

    // =======================================================================
    // Caso 6 — tenant_id en body IGNORADO para non-admin
    // =======================================================================

    @Test
    @DisplayName("Caso 6 — USER tenantA mandando tenant_id=B en createBot body → se fuerza al tenantA del JWT")
    void nonAdminCannotSpoofTenantInRequestBody() {
        when(authService.getUserAuthenticated()).thenReturn(userA);
        when(tenantRepository.findById(tenantAId)).thenReturn(Optional.of(tenantA));

        when(userRepository.findAllById(List.of(userA.getUserId())))
                .thenReturn(List.of(userA));

        ArgumentCaptor<Bot> botCaptor = ArgumentCaptor.forClass(Bot.class);
        when(botRepository.save(botCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        RegisterBotDto dto = new RegisterBotDto(
                "bot-spoof", "descr",
                /* tenantID = */ tenantBId,
                /* leadAssigneeUserIds = */ List.of(userA.getUserId())
        );

        botService.createBot(dto);

        Bot saved = botCaptor.getValue();
        assertEquals(tenantAId, saved.getTenant().getId(),
                "tenant_id del body debe ser ignorado para non-admin — el tenant se deriva del JWT");
        verify(tenantRepository).findById(tenantAId);
        verify(tenantRepository, never()).findById(tenantBId);
    }

    // =======================================================================
    // Caso 7 — WhatsappConfigService (FIX 8 — los 4 métodos)
    // =======================================================================

    @Nested
    @DisplayName("Caso 7 — whatsapp-config de otro tenant (FIX 8)")
    class WhatsappConfigOfOtherTenant {

        @Test
        @DisplayName("USER tenantA con createConfig sobre bot del tenantB → 403")
        void userCannotCreateWhatsappConfigOnBotInOtherTenant() {
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));
            when(authService.getUserAuthenticated()).thenReturn(userA);

            CreateWhatsappConfigRequest req = new CreateWhatsappConfigRequest();
            Entities.setField(req, "phoneNumberId", "pnid");
            Entities.setField(req, "accessToken", "secret");

            AppException ex = assertThrows(AppException.class,
                    () -> whatsappConfigService.createConfig(botBId, req));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(whatsappConfigRepository, never()).save(any(WhatsappConfig.class));
        }

        @Test
        @DisplayName("USER tenantA con getConfigByBot de bot del tenantB → 403")
        void userCannotGetWhatsappConfigOfBotInOtherTenant() {
            when(botRepository.findById(botBId)).thenReturn(Optional.of(botTenantB));
            when(authService.getUserAuthenticated()).thenReturn(userA);

            AppException ex = assertThrows(AppException.class,
                    () -> whatsappConfigService.getConfigByBot(botBId));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(whatsappConfigRepository, never()).findByBot_BotId(any(UUID.class));
        }

        @Test
        @DisplayName("USER tenantA con updateConfig sobre config del tenantB → 403")
        void userCannotUpdateWhatsappConfigOfOtherTenant() {
            UUID configId = UUID.randomUUID();
            WhatsappConfig config = newWhatsappConfigForBot(botTenantB);
            when(whatsappConfigRepository.findById(configId)).thenReturn(Optional.of(config));
            when(authService.getUserAuthenticated()).thenReturn(userA);

            CreateWhatsappConfigRequest req = new CreateWhatsappConfigRequest();
            Entities.setField(req, "phoneNumberId", "pnid2");
            Entities.setField(req, "accessToken", "secret2");

            AppException ex = assertThrows(AppException.class,
                    () -> whatsappConfigService.updateConfig(configId, req));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(whatsappConfigRepository, never()).save(any(WhatsappConfig.class));
        }

        @Test
        @DisplayName("USER tenantA con deleteConfig sobre config del tenantB → 403")
        void userCannotDeleteWhatsappConfigOfOtherTenant() {
            UUID configId = UUID.randomUUID();
            WhatsappConfig config = newWhatsappConfigForBot(botTenantB);
            when(whatsappConfigRepository.findById(configId)).thenReturn(Optional.of(config));
            when(authService.getUserAuthenticated()).thenReturn(userA);

            AppException ex = assertThrows(AppException.class,
                    () -> whatsappConfigService.deleteConfig(configId));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            verify(whatsappConfigRepository, never()).delete(any(WhatsappConfig.class));
        }

        @Test
        @DisplayName("ADMIN puede borrar cualquier whatsapp-config")
        void adminCanDeleteAnyWhatsappConfig() {
            UUID configId = UUID.randomUUID();
            WhatsappConfig config = newWhatsappConfigForBot(botTenantB);
            when(whatsappConfigRepository.findById(configId)).thenReturn(Optional.of(config));
            when(authService.getUserAuthenticated()).thenReturn(adminUser);

            whatsappConfigService.deleteConfig(configId);

            verify(whatsappConfigRepository).delete(config);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Construye un {@link Conversation} con botId arbitrario vía reflection.
     * {@code Conversation} tiene {@code @NoArgsConstructor(access = PROTECTED)}
     * y un constructor público de 5 args — usamos el de 5 args y luego fijamos
     * botId (ya queda puesto) y sessionId.
     */
    private Conversation newConversationForBot(UUID botId, String sessionId) {
        Conversation conv = Conversation.create(botId, sessionId, "user", "msg");
        return conv;
    }

    /**
     * Construye un {@link WhatsappConfig} apuntando al {@link Bot} dado.
     * Tiene constructor de 4 args (bot, phoneNumberId, accessToken, isActive).
     */
    private WhatsappConfig newWhatsappConfigForBot(Bot bot) {
        try {
            Constructor<WhatsappConfig> ctor = WhatsappConfig.class
                    .getDeclaredConstructor(Bot.class, String.class, String.class, Boolean.class);
            ctor.setAccessible(true);
            return ctor.newInstance(bot, "pnid-x", "key-x", Boolean.TRUE);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
