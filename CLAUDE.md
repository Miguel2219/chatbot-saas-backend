# ChatBot SaaS — Spring Boot Backend Context

## Project Overview
Multi-tenant AI chatbot SaaS platform for businesses of any size and industry.
Businesses register, create bots, upload documents, and get an embeddable chat
widget or WhatsApp integration. The AI is handled by a separate FastAPI Python
microservice using RAG (ChromaDB + OpenAI).

## Tech Stack
- Spring Boot 3.x, Java 21, PostgreSQL
- Spring Security + JWT authentication
- Spring Data JPA / Hibernate
- WebClient (WebFlux) for external HTTP calls
- Lombok, Jakarta Validation
- JavaMailSender + Gmail SMTP for email notifications
- jjwt 0.11.5 for JWT token handling

## Package Structure
```
com.chatbotsaas.chatbot_saas
├── auth/
│   ├── controller/AuthController.java
│   ├── dto/
│   │   ├── request/RegisterRequest.java
│   │   ├── request/LoginRequest.java
│   │   └── response/LoginResponse.java
│   ├── security/
│   │   ├── JwtAuthFilter.java
│   │   ├── JwtTokenProvider.java
│   │   ├── JwtAuthFilter.java
│   │   └── UserDetailsServiceImpl.java
│   └── service/AuthService.java
├── bot/
│   ├── controller/BotController.java
│   ├── dto/request/RegisterBotDto.java
│   ├── dto/response/ResponseBotDto.java
│   ├── entity/Bot.java
│   ├── repository/BotRepository.java
│   └── service/BotService.java
├── chat/
│   ├── controller/ChatController.java
│   ├── dto/request/ChatRequestDto.java
│   ├── dto/response/ChatResponseDto.java
│   └── service/ChatService.java
├── config/
│   ├── CorsConfig.java
│   ├── SwaggerConfig.java
├── conversation/
│   ├── controller/ConversationController.java
│   ├── dto/request/ConversationRequestDto.java
│   ├── dto/response/ConversationResponseDto.java
│   ├── entity/Conversation.java
│   ├── enums/ConversationStatus.java  (BOT_ACTIVE, PENDING_HUMAN, HUMAN_ACTIVE)
│   ├── repository/ConversationRepository.java
│   └── service/ConversationService.java
├── document/
│   ├── controller/DocumentController.java
│   ├── dto/response/DocumentResponseDto.java
│   ├── entity/Document.java
│   ├── repository/DocumentRepository.java
│   └── service/DocumentService.java
├── integration/
│   ├── PythonRagClient.java
│   └── dto/
│       ├── request/ChatRequest.java
│       ├── request/ProcessDocumentRequest.java
│       ├── request/DeleteDocumentRequest.java
│       └── response/ChatResponsePythonDto.java
├── lead/
│   ├── controller/LeadController.java
│   ├── dto/request/LeadRequestDto.java
│   ├── dto/response/LeadResponseDto.java
│   ├── dto/response/LeadDataDto.java
│   ├── entity/Lead.java
│   ├── enums/LeadStatus.java          (PENDING, CONTACTED, CLOSED)
│   ├── enums/LeadChannel.java         (WIDGET, WHATSAPP)
│   ├── repository/LeadRepository.java
│   └── service/LeadService.java
├── notification/
│   └── service/
│       ├── EmailService.java
│       └── NotificationService.java
├── role/
│   └── constant/RoleConstants.java    (enum: ADMIN, USER, ADVISER)
├── shared/
│   ├── exception/
│   │   ├── AppException.java
│   │   ├── ErrorResponse.java
│   │   └── GlobalExceptionHandler.java
│   └── security/
│       ├── JwtTokenProvider.java
│       ├── JwtAuthFilter.java
│       └── UserDetailsServiceImpl.java
├── tenant/
│   ├── entity/Tenant.java
│   └── repository/TenantRepository.java
├── user/
│   ├── controller/AdviserController.java
│   ├── dto/request/CreateAdviserRequestDto.java
│   ├── dto/response/AdviserResponseDto.java
│   ├── entity/User.java
│   ├── entity/Person.java
│   ├── enums/NotificationChannel.java  (EMAIL, WHATSAPP, BOTH)
│   ├── repository/UserRepository.java
│   ├── repository/PersonRepository.java
│   └── service/AdviserService.java
└── whatsapp/
    ├── controller/WhatsappConfigController.java  (TODO)
    ├── controller/WhatsappWebhookController.java (TODO)
    ├── dto/request/CreateWhatsappConfigRequest.java
    ├── dto/request/SendMessageRequestDto.java
    ├── dto/request/WhatsappMessageRequestDto.java
    ├── dto/response/WhatsappConfigResponseDto.java
    ├── dto/WhatsappWebhookPayloadDto.java
    ├── entity/WhatsappConfig.java
    ├── repository/WhatsappConfigRepository.java
    ├── service/WhatsappConfigService.java
    └── service/WhatsappService.java
```

## Entity Relationships
```
Tenant (1) ──< (N) User
Tenant (1) ──< (N) Bot
User (1) ──── (1) Person         (@MapsId — shares same UUID)
Bot (N) >────< (N) User           (bot_advisers join table — advisers)
Bot (1) ──── (1) WhatsappConfig  (@OneToOne)
Document.botId ──── UUID         (plain UUID reference to Bot)
Conversation.botId ──── UUID     (plain UUID reference to Bot)
Lead.botId ──── UUID             (plain UUID reference to Bot)
Lead.assignedAdviserId ──── UUID (plain UUID reference to User)
```

