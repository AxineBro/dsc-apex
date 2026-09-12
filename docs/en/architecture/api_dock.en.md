# DSC Apex — API Documentation

## Overview

DSC Apex provides a conversational AI assistant for apartment selection and business process automation. The API is built on Spring Boot and integrates with GigaChat LLM for natural language processing.

## Base URL

```
http://localhost:8080
```

All chat endpoints are prefixed with `/api/v1/chat`.
Manager endpoints are `/api/v1/manager/tasks`.

## Swagger UI / OpenAPI

Interactive API documentation is available via **Swagger UI** (springdoc-openapi). It is generated automatically from controller and DTO annotations.

| URL | Description |
| :-- | :---------- |
| `/swagger-ui.html` | Swagger UI entry point (redirects to `/swagger-ui/index.html`) |
| `/swagger-ui/index.html` | Interactive API explorer (Try it out) |
| `/v3/api-docs` | OpenAPI 3 specification in JSON format |
| `/v3/api-docs.yaml` | OpenAPI 3 specification in YAML format |

**Swagger UI features:**

- View all endpoints (chat and manager tasks) with parameter and schema descriptions.
- Send real requests ("Try it out") directly from the browser.
- Automatic substitution of the `X-Session-Id` header (specified manually if needed).
- View DTO schemas: `ChatRequest`, `ChatResponse`, `ChatAttachment`, `SessionInitResponse`, `ManagerTaskDto`.
- View response codes and error format.

**Example access** (with the backend running locally):

```
http://localhost:8080/swagger-ui/index.html
```

> In production, Swagger UI may be disabled or protected — check with the DevOps team.

## Authentication

Currently, the API does not require authentication. Session management is handled via the `X-Session-Id` header.

## Common Headers

| Header         | Type   | Required | Description                                                      |
| :------------- | :----- | :------- | :--------------------------------------------------------------- |
| `X-Session-Id` | string | No       | Session identifier. If not provided, a new session is created.   |
| `Content-Type` | string | Yes      | Must be `application/json` for POST/PATCH requests.              |

## Error Handling

All errors are returned in a standardized format:

```json
{
  "timestamp": "2026-09-05T12:34:56.789Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation error description",
  "path": "/api/v1/chat/message"
}
```

### Common HTTP Status Codes

| Status Code | Description                                                      |
| :---------- | :--------------------------------------------------------------- |
| `200 OK`    | Request completed successfully.                                  |
| `400 Bad Request` | Invalid request or validation error.                      |
| `404 Not Found` | Resource not found (manager task, apartment, etc.).       |
| `409 Conflict` | Resource conflict (e.g., apartment already booked).       |
| `429 Too Many Requests` | Request or cycle limit exceeded.                  |
| `500 Internal Server Error` | Unexpected server error.                          |

---

## Endpoints — Chat

### 1. Initialize Session

Creates a new chat session and returns a unique identifier.

**Endpoint:** `GET /api/v1/chat/init`

**Request headers:** None required.

**Response (`SessionInitResponse`):**

| Field       | Type   | Description                                |
| :---------- | :----- | :----------------------------------------- |
| `sessionId` | string | UUID v4 identifier of the new session.     |

**Example request:**

```http
GET /api/v1/chat/init
```

**Example response (200 OK):**

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

### 2. Send Message

Sends a user message to the AI assistant and receives a reply. The session is created automatically if `X-Session-Id` is not provided.

**Endpoint:** `POST /api/v1/chat/message`

**Request headers:**

| Header         | Type   | Required | Description                                              |
| :------------- | :----- | :------- | :------------------------------------------------------- |
| `X-Session-Id` | string | No       | Session identifier. If omitted, a new session is created. |

**Request body (`ChatRequest`):**

| Field     | Type   | Required | Description                                          |
| :-------- | :----- | :------- | :--------------------------------------------------- |
| `message` | string | Yes      | User message. Max length: 2000 characters.           |

**Validation rules:**

- `message` must not be blank or contain only whitespace (`@NotBlank`).
- `message` must not exceed 2000 characters (`@Size`).

