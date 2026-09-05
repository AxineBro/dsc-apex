package com.hackathon.agent.api.controller;

import com.hackathon.agent.api.dto.request.ChatRequest;
import com.hackathon.agent.api.dto.response.ChatResponse;
import com.hackathon.agent.api.dto.response.SessionInitResponse;
import com.hackathon.agent.application.facade.ChatFacade;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST-контроллер для управления чат-сессиями и обработки сообщений.
 * <p>
 * Предоставляет эндпоинты для инициализации сессии и отправки сообщений боту.
 * Все операции логируются с использованием SLF4J.
 * </p>
 *
 * <p><b>Версия API:</b> v1</p>
 * <p><b>Базовый путь:</b> {@code /api/v1/chat}</p>
 *
 * @author Axine
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatFacade chatFacade;

    /**
     * Обрабатывает сообщение пользователя в рамках текущей сессии.
     * <p>
     * Если заголовок {@code X-Session-Id} не передан или содержит пустое значение,
     * автоматически генерируется новый идентификатор сессии (UUID) и создаётся новая сессия.
     * В ответе возвращается ответ бота и актуальный идентификатор сессии.
     * </p>
     *
     * <p><b>Пример запроса:</b></p>
     * <pre>
     * POST /api/v1/chat/message
     * Headers: X-Session-Id: 550e8400-e29b-41d4-a716-446655440000
     * Body:
     * {
     *     "message": "Привет, как дела?"
     * }
     * </pre>
     *
     * <p><b>Пример ответа (200 OK):</b></p>
     * <pre>
     * {
     *     "reply": "Привет! У меня всё отлично, спасибо.",
     *     "sessionId": "550e8400-e29b-41d4-a716-446655440000"
     * }
     * </pre>
     *
     * @param sessionId идентификатор сессии, передаваемый в заголовке {@code X-Session-Id}.
     *                  Может быть {@code null} или пустым – в этом случае будет создана новая сессия.
     * @param request   тело запроса, содержащее сообщение пользователя. Объект проходит валидацию
     *                  (аннотация {@code @Valid}).
     * @return ответ с сообщением бота и идентификатором сессии, обёрнутый в {@link ResponseEntity}
     *         со статусом {@code 200 OK}.
     * @see ChatRequest
     * @see ChatResponse
     */
    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestBody @Valid ChatRequest request,
            HttpServletRequest httpRequest
    ) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);
        MDC.put("sessionId", sessionId != null ? sessionId : "new");

        long startTime = System.currentTimeMillis();
        try {
            String clientIp = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            String requestUri = httpRequest.getRequestURI();

            log.info("Incoming request: method=POST, uri={}, sessionId={}, ip={}, userAgent={}",
                    requestUri, sessionId, clientIp, userAgent);

            if (log.isDebugEnabled()) {
                String msg = request.getMessage();
                String truncated = msg.length() > 100 ? msg.substring(0, 100) + "..." : msg;
                log.debug("Request body: message='{}'", truncated);
            }

            if (sessionId == null || sessionId.isBlank()) {
                sessionId = UUID.randomUUID().toString();
                log.info("Создана новая сессия: {}", sessionId);
            }

            log.debug("Получено сообщение от сессии {}: {}", sessionId, request.getMessage());

            String reply = chatFacade.processMessage(sessionId, request.getMessage());

            long duration = System.currentTimeMillis() - startTime;
            log.info("Request processed successfully: sessionId={}, duration={}ms, replyLength={}",
                    sessionId, duration, reply != null ? reply.length() : 0);

            if (log.isDebugEnabled()) {
                log.debug("Reply content: {}", reply);
            }

            return ResponseEntity.ok(new ChatResponse(reply, sessionId));
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Error processing request: sessionId={}, duration={}ms, error={}",
                    sessionId, duration, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * Инициализирует новую чат-сессию и возвращает свежий идентификатор сессии.
     * <p>
     * Используется для получения {@code sessionId} перед отправкой первого сообщения,
     * хотя сессия также создаётся автоматически при первом вызове {@link #sendMessage}.
     * </p>
     *
     * <p><b>Пример запроса:</b></p>
     * <pre>
     * GET /api/v1/chat/init
     * </pre>
     *
     * <p><b>Пример ответа (200 OK):</b></p>
     * <pre>
     * {
     *     "sessionId": "550e8400-e29b-41d4-a716-446655440000"
     * }
     * </pre>
     *
     * @return объект {@link SessionInitResponse}, содержащий сгенерированный UUID,
     *         обёрнутый в {@link ResponseEntity} со статусом {@code 200 OK}.
     * @see SessionInitResponse
     */
    @GetMapping("/init")
    public ResponseEntity<SessionInitResponse> initSession(HttpServletRequest httpRequest) {
        String correlationId = UUID.randomUUID().toString();
        MDC.put("correlationId", correlationId);

        long startTime = System.currentTimeMillis();
        try {
            String clientIp = getClientIp(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            log.info("Session init request: ip={}, userAgent={}", clientIp, userAgent);

            String sessionId = UUID.randomUUID().toString();
            log.debug("Generated new sessionId: {}", sessionId);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Session init successful: sessionId={}, duration={}ms", sessionId, duration);

            return ResponseEntity.ok(new SessionInitResponse(sessionId));
        } catch (Exception e) {
            log.error("Session init failed: duration={}ms, error={}",
                    System.currentTimeMillis() - startTime, e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * Извлекает реальный IP-адрес клиента из HTTP-запроса.
     * <p>
     * Последовательно проверяет стандартные заголовки прокси-серверов:
     * <ul>
     *     <li>{@code X-Forwarded-For} — стандартный заголовок для передачи исходного IP.</li>
     *     <li>{@code Proxy-Client-IP} — используется некоторыми прокси.</li>
     *     <li>{@code WL-Proxy-Client-IP} — используется WebLogic.</li>
     * </ul>
     * Если ни один из заголовков не содержит IP, возвращается {@link HttpServletRequest#getRemoteAddr()}.
     * В случае, когда в заголовке перечислено несколько IP (разделены запятыми), возвращается первый.
     * </p>
     *
     * <p><b>Обработка:</b></p>
     * <ul>
     *     <li>Значения {@code null}, пустые строки или {@code "unknown"} игнорируются.</li>
     *     <li>Результат используется для логирования и аудита запросов.</li>
     * </ul>
     *
     * @param request HTTP-запрос (не {@code null})
     * @return IP-адрес клиента в виде строки (всегда не {@code null}, не пустой)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}