## Key Conventions — follow these strictly
- `AppException(message, HttpStatus)` for ALL error handling — never throw raw exceptions
- `GlobalExceptionHandler` catches `AppException` and `WebClientRequestException`
- Plain UUID for cross-module references (botId, tenantId) — no @ManyToOne unless needed
- `@JsonProperty` with snake_case on ALL DTO fields
- `@Transactional` on ALL service methods
- `WebClient` pattern for external HTTP calls (see PythonRagClient as reference)
- `RoleConstants` enum for roles: ADMIN, USER, ADVISER
- `NotificationChannel` enum: EMAIL, WHATSAPP, BOTH
- `ConversationStatus` enum: BOT_ACTIVE, PENDING_HUMAN, HUMAN_ACTIVE
- `LeadChannel` enum: WIDGET, WHATSAPP
- `LeadStatus` enum: PENDING, CONTACTED, CLOSED
- UUIDs generated with `@UuidGenerator` from Hibernate
- `@PrePersist` for automatic `createdAt` timestamps
- Never expose `apiKey` or `password` in response DTOs

## External Services

### Python RAG Microservice
- Base URL: `http://localhost:8000` (configured in `app.python-service-url`)
- Endpoints: `POST /chat`, `POST /process`, `DELETE /delete-document`
- Security: `X-Internal-Key` header 
- Request/Response uses snake_case JSON

### 360dialog WhatsApp API
- Base URL: `https://waba.360dialog.io/v1`
- Auth header: `D360-API-KEY: {apiKey}` (per bot config)
- Send message: `POST /messages`
- Send internal note: `POST /messages` with `type: note`
- Webhook verification: GET request with `hub.challenge` query param
- Webhook payload follows WhatsApp Cloud API format

### Gmail SMTP
- Configured via `spring.mail.*` in `application.yml`
- Used by `EmailService.sendLeadNotification()`

## application.yml Key Properties
```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: 86400000

app:
  upload-dir: uploads
  python-service-url: http://localhost:8000
  internal-api-key: ${INTERNAL_API_KEY}  # for X-Internal-Key header

spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${GMAIL_USERNAME}
    password: ${GMAIL_APP_PASSWORD}
```

## Security Configuration
- CSRF disabled, sessions stateless
- Public endpoints: `/api/auth/**`, `/webhook/whatsapp/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/** (for develop)`
- All other endpoints require valid JWT
- CORS configured to allow all origins (development)


## Chat Architecture
ChatService is a **pure orchestrator** — it calls Python RAG and returns all data.
Lead saving is the **caller's responsibility**, not ChatService's.

### Widget Flow (ChatController)
```
POST /api/chat
  → ChatController receives request
  → ChatService.sendMessage()
    → Save USER message to conversations
    → Call PythonRagClient.chat()
    → Save ASSISTANT message to conversations
    → Return ChatResponseDto { response, lead_captured, lead_data, request_detail, cede_control }
  → If lead_captured=true → ChatController calls LeadService.saveLead()
    → Round Robin adviser assignment → Email notification
  → Return { session_id, response } to frontend (no lead internals exposed)
```

### WhatsApp Flow (WhatsappService)
```
Webhook POST /webhook/whatsapp
  → WhatsappService.processIncomingMessage() (@Async)
  → Validate: text message, config exists, conversation is BOT_ACTIVE
  → ChatService.sendMessage()
  → Step 1: Send AI response to user via 360dialog sendMessage()
  → Step 2: If lead_captured=true → LeadService.saveLeadWhatsapp()
    → No Round Robin, assignedAdviserId=null, no email notifications
  → Step 3: If cede_control=true:
    → Update conversation status to PENDING_HUMAN
    → Send internal note via 360dialog sendInternalNote()
      (visual signal for adviser to take over)
```

## WhatsApp vs Widget Differences
| Aspect | Widget | WhatsApp |
|--------|--------|----------|
| Lead capture | Mandatory | Optional |
| Adviser assignment | Round Robin (one adviser) | No assignment (assignedAdviserId=null) |
| Adviser notification | Email via NotificationService | No email — 360dialog internal notes only |
| Adviser response | Outside (call/email) | Via 360dialog dashboard |
| Lead saving | ChatController → LeadService.saveLead() | WhatsappService → LeadService.saveLeadWhatsapp() |
| Conversation status | Not applicable | BOT_ACTIVE → PENDING_HUMAN → HUMAN_ACTIVE |
| cede_control handling | Not applicable | Updates status + sends internal note to 360dialog |

## What Needs to Be Finished
### High Priority
1. `WhatsappWebhookController` — `GET /webhook/whatsapp` (verification) + `POST /webhook/whatsapp`
2. `WhatsappConfigController` — CRUD endpoints for managing 360dialog config per bot

### Already Complete
- Auth, Tenant, User, Person, Bot, Document, Conversation, Chat
- Widget lead capture with Round Robin and email notifications (ChatController → LeadService.saveLead())
- WhatsApp lead capture without Round Robin (WhatsappService → LeadService.saveLeadWhatsapp())
- WhatsappService — sendMessage(), processIncomingMessage(), sendInternalNote()
- ConversationService.updateStatus() for WhatsApp status transitions
- ChatService refactored as pure orchestrator (no lead saving)
- Adviser management with notification channels
- Exception handling, CORS, JWT security

## Rules for Claude Code Sessions
- Read existing code in the relevant package BEFORE writing anything new
- Show the complete diff of every change before applying it
- One file at a time — never change multiple files simultaneously
- Follow existing conventions exactly — no new patterns or libraries
- If something is unclear, ask before assuming
- After each approved change, remind me to run the app and test before continuing
- Never expose secrets or API keys in code — always use @Value from application.yml