**Response (`ChatResponse`):**

| Field                  | Type    | Description                                                                 |
| :--------------------- | :------ | :-------------------------------------------------------------------------- |
| `reply`                | string  | AI assistant's response to the user's message.                              |
| `sessionId`            | string  | Session identifier (returned for the client to store).                      |
| `state`                | string  | Current session state code (`SessionState.getCode()`), or `null`.           |
| `transferredToManager` | boolean | `true` if the dialog was transferred to a live manager; otherwise `false`.   |
| `managerReason`        | string  | Reason for the transfer (only if `transferredToManager == true`), otherwise `null`. |
| `attachments`          | array   | List of `ChatAttachment` objects (never `null`, may be empty).              |

**`ChatAttachment` object:**

| Field   | Type   | Description                                                                 |
| :------ | :----- | :-------------------------------------------------------------------------- |
| `type`  | string | Attachment type. Currently `"complex_image"` — a residential complex image. |
| `url`   | string | Absolute URL of the media resource.                                         |
| `title` | string | Caption (e.g., residential complex name).                                   |

**Example request:**

```http
POST /api/v1/chat/message
X-Session-Id: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{
  "message": "Hello, I'm looking for a 2-room apartment in the city center with a budget of up to 15 million rubles"
}
```

**Example response (200 OK) — normal dialog:**

```json
{
  "reply": "Hello! I'll help you find an apartment. I'm checking available options in the center with 2 rooms and a budget up to 15,000,000 RUB. I found several interesting options. Would you like to see them?",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "state": "AWAITING_INPUT",
  "transferredToManager": false,
  "managerReason": null,
  "attachments": [
    {
      "type": "complex_image",
      "url": "https://dsk.vrn.ru/upload/iblock/e65/plan.jpg",
      "title": "Residential complex \"Lastochkino\""
    }
  ]
}
```

**Example response (200 OK) — transfer to manager:**

```json
{
  "reply": "Unfortunately, I couldn't find any suitable options. A specialist will contact you.",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "state": "TO_MANAGER",
  "transferredToManager": true,
  "managerReason": "limit_cycles",
  "attachments": []
}
```

**Example error response (400 Bad Request):**

```json
{
  "timestamp": "2026-09-05T12:34:56.789Z",
  "status": 400,
  "error": "Bad Request",
  "message": "message: Сообщение не может быть пустым",
  "path": "/api/v1/chat/message"
}
```

---

## Endpoints — Manager Tasks

Controller for tasks created when a chat session is escalated to a live manager.
Base path: `/api/v1/manager/tasks`.

### 1. List Manager Tasks

Returns a list of tasks sorted by creation date (newest first).

**Endpoint:** `GET /api/v1/manager/tasks`

**Query parameters:**

| Parameter | Type | Required | Default | Description                                                                     |
| :-------- | :--- | :------- | :------ | :------------------------------------------------------------------------------ |
| `limit`   | int  | No       | `50`    | Maximum number of tasks to return. Clamped to `Math.max(1, Math.min(limit, 200))`. |

**Response (`List<ManagerTaskDto>`):**

| Field           | Type              | Description                                        |
| :-------------- | :---------------- | :------------------------------------------------- |
| `id`            | long              | Unique task identifier.                            |
| `reason`        | string            | Reason for the dialog transfer.                    |
| `clientPhone`   | string            | Client's phone number (may be `null`).             |
| `status`        | string            | Task status (`NEW`, `IN_PROGRESS`, `DONE`, …).     |
| `createdAt`     | string (ISO-8601) | Date and time the task was created.                |
| `dialogHistory` | string            | Dialog history (JSON string).                      |

**Example request:**

```http
GET /api/v1/manager/tasks?limit=20
```

**Example response (200 OK):**

```json
[
  {
    "id": 42,
    "reason": "user_request",
    "clientPhone": "+79990000000",
    "status": "NEW",
    "createdAt": "2026-01-15T10:30:00",
    "dialogHistory": "[{\"role\":\"user\",\"text\":\"...\"}]"
  }
]
```

