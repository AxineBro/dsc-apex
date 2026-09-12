package com.hackathon.agent.infrastructure.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.agent.application.orchestrator.AgentOrchestrator;
import com.hackathon.agent.domain.exception.DomainException;
import com.hackathon.agent.domain.model.ManagerTask;
import com.hackathon.agent.domain.model.Message;
import com.hackathon.agent.domain.model.Session;
import com.hackathon.agent.domain.model.SessionState;
import com.hackathon.agent.domain.service.NotificationService;
import com.hackathon.agent.infrastructure.persistence.entity.ManagerTaskEntity;
import com.hackathon.agent.infrastructure.persistence.mapper.ManagerTaskMapper;
import com.hackathon.agent.infrastructure.persistence.repository.ManagerTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/**
 * Реализация сервиса уведомлений менеджеров {@link NotificationService}, выполняющая
 * сохранение задачи в базу данных и логирование события перевода диалога.
 * <p>
 * Является основной реализацией для продакшен-окружения, заменяющей заглушку.
 * При вызове {@link #notifyManager} создаётся объект {@link ManagerTask},
 * который преобразуется в сущность {@link ManagerTaskEntity} через маппер
 * и сохраняется в репозиторий. Дополнительно выполняется логирование на уровне {@code INFO}.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Сохранение тикета для менеджера в базе данных (таблица {@code manager_tasks}).</li>
 *     <li>Логирование перевода диалога с указанием сессии, причины и телефона клиента.</li>
 *     <li>Формирование текстового представления истории диалога для передачи в задачу.</li>
 * </ul>
 *
 * <p><b>Алгоритм работы:</b></p>
 * <ol>
 *     <li>Сериализация истории диалога {@link Session#getDialogHistory()} в JSON-строку
 *         с помощью {@link ObjectMapper}. При ошибке сериализации используется
 *         стандартный {@code toString()} истории.</li>
 *     <li>Построение объекта {@link ManagerTask} через билдер с указанием:
 *         <ul>
 *             <li>{@code sessionId} — идентификатор сессии ({@link Session#getId()}).</li>
 *             <li>{@code clientPhone} — переданный номер телефона.</li>
 *             <li>{@code reason} — причина перевода.</li>
 *             <li>{@code selectedApartmentId} — всегда {@code null} (в текущей реализации не используется).</li>
 *             <li>{@code dialogHistory} — сериализованная история.</li>
 *             <li>{@code status} — начальный статус {@code "NEW"}.</li>
 *         </ul>
 *         Поле {@code createdAt} не устанавливается явно, предполагается,
 *         что оно генерируется на уровне базы данных (например, аннотация {@code @CreationTimestamp})
 *         или в маппере {@link ManagerTaskMapper}.</li>
 *     <li>Преобразование {@link ManagerTask} в {@link ManagerTaskEntity} через
 *         {@link ManagerTaskMapper#toEntity}.</li>
 *     <li>Сохранение сущности в БД через {@link ManagerTaskRepository#save}.</li>
 *     <li>Логирование события с ключевой информацией для аудита и мониторинга.</li>
 * </ol>
 *
 * <p><b>Обработка ошибок:</b></p>
 * <ul>
 *     <li>При ошибке сериализации JSON (например, циклические ссылки) используется
 *         fallback-механизм — {@code toString()} истории диалога.
 *         Это гарантирует, что уведомление всегда будет создано.</li>
 *     <li>Ошибки сохранения в БД пробрасываются выше и должны обрабатываться
 *         вызывающим кодом (например, в {@link AgentOrchestrator} или глобальном обработчике).</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * NotificationService notificationService = ...;
 * Session session = sessionManager.getCurrentSession();
 * notificationService.notifyManager(session, "user_requested", "+79001234567");
 * </pre>
 *
 * @author Axine
 * @since 1.0.0
 * @see NotificationService
 * @see ManagerTask
 * @see ManagerTaskRepository
 * @see ManagerTaskMapper
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoggingNotificationService implements NotificationService {

    /**
     * Репозиторий для сохранения задач менеджеров в базу данных.
     * <p>
     * Используется для выполнения операции {@code save} над сущностью
     * {@link ManagerTaskEntity}.
     * </p>
     */
    private final ManagerTaskRepository repository;

    /**
     * Маппер для преобразования между {@link ManagerTask} (доменная модель)
     * и {@link ManagerTaskEntity} (JPA-сущность).
     * <p>
     * Обеспечивает разделение доменного слоя и инфраструктурного (БД).
     * </p>
     */
    private final ManagerTaskMapper mapper;

    /**
     * Jackson {@link ObjectMapper} для сериализации истории диалога в JSON-строку.
     * <p>
     * Преобразует список сообщений {@link Message} в компактное текстовое
     * представление, которое будет сохранено в задаче и доступно менеджеру.
     * </p>
     */
    private final ObjectMapper objectMapper;

    /**
     * {@inheritDoc}
     * <p>
     * Реализация создаёт задачу для менеджера, сериализует историю диалога,
     * сохраняет в БД и логирует событие.
     * </p>
     *
     * <p><b>Детали реализации:</b></p>
     * <ul>
     *     <li>История диалога сериализуется в JSON. При неудаче используется
     *         {@code toString()} как fallback.</li>
     *     <li>Задача сохраняется со статусом {@code "NEW"}.</li>
     *     <li>Поле {@code selectedApartmentId} оставлено {@code null}, так как
     *         в текущей бизнес-логике не требуется.</li>
     *     <li>Логирование на уровне {@code INFO} включает ключ сессии, причину
     *         и телефон клиента.</li>
     * </ul>
     *
     * @param session сессия, требующая вмешательства менеджера (не {@code null})
     * @param reason  причина перевода (не {@code null}, не пусто)
     * @param phone   контактный телефон клиента (может быть {@code null})
     * @throws IllegalArgumentException если {@code session} равен {@code null}
     *         или {@code reason} пустой
     * @throws RuntimeException если сохранение в БД завершилось ошибкой
     *         (пробрасывается из репозитория)
     */
    @Override
    public void notifyManager(Session session, String reason, String phone) {
        if (session == null) {
            log.error("Попытка уведомить менеджера с null сессией");
            throw new DomainException("Session cannot be null");
        }
        if (reason == null || reason.isBlank()) {
            log.error("Попытка уведомить менеджера с пустой причиной для сессии {}", session.getSessionKey());
            throw new DomainException("Reason cannot be null or empty");
        }

        String sessionKey = session.getSessionKey();
        MDC.put("sessionId", sessionKey);
        long startTime = System.currentTimeMillis();

        log.info("Создание задачи для менеджера: сессия={}, причина={}, телефон={}",
                sessionKey, reason, phone != null ? maskPhone(phone) : "не указан");

        try{
            String dialogText;
            try {
                dialogText = objectMapper.writeValueAsString(session.getDialogHistory());
                log.debug("История диалога сериализована, длина={} символов", dialogText.length());
            } catch (JsonProcessingException e) {
                log.warn("Ошибка сериализации истории диалога для сессии {}: {}, используем toString()",
                        sessionKey, e.getMessage());
                dialogText = session.getDialogHistory().toString();
            }

            ManagerTask task = ManagerTask.builder()
                    .sessionId(session.getId())
                    .clientPhone(phone)
                    .reason(reason)
                    .selectedApartmentId(null)
                    .dialogHistory(dialogText)
                    .status("NEW")
                    .build();

            ManagerTaskEntity entity = mapper.toEntity(task);
            ManagerTaskEntity saved = repository.save(entity);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Задача для менеджера сохранена: id={}, сессия={}, причина={}, duration={}ms",
                    saved.getId(), sessionKey, reason, duration);

            if (log.isDebugEnabled()) {
                log.debug("Сохранённая задача: {}", saved);
            }
        }catch (Exception e) {
            log.error("Ошибка при создании задачи для менеджера для сессии {}: {}",
                    sessionKey, e.getMessage(), e);
            throw new DomainException("Не удалось сохранить задачу менеджера", e);
        }
    }

    private static String maskPhone(String p) {
        if (p == null || p.length() < 4) return "не указан";
        return "***" + p.substring(p.length() - 4);
    }
}