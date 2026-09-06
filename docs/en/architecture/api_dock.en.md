# DSC Apex — API Documentation

## Overview

DSC Apex provides a conversational AI assistant for apartment selection and business process automation. The API is built with Spring Boot and integrates with GigaChat LLM for natural language processing.

## Base URL

```
http://localhost:8080
```

All API endpoints are prefixed with `/api/v1/chat`.

## Authentication

Currently, the API does not require authentication. Session management is handled via the `X-Session-Id` header.

## Common Headers

| Header         | Type   | Required | Description                                                      |
| :------------- | :----- | :------- | :--------------------------------------------------------------- |
| `X-Session-Id` | string | No       | Session identifier. If not provided, a new session is created.   |
| `Content-Type` | string | Yes      | Must be `application/json` for POST requests.                    |

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
| `200 OK`    | Request successful                                              |
| `400 Bad Request` | Invalid request payload or validation error          |
| `404 Not Found` | Resource not found (e.g., session, apartment)            |
| `409 Conflict` | Resource conflict (e.g., apartment already booked)         |
| `429 Too Many Requests` | Rate limit exceeded or too many cycles               |
| `500 Internal Server Error` | Unexpected server error                            |

---

## Endpoints

### 1. Initialize Session

Creates a new chat session and returns a unique session identifier.

**Endpoint:** `GET /api/v1/chat/init`

**Request Headers:** None required.

**Response:**

| Field       | Type   | Description                                |
| :---------- | :----- | :----------------------------------------- |
| `sessionId` | string | UUID v4 identifier for the new session.    |

**Example Request:**

```http
GET /api/v1/chat/init
```

**Example Response (200 OK):**

```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

### 2. Send Message

Sends a user message to the AI assistant and receives a reply. The session is automatically created if `X-Session-Id` is not provided.

**Endpoint:** `POST /api/v1/chat/message`

**Request Headers:**

| Header         | Type   | Required | Description                                      |
| :------------- | :----- | :------- | :----------------------------------------------- |
| `X-Session-Id` | string | No       | Session identifier. If omitted, a new session is created. |

**Request Body:**

| Field     | Type   | Required | Description                                         |
| :-------- | :----- | :------- | :-------------------------------------------------- |
| `message` | string | Yes      | User message. Max length: 2000 characters.         |

**Validation Rules:**

- `message` must not be empty or contain only whitespace.
- `message` must not exceed 2000 characters.

**Response:**

| Field       | Type   | Description                                         |
| :---------- | :----- | :-------------------------------------------------- |
| `reply`     | string | AI-generated response to the user's message.        |
| `sessionId` | string | Session identifier (returned for client to store).  |

**Example Request:**

```http
POST /api/v1/chat/message
X-Session-Id: 550e8400-e29b-41d4-a716-446655440000
Content-Type: application/json