---

### 2. Update Task Status

**Endpoint:** `PATCH /api/v1/manager/tasks/{id}`

**Path parameters:**

| Parameter | Type | Description         |
| :-------- | :--- | :------------------ |
| `id`      | long | Task identifier.    |

**Request body:**

```json
{
  "status": "IN_PROGRESS"
}
```

If the `status` field is missing, the previous status is preserved.

**Responses:**

| Status          | Description                                   |
| :-------------- | :-------------------------------------------- |
| `200 OK`        | Updated `ManagerTaskDto` object.              |
| `404 Not Found` | Task with the specified `id` was not found.   |

**Example request:**

```http
PATCH /api/v1/manager/tasks/42
Content-Type: application/json

{ "status": "IN_PROGRESS" }
```

**Example response (200 OK):**

```json
{
  "id": 42,
  "reason": "user_request",
  "clientPhone": "+79990000000",
  "status": "IN_PROGRESS",
  "createdAt": "2026-01-15T10:30:00",
  "dialogHistory": "[{\"role\":\"user\",\"text\":\"...\"}]"
}
```

---

## Session Management

### Session Lifecycle

1. **Creation:** A session is created either via `GET /api/v1/chat/init` or automatically on the first `POST /api/v1/chat/message` without a session header.

2. **Active state:** The session remains active while the user interacts with the assistant. The `lastActivityAt` timestamp is updated on every request.

3. **Timeout (two-stage):**
   - If the session is inactive for `app.timeout.scheduler` minutes (default: 10), a reminder is sent and the status becomes `TIMEOUT_REMINDER`.
   - If the user remains inactive for another `app.timeout.reminder-wait-minutes` minutes (default: 5), the session is transferred to a manager.

4. **Transfer to manager:** When the assistant cannot handle a request (error, insufficient data, cycle limit, off-topic question, explicit request), the session is transferred to a human manager. The reason is stored in `managerReason` and in the `manager_tasks` table.

5. **Completion:** Sessions can reach the `FINISHED` state (successful scenario via `OFFER_READY`) or `TO_MANAGER`. Both states block further AI processing.

### Session States (`SessionState`)

| State Code           | Description                                                     |
| :------------------- | :-------------------------------------------------------------- |
| `NEW`                | Session created, no messages sent yet.                          |
| `AWAITING_INPUT`     | Waiting for user input (default active state).                  |
| `TIMEOUT_REMINDER`   | Reminder sent due to inactivity.                                |
| `SHOWING_LIST`       | Assistant is showing a list of apartments.                      |
| `OFFER_READY`        | Commercial offer is ready for the user.                         |
| `TO_MANAGER`         | Session transferred to a manager. Further AI processing disabled. |
| `FINISHED`           | Session completed. Further processing is impossible.            |

The state code is returned in the `state` field of the `ChatResponse` (see `SessionState#getCode()`).

---

## AI Assistant Features

The assistant (based on GigaChat) exposes the following tools (`GigaChatTools`):

| Tool                    | Description                                                                                |
| :---------------------- | :----------------------------------------------------------------------------------------- |
| `searchApartments`      | Search apartments by filters (area, floor, price, rooms, complex). Auto-expansion once.    |
| `countBookedMatches`    | Count unavailable apartments (booked/sold) by the same filters.                            |
| `getTopApartments`      | Return the first `app.flat.limit` apartments from the ranked list.                         |
| `rateApartments`        | AI-scoring of apartments by non-metric parameters (0–100).                                 |
| `getNextApartment`      | Return the next apartment from the ranked list without changing filters.                   |
| `generateOffer`         | Generate a commercial offer (with real-time apartment status check).                       |
| `askClarification`      | Ask a clarifying question (maximum 2 per session).                                         |
| `saveClientPhone`       | Save and normalize the client's phone to the `+7XXXXXXXXXX` format.                        |
| `resetSession`          | Reset all filters, counters and ranked list; session state → `NEW`.                        |
| `transferToManager`     | Transfer the dialog to a manager with a reason.                                            |

