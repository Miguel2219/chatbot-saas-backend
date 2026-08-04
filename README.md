# chatbot-saas-backend

Spring Boot backend for a multi-tenant chatbot SaaS platform. It owns tenant
data, authentication, bot configuration, document management, conversations,
leads and usage quotas, and delegates all LLM work to a separate Python RAG
service.

Part of a four-service platform: this backend, a
[Python RAG microservice](https://github.com/Miguel2219/chatbot-service-rag),
an [Angular admin panel](https://github.com/Miguel2219/chatbot-panel), and an
embeddable Preact chat widget.

## Stack

- **Java** with **Spring Boot**
- **PostgreSQL** with **JPA / Hibernate**
- **Flyway** — versioned schema migrations
- **Spring Security** with **JWT** — access and refresh tokens
- **Cloudflare R2** — S3-compatible object storage for uploads
- **Resend** — transactional email
- **WhatsApp Cloud API** — inbound webhook and per-tenant configuration
- **springdoc / Swagger UI** — API documentation

## Architecture

Packages are organised by feature, not by layer. Each module owns its own
controllers, DTOs, entities, repositories and services:

```
com.chatbotsaas.chatbot_saas
├── auth/           # registration, login, refresh, password reset
├── bot/            # bot CRUD and system prompt management
├── chat/           # message handling, delegates to the RAG service
├── conversation/   # conversation history
├── dashboard/      # aggregated metrics
├── document/       # document upload and lifecycle
├── lead/           # captured leads and their status
├── module/         # application modules for permissions
├── permission/     # role-permission assignment
├── quota/          # subscription plans and usage cycles
├── role/           # roles
├── tenant/         # tenants
├── user/           # users and person data
├── whatsapp/       # WhatsApp config and inbound webhook
└── config/         # cross-cutting configuration
```

This keeps a feature's code in one place instead of spread across four
top-level folders, which is what makes the codebase navigable as the number of
modules grows.

## API

All endpoints live under the `/app` context path. Business endpoints are
prefixed with `/api` and protected by JWT; the WhatsApp webhook is public and
verified through Meta's signature mechanism instead.

| Module | Base path | What it covers |
|--------|-----------|----------------|
| Auth | `/api/auth` | Registration, login, token refresh, logout, change password, forgot/reset password |
| Bots | `/api/bot` | Bot CRUD, per-tenant listing, widget configuration, system prompt read and update |
| Chat | `/api/chat` | Inbound messages, routed to the RAG service |
| Conversations | `/api/conversations` | Conversation creation, listing, retrieval by session |
| Documents | `/api/documents` | Multipart upload per bot, listing, deletion |
| Leads | `/api/lead` | Lead listing and status updates |
| Dashboard | `/api/dashboard` | Aggregated summary metrics |
| Quota | `/api/quota` | Current usage, history, admin-wide view |
| Roles | `/api/roles` | Role listing and updates |
| Role permissions | `/api/role-permissions` | Permission assignment per role |
| Tenants | `/api/tenants` | Tenant CRUD and selector endpoints |
| Users | `/api/users` | User management |
| WhatsApp config | `/api/whatsapp-config` | Per-tenant WhatsApp credentials |
| WhatsApp webhook | `/webhook/whatsapp` | Inbound messages from Meta |

Interactive documentation is available at `/app/swagger-ui` when
`SPRINGDOC_ENABLED` is true. It is meant to be disabled in production so the API
surface is not publicly mapped, even though every endpoint stays behind JWT.

## Design notes

**Schema is owned by migrations, not by Hibernate.** `ddl-auto` is set to
`validate` and Flyway handles all DDL. Hibernate verifies that the entities
match the schema and refuses to start if they diverge, instead of silently
altering tables. Schema changes are reviewable files in `db/migration`.

**Refresh tokens are persisted, not just signed.** Access tokens last one hour,
refresh tokens seven days. Refresh tokens are stored as entities so they can be
revoked on logout rather than staying valid until expiry, and a scheduler cleans
up expired ones after a grace period.

**Password reset is rate limited.** Reset tokens expire after an hour, and a
tenant can request at most three resets per fifteen-minute window. Used and
expired tokens are cleaned up on a schedule.

**Uploads live in object storage.** Files were originally written to the
container filesystem, which meant every redeploy on Railway destroyed them. They
now go to Cloudflare R2: the backend uploads the file and passes the object key
to the RAG service, which reads it from the same bucket. Neither service holds
durable local state.

**LLM work is delegated, not embedded.** This backend never talks to a model
provider. It calls the Python RAG service over HTTP with a shared internal API
key, so the model, the embedding pipeline and the prompt strategy can change
without touching the data layer.

**Usage is metered per tenant.** Subscription plans, quota cycles and per-day
session counters are modelled explicitly, so consumption can be capped and
billed rather than tracked after the fact.

**Role-based access control.** Roles, permissions and application modules are
separate entities with an explicit join, so permissions can be assigned per role
per module instead of hardcoding role checks.

## Running locally

Requirements: JDK 17+, PostgreSQL, Maven.

```bash
cp .env.example .env      # fill in your own values
./mvnw spring-boot:run
```

Flyway applies migrations on startup. The API is available at
`http://localhost:8080/app`, Swagger UI at `http://localhost:8080/app/swagger-ui`.

## Configuration

All configuration comes from environment variables. Variables without a default
are intentionally fail-fast: the application will not start without them.

| Variable | Required | Description |
|----------|----------|-------------|
| `SPRING_DATASOURCE_URL` | no | JDBC URL (default local PostgreSQL) |
| `SPRING_DATASOURCE_USERNAME` | no | Database user (default `postgres`) |
| `SPRING_DATASOURCE_PASSWORD` | no | Database password (default `postgres`) |
| `JWT_SECRET` | yes | HMAC key for signing access tokens |
| `INTERNAL_API_KEY` | yes | Shared key for calls to the RAG service |
| `PYTHON_SERVICE_URL` | no | RAG service base URL (default `http://localhost:8000`) |
| `APP_FRONT_URL` | yes | Admin panel origin — used for CORS whitelist and links in emails |
| `RESEND_API_KEY` | yes | Transactional email credentials |
| `R2_ENDPOINT` | yes | Cloudflare R2 endpoint |
| `R2_BUCKET` | yes | Bucket name — must match the one used by the RAG service |
| `R2_ACCESS_KEY_ID` | yes | R2 access key |
| `R2_SECRET_ACCESS_KEY` | yes | R2 secret key |
| `R2_REGION` | no | Region (default `auto`) |
| `APP_WHATSAPP_VERIFY_TOKEN` | yes | Webhook verification token |
| `APP_WHATSAPP_APP_SECRET` | yes | Meta app secret for signature validation |
| `APP_WHATSAPP_GRAPH_API_VERSION` | no | Graph API version (default `v20.0`) |
| `ZOLVION_ADMIN_EMAIL` | yes | Administrator notification address |
| `SERVER_PORT` | no | HTTP port (default `8080`) |
| `JPA_SHOW_SQL` | no | Log generated SQL (default `false`) |
| `SPRINGDOC_ENABLED` | no | Expose Swagger UI and API docs (default `true`) |

## Notes

This backend was part of a larger platform I later stepped back from. Alongside
it I had built a custom admin panel, configuration layer and reporting — months
of work duplicating what existing tools like Chatwoot already solve. Recognizing
that led me to redesign the product around a leaner architecture, which became
Zolvion.