{
  "message": "Hello, I'm looking for a 2-room apartment in the city center with a budget of up to 15 million rubles"
}
```

**Example Response (200 OK):**

```json
{
  "reply": "Hello! I'll help you find a suitable apartment. Let me check available options in the city center with 2 rooms and budget up to 15,000,000 RUB. I found several interesting options. Would you like to see them?",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Example Error Response (400 Bad Request):**

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

## Session Management

### Session Lifecycle

1. **Creation:** A session is created either via `GET /api/v1/chat/init` or automatically on the first `POST /api/v1/chat/message` without a session header.

2. **Active State:** The session remains active while the user interacts with the assistant. The `lastActivityAt` timestamp is updated on each request.

3. **Timeout:** If a session is inactive for the configured timeout period (default: 10 minutes), the system sends a reminder. If the user remains inactive for an additional wait period (default: 5 minutes), the session is transferred to a manager.

4. **Transfer to Manager:** When the assistant cannot handle a request (error, insufficient data, complex case), the session is transferred to a human manager. The client receives a notification that a specialist will contact them.

5. **Completion:** Sessions can be completed by the AI or transferred to manager. Completed sessions cannot process further messages.

### Session States

| State              | Description                                                         |
| :----------------- | :------------------------------------------------------------------ |
| `NEW`              | Session created, no messages sent yet.                             |
| `AWAITING_INPUT`   | Waiting for user input (default active state).                     |
| `TIMEOUT_REMINDER` | Reminder sent due to inactivity.                                   |
| `SHOWING_LIST`     | Showing apartment list to the user.                                |
| `OFFER_READY`      | Commercial offer ready for the user.                               |
| `TO_MANAGER`       | Session transferred to manager. No further AI processing allowed.  |
| `FINISHED`         | Session completed. No further processing allowed.                  |

---

## AI Assistant Features

The assistant (powered by GigaChat) can perform the following operations:

### 1. Apartment Search

The assistant can search for apartments based on user preferences:

| Parameter     | Type    | Description                                   |
| :------------ | :------ | :-------------------------------------------- |
| `areaMin`     | number  | Minimum area in square meters.                |
| `areaMax`     | number  | Maximum area in square meters.                |
| `floor`       | integer | Desired floor number.                         |
| `priceMin`    | number  | Minimum price in RUB.                         |
| `priceMax`    | number  | Maximum price in RUB.                         |
| `roomsMin`    | integer | Minimum number of rooms.                      |
| `roomsMax`    | integer | Maximum number of rooms.                      |

### 2. Expanded Search

If no apartments match the criteria, the assistant automatically expands the search:

- **Area:** ±10%
- **Price:** ±10%
- **Rooms:** ±1
- **Floor:** ±2

### 3. Scoring and Ranking

Apartments are scored based on non-metric parameters using AI:

| Criterion        | Description                                       |
| :--------------- | :------------------------------------------------ |
| View type        | Park, water view, city view, yard view.          |
| Parking          | Available or not available.                      |
| Floor            | Middle floors score higher.                      |
| Building type    | Monolith/brick scores higher than panel.         |
| Metro proximity  | Closer metro = higher score.                     |

### 4. Commercial Offer Generation

The assistant generates personalized commercial offers using templates:

**Available Placeholders:**

| Placeholder      | Description                                       |
| :--------------- | :------------------------------------------------ |
| `{complexName}`  | Residential complex name.                        |
| `{address}`      | Full apartment address.                          |
| `{floor}`        | Floor number.                                    |
| `{totalFloors}`  | Total floors in building.                        |
| `{area}`         | Area in square meters.                           |
| `{rooms}`        | Number of rooms.                                 |
| `{price}`        | Price in RUB.                                    |
| `{viewType}`     | View from windows.                               |
| `{parking}`      | "есть" (yes) or "нет" (no).                      |

### 5. Manager Escalation

The assistant transfers sessions to managers in the following cases:

| Reason                   | Description                                              |
| :----------------------- | :------------------------------------------------------- |
| `error_*`                | Technical error during processing.                      |
| `limit_cycles`           | Maximum search cycles exceeded (default: 5).           |
| `too_many_questions`     | More than 2 clarification questions asked.             |
| `user_requested`         | User explicitly requested a manager.                   |
| `no_apartments_found`    | No matching apartments found.                          |
| `timeout`                | User inactive after reminder.                          |

---

## Performance Monitoring

All endpoints include performance logging:

### Request Duration Headers

Each request logs:

- **Duration in milliseconds**
- **Correlation ID** (generated per request for tracing)
- **Session ID**
- **Client IP and User-Agent** (for audit)

### Slow Request Thresholds

| Operation          | Threshold | Action                                   |
| :----------------- | :-------- | :--------------------------------------- |
| Chat message       | 60000 ms  | WARN log if exceeded                     |
| Scoring batch      | 20000 ms  | WARN log if exceeded                     |
| Session save       | 500 ms    | WARN log if exceeded                     |

---

## Rate Limiting & Security

### Cycle Limits

| Parameter               | Default | Description                                       |
| :---------------------- | :------ | :------------------------------------------------ |
| `app.max.cycles`        | 5       | Maximum search refinement cycles.                |
| `app.flat.limit`        | 5       | Number of apartments to show at once.            |
| Max clarification questions | 2   | Maximum follow-up questions before escalation.   |

### Data Validation

| Field               | Validation Rule                                      |
| :------------------ | :--------------------------------------------------- |
| `message`           | Required, max 2000 characters                       |
| `sessionId`         | Must be a valid UUID if provided                   |

---

## Environment Variables & Configuration

### Required Configuration

| Variable                       | Description                                     | Default / Example          |
| :----------------------------- | :---------------------------------------------- | :------------------------- |
| `GIGA_CHAT_API_KEY`            | API key for GigaChat.                          | (user-provided)            |
| `GIGA_CHAT_SCOPE`              | Scope for GigaChat.                            | `GIGACHAT_API_PERS`        |
| `GIGA_CHAT_MODEL`              | GigaChat model.                                | `GigaChat-Pro`             |
| `GIGA_CHAT_UNSAFE_SSL`         | Allow unsafe SSL certificates.                 | `true`                     |
| `DB_URL`                       | JDBC URL for PostgreSQL.                       | `jdbc:postgresql://postgres:5432/agentdb` |
| `DB_USERNAME`                  | Database username.                             | `postgres`                 |
| `DB_PASSWORD`                  | Database password.                             | `postgres`                 |
| `DB_DDL_AUTO`                  | Hibernate DDL strategy.                        | `update`                   |
| `DB_SHOW_SQL`                  | Show SQL queries in logs.                      | `true`                     |
| `DB_FORMAT_SQL`                | Format SQL in logs.                            | `true`                     |
| `APP_MAX_CYCLES`               | Maximum search cycles.                         | `5`                        |
| `APP_BATCH_SIZE`               | Batch size for apartment scoring.              | `5`                        |
| `APP_FLAT_LIMIT`               | Number of apartments to show at once.          | `5`                        |
| `APP_SLOW_THRESHOLD_MS`        | Slow request threshold (chat).                 | `60000` (60 s)             |
| `APP_SLOW_SCORING_THRESHOLD_MS`| Slow request threshold (scoring).              | `20000` (20 s)             |
| `APP_TIMEOUT_REMINDER_WAIT_MINUTES` | Minutes to wait after reminder before escalation. | `5`) |
| `APP_TIMEOUT_SCHEDULER`        | Minutes of inactivity before first reminder.   | `10`                       |
| `APP_DATA_APARTMENTS`          | Path to apartment data JSON.                   | `data/apartments.json`     |
| `APP_OFFER_TEMPLATE`           | Path to offer template.                        | `templates/offer_template.txt` |
| `APP_SYSTEM_PROMPT`            | Path to system prompt file.                    | `prompts/system_instruction.txt` |
| `LOG_LEVEL_SPRING_AI`          | Log level for Spring AI.                       | `DEBUG`                    |
| `LOG_LEVEL_GIGA`               | Log level for GigaChat.                        | `DEBUG`                    |