### Apartment Search Parameters

| Parameter  | Type    | Description                                   |
| :--------- | :------ | :-------------------------------------------- |
| `areaMin`  | number  | Minimum area in m².                           |
| `areaMax`  | number  | Maximum area in m².                           |
| `floor`    | integer | Desired floor.                                |
| `priceMin` | number  | Minimum price in rubles.                      |
| `priceMax` | number  | Maximum price in rubles.                      |
| `roomsMin` | integer | Minimum number of rooms.                      |
| `roomsMax` | integer | Maximum number of rooms.                      |
| `complex`  | string  | Residential complex name (substring search).  |

### Expanded Search

If no apartments match the criteria, the assistant automatically expands the search (once):

- **Area:** ±10%
- **Price:** ±10%
- **Rooms:** ±1
- **Floor:** ±2

### Scoring and Ranking

Apartments are scored by non-metric parameters using AI (range 0–100):

| Criterion        | Description                                       |
| :--------------- | :------------------------------------------------ |
| Window view      | Park, water, city, courtyard.                     |
| Parking          | Available or not.                                 |
| Floor            | Middle floors are scored higher.                  |
| Building type    | Monolith/brick higher than panel.                 |
| Metro proximity  | The closer, the higher the score.                 |

### Commercial Offer Generation

The assistant generates personalized commercial offers using templates. Available placeholders:

| Placeholder      | Description                                  |
| :--------------- | :------------------------------------------- |
| `{complexName}`  | Residential complex name.                    |
| `{address}`      | Full apartment address.                      |
| `{floor}`        | Floor number.                                |
| `{totalFloors}`  | Total number of floors in the building.      |
| `{area}`         | Area in m².                                  |
| `{rooms}`        | Number of rooms.                             |
| `{price}`        | Price in rubles.                             |
| `{viewType}`     | Window view.                                 |
| `{parking}`      | `"есть"` or `"нет"`.                         |

### Manager Escalation Reasons

| Reason                 | Description                                                       |
| :--------------------- | :---------------------------------------------------------------- |
| `error_*`              | Technical error during processing (e.g., `error_TimeoutException`). |
| `limit_cycles`         | Maximum number of search cycles exceeded (`app.max.cycles`, default 5). |
| `too_many_questions`   | More than 2 clarifying questions asked.                           |
| `booked`               | Suitable apartments exist but are already booked/sold.            |
| `no_phone`             | Client refused to provide a phone number, and there is not enough data to continue. |
| `user_request`         | User explicitly requested a manager.                              |
| `timeout`              | User inactive after a reminder.                                   |
| `unknown`              | Fallback if the reason was not set.                               |

---

## Performance Monitoring

### Slow Request Thresholds

| Operation         | Property                                   | Default |
| :---------------- | :----------------------------------------- | :------ |
| Chat message      | `app.logging.slow-threshold-ms`            | `60000` |
| Batch scoring     | `app.logging.slow-scoring-threshold-ms`    | `20000` |

Additional fixed thresholds:

| Operation         | Threshold | Action                                   |
| :---------------- | :-------- | :--------------------------------------- |
| Session save      | 500 ms    | WARN log if exceeded.                    |

### Correlation ID and Session ID

- A `correlationId` (UUID) is generated per request and placed in the MDC for end-to-end tracing.
- `sessionId` is also propagated through the MDC at all layers of the application.

---

## Limits and Security

### Cycle Limits

| Parameter                    | Default | Property             |
| :--------------------------- | :------ | :------------------- |
| `app.max.cycles`             | 5       | `app.max.cycles`     |
| `app.flat.limit`             | 5       | `app.flat.limit`     |
| Max clarifying questions     | 2       | hardcoded            |
| `app.batch.size` (scoring)   | 5       | `app.batch.size`     |

### Data Validation

| Field       | Validation Rule                                       |
| :---------- | :---------------------------------------------------- |
| `message`   | Required, not blank, max 2000 characters.             |
| `sessionId` | Must be a valid UUID (if absent, a new session is created). |

