package com.chatbotsaas.chatbot_saas.bot.service;

import com.chatbotsaas.chatbot_saas.auth.service.AuthService;
import com.chatbotsaas.chatbot_saas.bot.dto.request.RegisterBotDto;
import com.chatbotsaas.chatbot_saas.bot.dto.request.UpdateBotDto;
import com.chatbotsaas.chatbot_saas.bot.dto.request.UpdateSystemPromptDto;
import com.chatbotsaas.chatbot_saas.bot.dto.response.*;
import com.chatbotsaas.chatbot_saas.bot.entity.Bot;
import com.chatbotsaas.chatbot_saas.bot.repository.BotRepository;
import com.chatbotsaas.chatbot_saas.role.constant.RoleConstants;
import com.chatbotsaas.chatbot_saas.shared.exception.AppException;
import com.chatbotsaas.chatbot_saas.shared.security.TenantAccessValidator;
import com.chatbotsaas.chatbot_saas.tenant.entity.Tenant;
import com.chatbotsaas.chatbot_saas.tenant.enums.ImplementationType;
import com.chatbotsaas.chatbot_saas.tenant.repository.TenantRepository;
import com.chatbotsaas.chatbot_saas.user.entity.Person;
import com.chatbotsaas.chatbot_saas.user.entity.User;
import com.chatbotsaas.chatbot_saas.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class BotService {
    private final BotRepository botRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final TenantAccessValidator tenantAccessValidator;

    public BotService(BotRepository botRepository, TenantRepository tenantRepository, UserRepository userRepository, AuthService authService, TenantAccessValidator tenantAccessValidator) {
        this.botRepository = botRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.authService = authService;
        this.tenantAccessValidator = tenantAccessValidator;
    }

    @Transactional
    public ResponseBotDto createBot(RegisterBotDto bot) {
        User user = authService.getUserAuthenticated();

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleId().equals(RoleConstants.ADMIN_ROLE));
        UUID tenantId;

        if (isAdmin) {
            if (bot.getTenantID() == null) {
                throw new AppException("Tenant is required", HttpStatus.BAD_REQUEST);
            }
            tenantId = bot.getTenantID();
        } else {
            tenantId = user.getTenant().getId();
        }

        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(
                () -> new IllegalArgumentException("Tenant not found")
        );

        // Regla: si el tenant usa widget (WIDGET/BOTH) los leads se asignan por
        // round-robin → sin assignees el primer lead que entra revienta con 409.
        // Se valida AQUÍ (y no en Bean Validation) porque el requirement depende
        // del implementation_type del tenant, no del payload aislado.
        ImplementationType impl = tenant.getImplementationType();
        boolean tenantUsesAssignees = impl == ImplementationType.WIDGET
                                   || impl == ImplementationType.BOTH;

        List<User> resolvedAssignees = List.of();
        if (tenantUsesAssignees) {
            List<UUID> ids = bot.getLeadAssigneeUserIds();
            if (ids == null || ids.isEmpty()) {
                throw new AppException(
                        "Debe asignar al menos un responsable al bot",
                        HttpStatus.BAD_REQUEST);
            }
            resolvedAssignees = userRepository.findAllById(ids);
            if (resolvedAssignees.size() != ids.size()) {
                throw new AppException(
                        "Alguno de los responsables indicados no existe",
                        HttpStatus.BAD_REQUEST);
            }
            // Todos los assignees deben pertenecer al mismo tenant del bot.
            UUID tId = tenant.getId();
            boolean crossTenant = resolvedAssignees.stream().anyMatch(u ->
                    u.getTenant() == null || !u.getTenant().getId().equals(tId));
            if (crossTenant) {
                throw new AppException(
                        "Los responsables deben pertenecer al tenant del bot",
                        HttpStatus.BAD_REQUEST);
            }
        }
        // impl == WHATSAPP → se ignora silenciosamente el campo si viene.

        Bot created = Bot.create(bot.getName(), bot.getDescription(), tenant);
        if (!resolvedAssignees.isEmpty()) {
            created.setLeadAssignees(new java.util.ArrayList<>(resolvedAssignees));
            created.setLastAssigneeIndex(0);
        }
        Bot saved = botRepository.save(created);

        return toResponseBotDto(saved);
    }

    @Transactional
    public Page<ResponseBotTableDto> getBotByTenant(UUID tenantIdFilter, Pageable pageable) {
        User user = authService.getUserAuthenticated();

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleId().equals(RoleConstants.ADMIN_ROLE));

        // Resolución del tenant efectivo:
        //  - Admin + tenantIdFilter != null → filtramos por ese tenant.
        //  - Admin sin filtro                → sigue viendo TODOS los bots.
        //  - Non-admin                       → siempre su propio tenant,
        //    ignorando cualquier tenantIdFilter que mande el cliente (defensa
        //    frente a spoof: @PreAuthorize ya valida el permiso, acá cerramos
        //    el alcance por tenant).
        UUID effectiveTenantId;
        if (isAdmin) {
            effectiveTenantId = tenantIdFilter;
        } else {
            effectiveTenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        }

        Page<Bot> bots = (effectiveTenantId == null)
                ? botRepository.findAll(pageable)
                : botRepository.findByTenant_Id(effectiveTenantId, pageable);

        return bots.map(bot -> ResponseBotTableDto.builder()
                .botId(bot.getBotId())
                .name(bot.getName())
                .description(bot.getDescription())
                .isActive(bot.getIsActive())
                .createdAt(bot.getCreatedAt())
                .tenantId(bot.getTenant() != null ? bot.getTenant().getId() : null)
                .tenantName(bot.getTenant() != null ? bot.getTenant().getName() : null)
                .implementationType(bot.getTenant() != null && bot.getTenant().getImplementationType() != null
                        ? bot.getTenant().getImplementationType().name()
                        : null)
                .leadAssignees(mapAssignees(bot))
                .build()
        );
    }

    @Transactional
    public List<ResponseBotSelectDto> getBotByTenantSelect() {
        User user = authService.getUserAuthenticated();

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getRoleId().equals(RoleConstants.ADMIN_ROLE));

        List<Bot> bots = isAdmin
                ? botRepository.findAll()
                : botRepository.findByTenant_Id(user.getTenant().getId());

        return bots.stream()
                .map(bot -> ResponseBotSelectDto.builder()
                        .botId(bot.getBotId())
                        .name(bot.getName())
                        .build()
                ).toList();
    }

    @Transactional(readOnly = true)
    public List<ResponseBotSelectDto> getBotsByTenantIdSelect(UUID tenantId) {
        User user = authService.getUserAuthenticated();
        if (user == null) {
            throw new AppException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isAdmin()) {
            UUID myTenantId = user.getTenant() != null ? user.getTenant().getId() : null;
            if (!tenantId.equals(myTenantId)) {
                throw new AppException("Forbidden", HttpStatus.FORBIDDEN);
            }
        }

        List<Bot> bots = botRepository.findByTenant_Id(tenantId);

        return bots.stream()
                .map(bot -> ResponseBotSelectDto.builder()
                        .botId(bot.getBotId())
                        .name(bot.getName())
                        .build()
                ).toList();
    }

    @Transactional
    public void deleteBot(UUID botId) {
        Bot bot = botRepository.findById(botId).orElseThrow(
                () -> new AppException("El bot no existe", HttpStatus.NOT_FOUND)
        );
        // Aislamiento multi-tenant — borrado: delegamos al validator compartido
        // (mismo guard que usan updateBot / updateSystemPrompt / getBotById).
        tenantAccessValidator.assertCanAccessBot(bot);

        botRepository.deleteById(botId);
    }

    @Transactional
    public ResponseBotDto getBotById(UUID botId) {
        Bot bot = botRepository.findById(botId).orElseThrow(
                () -> new AppException("Bot not found", HttpStatus.NOT_FOUND)
        );
        // Aislamiento multi-tenant — lectura: el bot solo puede ser consultado
        // por users del mismo tenant (admin bypass vive en el validator).
        tenantAccessValidator.assertCanAccessBot(bot);
        return toResponseBotDto(bot);
    }

    /**
     * Versión pública del bot — usada por el widget embebido en sitios de
     * clientes. <b>Sin {@code TenantAccessValidator}</b>: el endpoint que
     * la invoca ({@code GET /api/bot/get_widget_bot/{bot_id}}) está en
     * {@code permitAll()} de SecurityConfig. La identificación es por
     * {@code bot_id} (UUID, alta entropía) — el cliente que embebe el
     * widget conoce su propio bot_id, terceros no lo adivinan.
     *
     * <p>Expone solo lo mínimo necesario para renderizar la UI inicial
     * del widget — hoy únicamente el nombre. NO expone {@code system_prompt},
     * {@code tenant_id}, {@code lead_assignees} ni nada sensible.
     */
    @Transactional(readOnly = true)
    public ResponseBotWidget getWidgetBot(UUID botId) {
        Bot bot = botRepository.findById(botId).orElseThrow(
                () -> new AppException("Bot not found", HttpStatus.NOT_FOUND)
        );
        return ResponseBotWidget.builder()
                .name(bot.getName())
                .build();
    }

    @Transactional
    public SystemPromptDto getSystemPrompt(UUID botId) {
        Bot bot = botRepository.findById(botId).orElseThrow(
                () -> new AppException("Bot not found", HttpStatus.NOT_FOUND)
        );
        // El system prompt es propiedad intelectual del cliente — mismo guard
        // que getBotById.
        tenantAccessValidator.assertCanAccessBot(bot);
        return SystemPromptDto.builder()
                .systemPrompt(bot.getSystemPrompt())
                .build();
    }

    @Transactional
    public ResponseBotDto updateBot(UUID botId, UpdateBotDto dto) {
        if (dto.getName() == null
                && dto.getDescription() == null
                && dto.getLeadAssigneeUserIds() == null) {
            throw new AppException(
                    "Debes enviar al menos un campo para actualizar",
                    HttpStatus.BAD_REQUEST
            );
        }

        Bot bot = botRepository.findById(botId).orElseThrow(
                () -> new AppException("El bot no existe", HttpStatus.NOT_FOUND)
        );

        tenantAccessValidator.assertCanAccessBot(bot);

        if (dto.getName() != null) {
            bot.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            bot.setDescription(dto.getDescription());
        }

        // Reemplazo total de assignees. Null = no tocar. Lista vacía = rechazado
        // (dejar 0 assignees rompe el round-robin → primer lead cae en 409).
        if (dto.getLeadAssigneeUserIds() != null) {
            List<UUID> ids = dto.getLeadAssigneeUserIds();
            if (ids.isEmpty()) {
                throw new AppException(
                        "Debe asignar al menos un responsable al bot",
                        HttpStatus.BAD_REQUEST);
            }
            List<User> resolved = userRepository.findAllById(ids);
            if (resolved.size() != ids.size()) {
                throw new AppException(
                        "Alguno de los responsables indicados no existe",
                        HttpStatus.BAD_REQUEST);
            }
            UUID tId = bot.getTenant() != null ? bot.getTenant().getId() : null;
            boolean crossTenant = resolved.stream().anyMatch(u ->
                    u.getTenant() == null
                 || tId == null
                 || !u.getTenant().getId().equals(tId));
            if (crossTenant) {
                throw new AppException(
                        "Los responsables deben pertenecer al tenant del bot",
                        HttpStatus.BAD_REQUEST);
            }
            bot.getLeadAssignees().clear();
            bot.getLeadAssignees().addAll(resolved);
            // Reset del round-robin: el nuevo set puede ser más corto y
            // `(lastIndex + 1) % size` podría quedar fuera de rango.
            bot.setLastAssigneeIndex(0);
        }

        Bot saved = botRepository.save(bot);
        return toResponseBotDto(saved);
    }

    @Transactional
    public SystemPromptDto updateSystemPrompt(UUID botId, UpdateSystemPromptDto dto) {
        Bot bot = botRepository.findById(botId).orElseThrow(
                () -> new AppException("El bot no existe", HttpStatus.NOT_FOUND)
        );

        tenantAccessValidator.assertCanAccessBot(bot);

        bot.setSystemPrompt(dto.getSystemPrompt());
        Bot saved = botRepository.save(bot);

        return SystemPromptDto.builder()
                .systemPrompt(saved.getSystemPrompt())
                .build();
    }

    /**
     * Mapper central Bot → ResponseBotDto. Centralizado para que los 3 endpoints
     * que devuelven un bot individual (create/getById/update) expongan SIEMPRE
     * la misma forma — evita que el frontend tenga que defender cada campo con
     * opcional-chaining.
     */
    private ResponseBotDto toResponseBotDto(Bot bot) {
        return ResponseBotDto.builder()
                .botId(bot.getBotId())
                .name(bot.getName())
                .description(bot.getDescription())
                .isActive(bot.getIsActive())
                .implementationType(bot.getTenant() != null && bot.getTenant().getImplementationType() != null
                        ? bot.getTenant().getImplementationType().name()
                        : null)
                .leadAssignees(mapAssignees(bot))
                .build();
    }

    private List<BotAssigneeDto> mapAssignees(Bot bot) {
        if (bot.getLeadAssignees() == null || bot.getLeadAssignees().isEmpty()) {
            return List.of();
        }
        return bot.getLeadAssignees().stream()
                .map(u -> {
                    Person p = u.getPerson();
                    String full = p != null
                            ? ((p.getName() != null ? p.getName() : "") + " "
                             + (p.getLastname() != null ? p.getLastname() : "")).trim()
                            : "";
                    return BotAssigneeDto.builder()
                            .userId(u.getUserId())
                            .fullName(full)
                            .build();
                })
                .toList();
    }

}