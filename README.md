[![Русский](https://img.shields.io/badge/Русский-Документация-blue?style=flat-square)](docs/ru/README.ru.md)
[![License](https://img.shields.io/badge/License-Apache2.0-green?style=flat-square)](LICENSE)

# Dsc Apex

AI assistant for business process automation using GigaChat LLM and external APIs

## Technology Stack

| Layer                | Technologies                                                     |
| :------------------- | :--------------------------------------------------------------- |
| **Frontend**         | `React 19` · `Vite`                                              |
| **Backend**          | `Java 17` · `Spring Boot 4.x`                                    |
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