---

## Environment Variables and Configuration

### GigaChat

| Variable                | Description                              | Default / example     |
| :---------------------- | :--------------------------------------- | :-------------------- |
| `GIGA_CHAT_API_KEY`     | GigaChat API key.                        | (user-provided)       |
| `GIGA_CHAT_SCOPE`       | Scope for GigaChat.                      | `GIGACHAT_API_PERS`   |
| `GIGA_CHAT_MODEL`       | GigaChat model.                          | `GigaChat-Pro`        |
| `GIGA_CHAT_UNSAFE_SSL`  | Allow unsafe SSL certificates.           | `true`                |

### Database

| Variable                       | Description                    | Default                                     |
| :----------------------------- | :----------------------------- | :------------------------------------------ |
| `SPRING_DATASOURCE_URL`        | JDBC URL for PostgreSQL.       | `jdbc:postgresql://postgres:5432/agentdb`   |
| `SPRING_DATASOURCE_USERNAME`   | Database username.             | `postgres`                                  |
| `SPRING_DATASOURCE_PASSWORD`   | Database password.             | `postgres`                                  |
| `DB_DDL_AUTO`                  | Hibernate DDL strategy.        | `update`                                    |
| `DB_SHOW_SQL`                  | Show SQL queries in logs.      | `true`                                      |
| `DB_FORMAT_SQL`                | Format SQL in logs.            | `true`                                      |

### Application Properties

| Property                                              | Description                                                | Default                          |
| :---------------------------------------------------- | :--------------------------------------------------------- | :------------------------------- |
| `app.max.cycles`                                      | Maximum number of search cycles.                           | `5`                              |
| `app.flat.limit`                                      | Number of apartments shown per view.                       | `5`                              |
| `app.batch.size`                                      | Batch size for apartment scoring.                          | `5`                              |
| `app.logging.slow-threshold-ms`                       | Slow chat request threshold (ms).                          | `60000`                          |
| `app.logging.slow-scoring-threshold-ms`               | Slow scoring batch threshold (ms).                         | `20000`                          |
| `app.timeout.scheduler`                               | Minutes of inactivity before the first reminder.           | `10`                             |
| `app.timeout.reminder-wait-minutes`                   | Minutes to wait after the reminder before escalation.      | `5`                              |
| `app.data.apartments`                                 | Classpath path to the apartments JSON file.                | `data/apartments.json`           |
| `app.data.complexes`                                  | Classpath path to the residential complexes JSON catalog.  | `data/complexes.json`            |
| `app.prompt.path`                                     | Classpath path to the system prompt file.                  | `prompts/system_instruction.txt` |
| `app.resource.path`                                   | Classpath path to the commercial offer template.           | `templates/offer_template.txt`   |

### Logging

| Property                 | Description                    | Default |
| :----------------------- | :----------------------------- | :------ |
| `LOG_LEVEL_SPRING_AI`    | Log level for Spring AI.       | `DEBUG` |
| `LOG_LEVEL_GIGA`         | Log level for GigaChat.        | `DEBUG` |

---

## Logging and Monitoring

### Log Levels

| Level   | Description                                                    |
| :------ | :------------------------------------------------------------- |
| `ERROR` | Critical errors requiring immediate attention.                 |
| `WARN`  | Warnings (slow requests, timeouts, escalations).               |
| `INFO`  | Key business events (session creation, message processing).    |
| `DEBUG` | Detailed execution flow (filters, scoring, API calls).         |
| `TRACE` | Full request/response bodies (scoring responses).              |

### Correlation ID

A `correlationId` is generated for each request and added to the MDC for tracing:

```xml
<correlationId>550e8400-e29b-41d4-a716-446655440000</correlationId>
```

This provides end-to-end tracing across logs. `sessionId` is also propagated through the MDC at all layers.

---

## Testing

### Local Development

