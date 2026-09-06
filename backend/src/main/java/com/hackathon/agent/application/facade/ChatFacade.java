package com.hackathon.agent.application.facade;

import com.hackathon.agent.api.controller.ChatController;
import com.hackathon.agent.application.orchestrator.AgentOrchestrator;
import com.hackathon.agent.application.orchestrator.SessionManager;
import com.hackathon.agent.domain.model.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Фасадный сервис, координирующий обработку входящих сообщений в чат-системе.
 * <p>
 * Выступает в роли единой точки входа для бизнес-логики обработки сообщений.
 * Инкапсулирует взаимодействие между менеджером сессий и оркестратором агентов,
 * обеспечивая прозрачность для контроллера.
 * </p>
 *
 * <p><b>Основные обязанности:</b></p>
 * <ul>
 *     <li>Получение или создание сессии по ключу через {@link SessionManager}.</li>
 *     <li>Делегирование обработки сообщения оркестратору {@link AgentOrchestrator}.</li>
 *     <li>Сохранение изменённого состояния сессии после обработки.</li>
 *     <li>Логирование ключевых этапов процесса на уровнях DEBUG и INFO.</li>
 * </ul>
 *
 * <p><b>Поток выполнения:</b></p>
 * <ol>
 *     <li>Получение сессии по {@code sessionKey} (если не существует — создаётся).</li>
 *     <li>Логирование факта обработки.</li>
 *     <li>Вызов {@link AgentOrchestrator#process(Session, String)} для генерации ответа.</li>
 *     <li>Сохранение сессии (обновление времени последней активности, возможно, изменение статуса).</li>
 *     <li>Возврат ответа клиенту через контроллер.</li>
 * </ol>
 *
 * <p><b>Зависимости:</b></p>
 * <ul>
 *     <li>{@link SessionManager} — управляет жизненным циклом сессий (CRUD, кэширование).</li>
 *     <li>{@link AgentOrchestrator} — координатор агентов, формирующих ответ на основе сообщения и состояния сессии.</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see SessionManager
 * @see AgentOrchestrator
 * @see ChatController
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatFacade {

    private final SessionManager sessionManager;
    private final AgentOrchestrator orchestrator;

    /**
     * Обрабатывает входящее сообщение пользователя в рамках указанной сессии.
     * <p>
     * Получает или создаёт сессию по ключу {@code sessionKey}, делегирует
     * генерацию ответа оркестратору, сохраняет обновлённую сессию и возвращает
     * ответное сообщение.
     * </p>
     *
     * <p><b>Алгоритм:</b></p>
     * <ol>
     *     <li>Вызов {@link SessionManager#getOrCreate(String)} для получения сессии.
     *         Если сессия с данным ключом отсутствует, создаётся новая с начальным статусом
     *         (обычно {@code ACTIVE} и текущим временем).</li>
     *     <li>Логирование на уровне DEBUG с информацией о ключе и статусе сессии.</li>
     *     <li>Передача сессии и текста сообщения в {@link AgentOrchestrator#process(Session, String)}.
     *         Оркестратор может обращаться к внешним AI-сервисам, базам знаний и т.д.</li>
     *     <li>Сохранение сессии через {@link SessionManager#save(Session)}.
     *         Обычно обновляется поле {@code lastActivityAt}, возможно, статус.</li>
     *     <li>Возврат сгенерированного ответа (строки).</li>
     * </ol>
     *
     * <p><b>Параметры сессии:</b></p>
     * Предполагается, что объект {@link Session} содержит как минимум:
     * <ul>
     *     <li>{@code sessionKey} — строковый идентификатор.</li>
     *     <li>{@code status} — текущее состояние (ACTIVE, EXPIRED и т.д.).</li>
     *     <li>{@code lastActivityAt} — временная метка для отслеживания активности.</li>
     * </ul>
     *
     * @param sessionKey уникальный идентификатор сессии (передаётся клиентом в заголовке X-Session-Id).
     *                   Не должен быть {@code null} или пустым.
     * @param userMessage текст сообщения от пользователя. Не должен быть {@code null} или пустым
     *                   (валидация выполняется на уровне контроллера).
     * @return текстовый ответ, сгенерированный оркестратором агентов.
     * @throws IllegalArgumentException если {@code sessionKey} или {@code userMessage} невалидны
     *         (хотя валидация обычно уже выполнена ранее).
     * @throws RuntimeException если в процессе обработки возникает ошибка (например,
     *         сбой при сохранении, ошибка оркестратора). Конкретные исключения зависят
     *         от реализаций зависимостей.
     * @see SessionManager#getOrCreate(String)
     * @see AgentOrchestrator#process(Session, String)
     * @see SessionManager#save(Session)
     */
    public String processMessage(String sessionKey, String userMessage) {
        MDC.put("sessionId", sessionKey);

        long startTime = System.currentTimeMillis();
        log.info("Processing message for session: {}", sessionKey);

        if (log.isDebugEnabled()) {
            String truncated = userMessage.length() > 100 ? userMessage.substring(0, 100) + "..." : userMessage;
            log.debug("User message: '{}'", truncated);
        }

        try {
            Session session = sessionManager.getOrCreate(sessionKey);
            log.debug("Session retrieved: status={}, lastActivity={}", session.getStatus(), session.getLastActivityAt());

            String reply = orchestrator.process(session, userMessage);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Message processed successfully: session={}, duration={}ms, replyLength={}",
                    sessionKey, duration, reply != null ? reply.length() : 0);

            if (log.isDebugEnabled()) {
                log.debug("Reply: '{}'", reply);
            }


            return reply;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed to process message for session {}: duration={}ms, error={}",
                    sessionKey, duration, e.getMessage(), e);
            throw e;
        }
    }
}