### Database Configuration

The application uses PostgreSQL with JPA/Hibernate:

| Variable          | Description                     |
| :---------------- | :------------------------------ |
| `SPRING_DATASOURCE_URL` | JDBC URL for PostgreSQL      |
| `SPRING_DATASOURCE_USERNAME` | Database username          |
| `SPRING_DATASOURCE_PASSWORD` | Database password          |

---

## Logging & Monitoring

### Log Levels

| Level   | Output Description                                        |
| :------ | :-------------------------------------------------------- |
| `ERROR` | Critical errors requiring immediate attention.            |
| `WARN`  | Warning conditions (slow requests, timeouts, escalations).|
| `INFO`  | Key business events (session creation, message processing).|
| `DEBUG` | Detailed flow information (filters, scoring, API calls).  |
| `TRACE` | Full request/response bodies (scoring responses).         |

### Correlation ID

A `correlationId` is generated for each request and added to MDC for traceability:

```xml
<correlationId>550e8400-e29b-41d4-a716-446655440000</correlationId>
```

This enables end-to-end tracing across logs.

---

## Testing

### Local Development

```bash
# Run backend tests
mvn test

# Run integration tests with Testcontainers
mvn verify

# Start entire stack with Docker Compose
docker-compose up -d

# Run frontend tests (Playwright)
npm run test:e2e
```