```bash
# Run backend tests
mvn test

# Run integration tests with Testcontainers
mvn verify

# Start the full stack via Docker Compose
docker-compose up -d

# Run frontend tests (Playwright)
npm run test:e2e
```

### Test Data

The application includes a sample apartment dataset in `src/main/resources/data/apartments.json` and a residential complex catalog in `src/main/resources/data/complexes.json`.

---

## API Versioning

The API uses versioned endpoints:

- Current version: `v1`
- Chat: `/api/v1/chat`
- Manager tasks: `/api/v1/manager/tasks`
- Future versions: `/api/v2/chat` (backward compatible)

---

## Rate Limiting and Throttling

Although the application currently does not implement rate limiting, the following limits apply:

1. **Cycle limit:** 5 search cycles per session (configurable via `app.max.cycles`).
2. **Clarification limit:** 2 clarifying questions per session (hardcoded in code).
3. **Timeout:** Sessions automatically expire after `app.timeout.scheduler` minutes of inactivity (default 10), after which a reminder is sent. If the user does not respond for another `app.timeout.reminder-wait-minutes` minutes (default 5), the session is transferred to a manager.

For production, it is recommended to add:

- Rate limiting via an API Gateway.
- A backend rate limiter (Resilience4j).

---

## Developer Notes

### Architectural Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller Layer                         │
│   (ChatController, ManagerTaskController, DTO)              │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Application Layer                        │
│   (ChatFacade, AgentOrchestrator, SessionManager, Tools)    │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Domain Layer                             │
│        (Session, Apartment, Filters, Services)              │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                 Infrastructure Layer                        │
│  (GigaChatClient, Repository, Mappers, Notifications,       │
│   ComplexCatalog, JsonApartmentSearchService)               │
└─────────────────────────────────────────────────────────────┘
```

### Key Design Patterns

| Pattern         | Implementation                                                |
| :-------------- | :------------------------------------------------------------ |
| **Facade**      | `ChatFacade` orchestrates session management and AI calls.   |
| **Orchestrator**| `AgentOrchestrator` coordinates AI processing.               |
| **Repository**  | Spring Data JPA for data access.                             |
| **Mapper**      | `SessionMapper` and `ManagerTaskMapper` for DTO↔Entity.      |
| **ThreadLocal** | `SessionManager` uses ThreadLocal for request-scoped sessions. |
| **Strategy**    | Different search strategies (normal, expanded).              |

### Resilience Mechanisms

The application uses `@EnableResilientMethods` (Spring Resilience4j) for:

- **Retry:** Automatic retry on transient failures.
- **Circuit Breaker:** Prevention of cascading failures.
- **Time Limiter:** Protection against timeouts in external calls.

---

## Troubleshooting

### Common Issues

| Issue                                | Solution                                                       |
| :----------------------------------- | :------------------------------------------------------------- |
| Session not found                    | Create a new session via `/api/v1/chat/init`.                 |
| Timeout on AI calls                  | Check the GigaChat API key and network connectivity.          |
| Empty apartment search results       | Check the `app.data.apartments` path and JSON format.         |
| No residential complex images in attachments | Check `app.data.complexes` and residential complex name normalization. |
| Received reason `limit_cycles`       | `app.max.cycles` reached. Transfer to a manager or reset the session. |
| Database connection errors           | Ensure PostgreSQL is running and the connection string is correct. |
| Swagger UI unavailable               | Ensure the backend is running and springdoc-openapi is not disabled in the profile. |

### Debugging

To enable DEBUG logging for a specific package:

```yaml
logging:
  level:
    com.hackathon.agent: DEBUG
    org.springframework.ai: DEBUG
```

---

## License

This project is distributed under the [Apache License 2.0](../../../LICENSE).

---

## Contributing

1. Fork the repository.
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request.

---

## Support

For questions and support:

- **Team Lead:** Alexey — [GitHub](https://github.com/AxineBro)
- **Frontend:** Ivan — [GitHub](https://github.com/Onizuk0)
- **DevOps:** Sergei — [GitHub](https://github.com/SergeyTerpugov)