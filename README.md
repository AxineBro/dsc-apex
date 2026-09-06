[![Русский](https://img.shields.io/badge/Русский-Документация-blue?style=flat-square)](docs/ru/README.ru.md)
[![License](https://img.shields.io/badge/License-Apache2.0-green?style=flat-square)](LICENSE)

# Dsc Apex

AI assistant for business process automation using GigaChat LLM and external APIs

## Technology Stack

| Layer                | Technologies                                                     |
| :------------------- | :--------------------------------------------------------------- |
| **Frontend**         | `React 19` · `Vite` · `vite-plugin-svgr` · `vite-plugin-css-injected-by-js` |
| **Backend**          | `Java 25` · `Spring Boot 4.x`                                    |
| **Database**         | `PostgreSQL 15`                                                  |
| **Web Server**       | `Nginx`                                                          |
| **Containerization** | `Docker` · `Docker Compose`                                      |
| **Build & CI/CD**    | `Maven` · `GitHub Actions`                                       |
| **Testing**          | `JUnit 5` · `Testcontainers` (backend) / `Playwright` (frontend) |

## Team

- **Alexey** — *Team Lead / Backend Developer*  
  System Architect · Business Analyst · UX/UI Designer  
  [GitHub](https://github.com/AxineBro)

- **Ivan** — *Frontend Developer*  
  Business Analyst · UX/UI Designer  
  [GitHub](https://github.com/Onizuk0)

- **Sergei** — *DevOps / QA Engineer*  
  Business Analyst · UX/UI Designer  
  [GitHub](https://github.com/SergeyTerpugov)

## Documentation and architecture
- [The agent's work plan](docs/en/architecture/behavior-flow.en.md) — detailed flowchart of the AI assistant’s logic
- [API Documentation](docs/en/architecture/api_dock.en.md) — complete API reference with endpoints, session management, and configuration.
- [File structure](docs/en/architecture/files_tree.en.md) — detailed description of the backend file structure.

## Frontend Widget Overview

Embeddable AI-consultant for DSK site. Built with `Vite` as a single `widget.js`, mounted via `iframe` with style isolation.

| Mode | Behavior |
| :--- | :--- |
| `closed` | 60px round button, `iframe` 76x76 |
| `floating` | 400x680 card, header + history + input, quick chips |
| `full` | modal with 24px offset + dim, `SideMenu` 280px + chat, welcome-screen with 4 cards |
| `mobile <=640px` | `floating` opens as `full`, no offsets, `100dvh`, sidebar as fullscreen fade overlay |

Key features:
- `GET /api/v1/chat/init` + `POST /api/v1/chat/message` with `X-Session-Id`, `sid` stored in `localStorage` as `dsk_sid`
- Suggestions system: 20 prompts in 4 groups (`pick`, `mortgage`, `build`, `docs`), 1 random per group
- Thinking indicator (9 rotating statuses, 4.5-5.5s), `send` button locked while loading
- Edit last user message / regenerate, copy, markdown tables, `sys-info` pill for manager transfer
- Parent <-> iframe sync via `postMessage({ type: 'CHAT_MODE_CHANGE', mode })`, body scroll lock in `full`

## Widget Integration (1 line demo)

```html
<script src="/widget.js"></script>
```

The script creates `iframe.dsk-chat-iframe` and mounts `ChatWidget` inside. No global CSS leaks.

## Repository Layout

```text
dsc-apex/
  frontend/
    index.html                  # DSK homepage copy + <script src="./widget.js">
    public/CopyAssetsDSK/       # copy assets (bitrix, upload, css, img)
    src/main.tsx                # widget entry, iframe mount
    src/Components/             # ChatWidget, ChatLayout, ChatWindow, SideMenu, Suggestions
    src/api/chatApi.ts          # initSession + sendMessage
    dist/                       # build output, NOT in git
      widget.js
      index.html
      CopyAssetsDSK/
  backend/
    src/main/java/...           # ChatController, ChatFacade, AgentOrchestrator
    src/main/resources/         # prompts/, templates/, data/apartments.json
  nginx/nginx.conf              # / -> index.html, /api/ -> backend:8080
  docker-compose.yml
  .env                          # NOT in git
```

## Quick Start

### 1. Frontend dev (UI + live backend)

```bash
cd frontend
npm ci
npm run dev
# open http://localhost:5173
```

`vite.config.ts` already proxies `/api` to `http://localhost:8080` for dev. Prod uses relative `/api/...` via `nginx`, no rebuild needed.

### 2. Backend dev

```bash
docker compose up db -d
cd backend
export GIGA_CHAT_API_KEY='xxx'
export GIGA_CHAT_SCOPE='GIGACHAT_API_PERS'
export GIGA_CHAT_MODEL='GigaChat'
export DB_URL=jdbc:postgresql://localhost:5432/agentdb
export DB_USERNAME=postgres
export DB_PASSWORD=secret
./mvnw spring-boot:run
# check: curl http://localhost:8080/api/v1/chat/init
```

### 3. Full stack via Docker (demo)

```bash
cd frontend && npm ci && npm run build
cd ..
docker compose up db backend nginx -d --build
# open http://localhost:80/
# check: curl http://localhost:80/api/v1/chat/init
```

`nginx` serves `frontend/dist/index.html` and proxies `/api/*` to `backend:8080`, so no `CORS` needed.

<details>
<summary><b>Environment variables</b></summary>

| Variable | Default | Description |
| :--- | :--- | :--- |
| `GIGA_CHAT_API_KEY` | — | `Authorization key` (base64) from SaluteAI, required |
| `GIGA_CHAT_SCOPE` | `GIGACHAT_API_PERS` | must match key type |
| `GIGA_CHAT_MODEL` | `GigaChat` | use base model for free quota, `GigaChat-Pro` is paid |
| `DB_URL` | `jdbc:postgresql://localhost:5432/agentdb` | `db:5432` inside compose |
| `DB_USERNAME` / `DB_PASS` | `postgres` / `secret` | note `DB_PASS` in compose vs `DB_PASSWORD` for `mvn` |
| `APP_MAX_CYCLES` | `5` | search loops |
| `APP_FLAT_LIMIT` | `5` | top apartments |

See full list in `docs/en/architecture/api_dock.en.md`.

</details>

<details>
<summary><b>Build notes & troubleshooting</b></summary>

- Build is `tsc -b && vite build` as `IIFE` lib to single `widget.js`. `React` is bundled inside, `assetsInlineLimit` inlines logos.
- `public/*` is copied to `dist/*` automatically. `index.html` is copied via `cp index.html dist/index.html` script because `lib` mode ignores it.
- Never commit `dist/`, `node_modules/`, `.env`.
- `file://` preview never works for DSK copy (`BX`, `$`, `CORS`). Use `npx serve dist -l 4173`, not double-click.
- `process is not defined` in `widget.js` — fixed via `define: { 'process.env.NODE_ENV': '"production"' }` in `vite.config.ts`.
- `402 Payment Required` from GigaChat — switch `GIGA_CHAT_MODEL` to `GigaChat`, check model activation in SaluteAI cabinet.
- `Row was already updated (optimistic lock)` — fixed in `ChatFacade` by best-effort second `save`, see branches `fix/lombok-jdk25`, `fix/session-double-save`.
- `value too long for type character varying(20)` on `manager_tasks` — extend `client_phone` to `varchar(50)` and sanitize placeholder phones.

</details>

## Architecture Overview (Backend)

The backend follows a layered architecture that separates concerns and promotes maintainability:

| Layer            | Responsibility                                                                 |
| :--------------- | :----------------------------------------------------------------------------- |
| **Controller**   | REST endpoints, request validation, session header handling, response formatting. |
| **Application**  | Facade (`ChatFacade`), orchestrator (`AgentOrchestrator`), session manager (`SessionManager`). |
| **Domain**       | Core business logic: session state, filters, scoring, apartment search, offer generation. |
| **Infrastructure** | External integrations (GigaChat client, JSON data source, JPA repositories, notification service). |

### Key Design Patterns

- **Facade** – simplifies the controller layer by delegating to orchestrators.
- **Orchestrator** – coordinates the AI processing pipeline (history, prompt, tools, response).
- **Repository** – Spring Data JPA for database access.
- **Mapper** – converts between domain models and JPA entities.
- **ThreadLocal** – provides request-scoped session context without explicit parameter passing.
- **Strategy** – normal vs. expanded search strategies

### Resilience & Fault Tolerance

The backend uses **Spring Resilience4j** (enabled via `@EnableResilientMethods`) to:

- Automatically **retry** transient failures (network timeouts, temporary unavailability of GigaChat).
- **Circuit break** external calls to prevent cascading failures.
- **Time‑limit** long-running operations to avoid thread starvation.

All critical operations (session creation, message processing, scoring) are annotated with `@Transactional` to guarantee atomicity and data consistency.

### Logging & Observability

Each request is assigned a **correlation ID** (UUID) that is propagated through the entire call chain using **MDC** (Mapped Diagnostic Context). This enables:

- End‑to‑end tracing across logs.
- Easy debugging of user sessions.
- Performance monitoring with **slow request thresholds** (configurable via environment variables).

All significant events (session creation, AI calls, escalations, errors) are logged at appropriate levels (`INFO`, `WARN`, `DEBUG`). The logging strategy also serves as an **audit trail** for security and compliance purposes.

## Security

While the current version does not enforce authentication (planned for future releases), the system incorporates several security‑conscious design choices:

| Measure | Benefit |
| :------ | :------ |
| **Stateless session tokens** (UUIDs) | Easy to rotate, no sensitive data embedded. |
| **Input validation** (Jakarta Validation) | Prevents injection attacks and malformed payloads. |
| **Structured logging with correlation IDs** | Enables forensic analysis and monitoring of suspicious activity. |
| **Optimistic locking** (JPA `@Version`) | Prevents concurrent session corruption. |
| **Transaction boundaries** | Ensures data integrity even in error scenarios. |
| **Resilience patterns** | Mitigates DoS‑like effects from external service failures. |

We treat **logging as a security control** – all operations that affect user data or state changes are recorded with sufficient context (session ID, IP, user agent, duration). This allows operators to detect anomalies, investigate incidents, and meet audit requirements.

In future iterations we plan to add:

- **OAuth2 / JWT** authentication.
- **Rate limiting** (per session or per IP).
- **Sensitive data masking** in logs.
- **TLS/SSL** enforcement.

## License

This project is distributed under a license [Apache License 2.0](LICENSE).
```