### Test Data

The application includes a sample apartment dataset in `src/main/resources/data/apartments.json` with 10+ apartments for testing.

---

## API Versioning

The API uses versioned endpoints:

- Current version: `v1`
- Base path: `/api/v1/chat`
- Future versions: `/api/v2/chat` (backward-compatible)

---

## Rate Limiting & Throttling

While the application does not currently implement rate limiting, the following constraints apply:

1. **Cycle Limit:** 5 search cycles per session (configurable via `APP_MAX_CYCLES`).
2. **Clarification Limit:** 2 clarification questions per session (hardcoded).
3. **Timeout:** Sessions automatically expire after 10 minutes of inactivity (configurable via `APP_TIMEOUT_SCHEDULER`), after which a reminder is sent. If the user does not respond within an additional 5 minutes (`APP_TIMEOUT_REMINDER_WAIT_MINUTES`), the session is transferred to a manager.

For production deployments, consider adding:

- API Gateway rate limiting.
- Backend rate limiter (Resilience4j).

---

## Development Notes

### Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller Layer                         │
│                (ChatController, DTOs)                      │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Application Layer                        │
│            (ChatFacade, AgentOrchestrator,                 │
│             SessionManager)                                 │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Domain Layer                             │
│            (Session, Apartment, Filters, Services)          │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                 Infrastructure Layer                        │
│      (GigaChatClient, Repository, Mappers, Notifications)   │
└─────────────────────────────────────────────────────────────┘
```

### Key Design Patterns

| Pattern         | Implementation                                            |
| :-------------- | :-------------------------------------------------------- |
| **Facade**      | `ChatFacade` orchestrates session management and AI calls. |
| **Orchestrator**| `AgentOrchestrator` coordinates AI processing.           |
| **Repository**  | Spring Data JPA repositories for data access.             |
| **Mapper**      | `SessionMapper` and `ManagerTaskMapper` for DTO↔Entity.  |
| **ThreadLocal** | `SessionManager` uses ThreadLocal for request-scoped sessions. |
| **Strategy**    | Different search strategies (normal, expanded).            |

### Resilience Mechanisms

The application uses `@EnableResilientMethods` (Spring Resilience4j) for:

- **Retry:** Automatic retry on transient errors.
- **Circuit Breaker:** Prevents cascading failures.
- **Time Limiter:** Timeout protection for external calls.

---

## Troubleshooting

### Common Issues

| Issue                              | Solution                                                       |
| :--------------------------------- | :------------------------------------------------------------- |
| Session not found                  | Generate new session via `/init` endpoint.                    |
| Timeout during AI calls            | Check GigaChat API key and network connectivity.              |
| Empty apartment search results     | Check apartment data file path and format.                   |
| "Превышен лимит циклов" (limit cycles exceeded) | Session reached max cycles. Transfer to manager or reset.    |
| Database connection errors         | Verify PostgreSQL is running and connection string is correct. |

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
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request.

---

## Support

For questions and support:

- **Team Lead:** Alexey — [GitHub](https://github.com/AxineBro)
- **Frontend:** Ivan — [GitHub](https://github.com/Onizuk0)
- **DevOps:** Sergei — [GitHub](https://github.com/SergeyTerpugov)
