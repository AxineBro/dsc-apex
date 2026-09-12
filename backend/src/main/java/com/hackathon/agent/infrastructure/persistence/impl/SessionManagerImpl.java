package com.hackathon.agent.infrastructure.persistence.impl;

import com.hackathon.agent.application.orchestrator.SessionManager;
import com.hackathon.agent.domain.exception.DomainException;
import com.hackathon.agent.domain.model.Filters;
import com.hackathon.agent.domain.model.Session;
import com.hackathon.agent.domain.model.SessionState;
import com.hackathon.agent.infrastructure.ai.tools.GigaChatTools;
import com.hackathon.agent.infrastructure.persistence.entity.SessionEntity;
import com.hackathon.agent.infrastructure.persistence.mapper.SessionMapper;
import com.hackathon.agent.infrastructure.persistence.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Реализация менеджера сессий {@link SessionManager}, обеспечивающая полный цикл управления
 * диалоговыми сессиями с сохранением состояния в базе данных.
 * <p>
 * Использует паттерн ThreadLocal для хранения текущей сессии в контексте потока выполнения,
 * что позволяет получать доступ к сессии без явной передачи параметров через цепочку вызовов.
 * Все операции, изменяющие состояние сессии, выполняются в рамках транзакций (@Transactional)
 * и сохраняются в базу данных через JPA-репозиторий.
 * </p>
 *
 * <p><b>Основные обязанности:</b></p>
 * <ul>
 *     <li>Создание новой сессии или получение существующей по ключу через {@link #getOrCreate}.</li>
 *     <li>Сохранение состояния сессии в БД через {@link #save} с обновлением версии и идентификатора.</li>
 *     <li>Обновление фильтров подбора с автоматическим инкрементом счётчика циклов при критических изменениях.</li>
 *     <li>Управление историей сообщений, ранжированными списками, временными метками.</li>
 *     <li>Поиск неактивных сессий для последующей очистки через {@link #findExpired}.</li>
 *     <li>Управление текущей сессией в ThreadLocal-контексте ({@link #setCurrentSession}, {@link #clearCurrentSession}).</li>
 * </ul>
 *
 * <p><b>Архитектурные особенности:</b></p>
 * <ul>
 *     <li><b>Транзакция:</b> Все методы, модифицирующие состояние, аннотированы {@code @Transactional},
 *         что гарантирует атомарность операций и согласованность данных.</li>
 *     <li><b>Маппинг:</b> Преобразование между доменной моделью {@link Session} и JPA-сущностью
 *         {@link SessionEntity} выполняется через {@link SessionMapper}.</li>
 *     <li><b>Контекст потока:</b> {@code ThreadLocal<Session>} обеспечивает изоляцию сессий
 *         между разными запросами в многопоточном окружении.</li>
 *     <li><b>Оптимистичная блокировка:</b> Версия сущности обновляется при каждом сохранении,
 *         предотвращая конкурентные изменения.</li>
 * </ul>
 *
 * <p><b>Использование в цепочке вызовов:</b></p>
 * <pre>
 * // В контроллере/фасаде
 * Session session = sessionManager.getOrCreate(sessionKey);
 * sessionManager.setCurrentSession(session);
 * try {
 *     // обработка запроса
 *     String reply = orchestrator.process(session, userMessage);
 * } finally {
 *     sessionManager.clearCurrentSession();
 * }
 * </pre>
 *
 * <p><b>Обработка ошибок:</b></p>
 * <ul>
 *     <li>Ошибки сохранения в БД (например, нарушение уникальности) пробрасываются как
 *         {@link org.springframework.dao.DataAccessException}.</li>
 *     <li>При отсутствии сессии в ThreadLocal методы {@link #getCurrentSession} возвращают {@code null},
 *         что обрабатывается вызывающим кодом (например, {@link GigaChatTools}).</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see SessionManager
 * @see Session
 * @see SessionEntity
 * @see SessionMapper
 * @see SessionRepository
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SessionManagerImpl implements SessionManager {

    private final SessionRepository repository;
    private final SessionMapper mapper;

    /**
     * ThreadLocal-контейнер для хранения текущей сессии в контексте потока.
     * <p>
     * Позволяет получать доступ к сессии из любого места без явной передачи параметров.
     * Устанавливается перед началом обработки запроса и очищается после завершения.
     * </p>
     *
     * @see #setCurrentSession(Session)
     * @see #clearCurrentSession()
     * @see #getCurrentSession()
     */
    private static final ThreadLocal<Session> currentSession = new ThreadLocal<>();

    /**
     * {@inheritDoc}
     * <p>
     * Реализация выполняет поиск сессии в БД по ключу через {@link SessionRepository#findBySessionKey}.
     * Если сессия не найдена, создаётся новая с начальным статусом {@link SessionState#NEW},
     * пустой историей и ранжированным списком. Новая сессия сразу сохраняется в БД.
     * </p>
     *
     * <p><b>Транзакция:</b> Метод аннотирован {@code @Transactional}, гарантирует,
     * что поиск и создание выполняются в одной транзакции.</p>
     *
     * @param sessionKey уникальный ключ сессии (не {@code null}, не пустой)
     * @return существующая или вновь созданная сессия (всегда не {@code null})
     * @throws IllegalArgumentException если {@code sessionKey} равен {@code null} или пустой
     */
    @Override
    @Transactional
    public Session getOrCreate(String sessionKey) {
        MDC.put("sessionId", sessionKey);

        if (sessionKey == null || sessionKey.isBlank()) {
            throw new DomainException("Session key cannot be null or empty");
        }

        long startTime = System.currentTimeMillis();
        log.debug("Getting or creating session for key: {}", sessionKey);

        try {
            return repository.findBySessionKey(sessionKey)
                    .map(entity -> {
                        Session session = mapper.toDomain(entity);
                        log.debug("Existing session found: id={}, status={}, lastActivity={}",
                                session.getId(), session.getStatus(), session.getLastActivityAt());
                        long duration = System.currentTimeMillis() - startTime;
                        log.debug("Session retrieval completed in {}ms", duration);
                        return session;
                    })
                    .orElseGet(() -> {
                        log.info("Creating new session for key: {}", sessionKey);
                        Session newSession = Session.builder()
                                .sessionKey(sessionKey)
                                .status(SessionState.NEW)
                                .counter(0)
                                .dialogHistory(new ArrayList<>())
                                .rankedList(new ArrayList<>())
                                .lastActivityAt(LocalDateTime.now())
                                .build();
                        save(newSession);
                        long duration = System.currentTimeMillis() - startTime;
                        log.info("New session created and saved: key={}, id={}, duration={}ms",
                                sessionKey, newSession.getId(), duration);
                        return newSession;
                    });
        } catch (Exception e) {
            log.error("Failed to get or create session for key {}: {}", sessionKey, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Сохраняет сессию в базе данных через репозиторий. После сохранения обновляет
     * доменную модель: устанавливает {@code id}, {@code version} и {@code createdAt}
     * из сохранённой сущности.
     * </p>
     *
     * <p><b>Оптимистичная блокировка:</b> Поле {@code version} проверяется при сохранении,
     * и если версия изменилась с момента загрузки, выбрасывается исключение
     * {@link org.springframework.orm.ObjectOptimisticLockingFailureException}.</p>
     *
     * @param session объект сессии для сохранения (не {@code null})
     * @throws IllegalArgumentException если {@code session} равен {@code null}
     * @throws org.springframework.dao.DataAccessException если сохранение в БД не удалось
     */
    @Override
    @Transactional
    public void save(Session session) {
        if (session == null) {
            log.warn("Attempt to save null session");
            throw new DomainException("Session cannot be null");
        }

        String sessionKey = session.getSessionKey();
        MDC.put("sessionId", sessionKey);

        long startTime = System.currentTimeMillis();
        log.debug("Saving session: key={}, id={}, version={}, status={}",
                sessionKey, session.getId(), session.getVersion(), session.getStatus());

        try {
            SessionEntity entity = mapper.toEntity(session);
            SessionEntity saved = repository.save(entity);

            session.setId(saved.getId());
            session.setVersion(saved.getVersion());
            session.setCreatedAt(saved.getCreatedAt());

            long duration = System.currentTimeMillis() - startTime;
            if (duration > 500) {
                log.warn("Session save took {}ms for key {} (threshold 500ms)", duration, sessionKey);
            } else {
                log.debug("Session saved successfully: key={}, id={}, version={}, duration={}ms",
                        sessionKey, saved.getId(), saved.getVersion(), duration);
            }
        } catch (Exception e) {
            log.error("Failed to save session key {}: {}", sessionKey, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Обновляет фильтры сессии, сбрасывает индекс текущей позиции, флаг расширения
     * и счётчик уточнений. Если критически важные фильтры (площадь или цена) изменились,
     * инкрементирует счётчик циклов {@code counter}. Обновляет время последней активности
     * и сохраняет сессию.
     * </p>
     *
     * <p><b>Критические изменения:</b> Считаются изменения в полях {@code areaMin},
     * {@code areaMax}, {@code priceMin}, {@code priceMax}. Изменение количества комнат
     * или этажа не увеличивает счётчик (они не считаются "перезапуском" поиска).</p>
     *
     * <p><b>Сброс:</b> При обновлении фильтров всегда выполняется:
     * <ul>
     *     <li>Сброс {@code currentIndex} в 0 (начало просмотра списка).</li>
     *     <li>Сброс {@code expandedOnce} (расширение может быть применено снова).</li>
     *     <li>Сброс {@code clarificationCount} (новый поиск — новые уточнения).</li>
     * </ul>
     * </p>
     *
     * @param session     объект сессии (не {@code null})
     * @param newFilters  новые фильтры (может быть {@code null} — тогда фильтры обнуляются)
     */
    @Override
    @Transactional
    public void updateFilters(Session session, Filters newFilters) {
        if (session == null) {
            log.warn("Attempt to update filters on null session");
            throw new DomainException("Session cannot be null");
        }
        if (newFilters == null) {
            log.warn("Received null filters for session {}, resetting filters", session.getSessionKey());
        }

        String sessionKey = session.getSessionKey();
        MDC.put("sessionId", sessionKey);

        log.debug("Updating filters for session {}: old filters (area={}-{}, price={}-{}, rooms={}-{}, floor={})",
                sessionKey,
                session.getAreaMin(), session.getAreaMax(),
                session.getPriceMin(), session.getPriceMax(),
                session.getRoomsMin(), session.getRoomsMax(),
                session.getFloor());


        boolean criticalChanged = !Objects.equals(session.getAreaMin(), newFilters.getAreaMin()) ||
                !Objects.equals(session.getAreaMax(), newFilters.getAreaMax()) ||
                !Objects.equals(session.getPriceMin(), newFilters.getPriceMin()) ||
                !Objects.equals(session.getPriceMax(), newFilters.getPriceMax());

        session.setAreaMin(newFilters.getAreaMin());
        session.setAreaMax(newFilters.getAreaMax());
        session.setPriceMin(newFilters.getPriceMin());
        session.setPriceMax(newFilters.getPriceMax());
        session.setRoomsMin(newFilters.getRoomsMin());
        session.setRoomsMax(newFilters.getRoomsMax());
        session.setFloor(newFilters.getFloor());
        session.setComplex(newFilters.getComplex());

        session.setCurrentIndex(0);
        session.resetExpandedOnce();
        session.resetClarificationCount();

        if (criticalChanged) {
            int newCounter = session.getCounter() + 1;
            session.setCounter(newCounter);
            log.info("Critical filters changed for session {}: counter incremented to {}",
                    sessionKey, newCounter);
        } else {
            log.debug("Non-critical filters updated for session {}", sessionKey);
        }

        session.updateLastActivity();
        save(session);
        log.debug("Filters updated and session saved for {}", sessionKey);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Устанавливает ранжированный список ID квартир в сессии, обновляет время активности
     * и сохраняет сессию.
     * </p>
     *
     * @param session   объект сессии (не {@code null})
     * @param rankedIds список ID квартир в порядке ранжирования (может быть {@code null} или пустым)
     */
    @Override
    @Transactional
    public void updateRankedList(Session session, List<Long> rankedIds) {
        if (session == null) {
            log.warn("Attempt to update ranked list on null session");
            throw new DomainException("Session cannot be null");
        }

        String sessionKey = session.getSessionKey();
        MDC.put("sessionId", sessionKey);

        int size = (rankedIds != null) ? rankedIds.size() : 0;
        log.debug("Updating ranked list for session {}: {} items", sessionKey, size);

        session.setRankedList(rankedIds);
        session.updateLastActivity();
        save(session);

        log.debug("Ranked list updated for session {}", sessionKey);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Добавляет новое сообщение в историю диалога, обновляет время последней активности
     * и сохраняет сессию.
     * </p>
     *
     * @param session объект сессии (не {@code null})
     * @param role    роль отправителя ({@code "user"} или {@code "assistant"})
     * @param text    текст сообщения
     */
    @Override
    @Transactional
    public void addMessage(Session session, String role, String text) {
        if (session == null) {
            log.warn("Attempt to add message to null session");
            throw new DomainException("Session cannot be null");
        }

        String sessionKey = session.getSessionKey();
        MDC.put("sessionId", sessionKey);

        int textLength = (text != null) ? text.length() : 0;
        log.debug("Adding message to session {}: role={}, textLength={}", sessionKey, role, textLength);

        session.addMessage(role, text);
        session.updateLastActivity();
        save(session);
        log.debug("Message added to session {}", sessionKey);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Обновляет время последней активности на текущий момент и сохраняет сессию.
     * </p>
     *
     * @param session объект сессии (не {@code null})
     */
    @Override
    @Transactional
    public void updateLastActivity(Session session) {
        if (session == null) {
            log.warn("Attempt to update last activity on null session");
            throw new DomainException("Session cannot be null");
        }

        String sessionKey = session.getSessionKey();
        MDC.put("sessionId", sessionKey);

        log.debug("Updating last activity for session {}", sessionKey);
        session.updateLastActivity();
        save(session);
        log.debug("Last activity updated for session {}", sessionKey);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Находит сессии со статусами {@code AWAITING_INPUT} или {@code TIMEOUT_REMINDER},
     * у которых время последней активности превышает указанный порог.
     * </p>
     * <p>
     * <b>Примечание:</b> Метод не является транзакционным, так как только читает данные.
     * </p>
     *
     * @param timeoutMinutes максимальное количество минут бездействия (не {@code null}, > 0)
     * @return список сессий, которые можно считать истёкшими (не {@code null}, может быть пустым)
     */
    @Override
    public List<Session> findExpired(int timeoutMinutes) {
        if (MDC.get("correlationId") == null) {
            MDC.put("correlationId", "scheduler-" + UUID.randomUUID().toString());
        }

        long startTime = System.currentTimeMillis();
        log.info("Searching for expired sessions with timeout {} minutes", timeoutMinutes);

        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
            List<SessionEntity> entities = repository.findByStatusInAndLastActivityAtBefore(
                    List.of(SessionState.AWAITING_INPUT.name(), SessionState.TIMEOUT_REMINDER.name()),
                    threshold);

            int count = entities.size();
            long duration = System.currentTimeMillis() - startTime;
            log.info("Found {} expired sessions (duration={}ms)", count, duration);

            if (log.isDebugEnabled() && count > 0) {
                List<String> keys = entities.stream().map(SessionEntity::getSessionKey).toList();
                log.debug("Expired session keys: {}", keys);
            }

            return entities.stream().map(mapper::toDomain).toList();
        } catch (Exception e) {
            log.error("Failed to find expired sessions: {}", e.getMessage(), e);
            throw e;
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return текущая сессия или {@code null}, если она не была установлена
     */
    @Override
    public Session getCurrentSession() {
        Session session = currentSession.get();
        if (session != null) {
            log.debug("Current session retrieved: key={}", session.getSessionKey());
        } else {
            log.debug("No current session in ThreadLocal");
        }
        return session;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Устанавливает текущую сессию в ThreadLocal-контекст.
     * </p>
     *
     * @param session сессия для установки (может быть {@code null})
     */
    public void setCurrentSession(Session session) {
        if (session != null) {
            MDC.put("sessionId", session.getSessionKey());
            log.debug("Setting current session: key={}", session.getSessionKey());
        } else {
            log.debug("Clearing current session (setting null)");
        }
        currentSession.set(session);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Очищает текущую сессию из ThreadLocal-контекста. Рекомендуется вызывать
     * в блоке {@code finally} после обработки запроса.
     * </p>
     */
    public void clearCurrentSession() {
        Session session = currentSession.get();
        if (session != null) {
            log.debug("Clearing current session: key={}", session.getSessionKey());
        } else {
            log.debug("Clearing current session (already null)");
        }
        currentSession.remove();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Устанавливает ранжированный список в сессии и сбрасывает индекс текущей позиции в 0
     * (показ начинается с первого элемента). Обновляет время последней активности.
     * В отличие от {@link #updateRankedList}, этот метод НЕ СОХРАНЯЕТ сессию в БД,
     * что позволяет выполнять несколько операций перед фиксацией.
     * </p>
     *
     * <p><b>Использование:</b></p>
     * Этот метод вызывается из {@link GigaChatTools#searchApartments} для установки
     * списка перед возвратом ответа. Сохранение происходит позже в цепочке вызовов.
     *
     * @param session   объект сессии (не {@code null})
     * @param rankedIds список ID квартир в порядке ранжирования (может быть {@code null} или пустым)
     */
    @Override
    public void setRankedList(Session session, List<Long> rankedIds) {
        if (session == null) {
            log.warn("Attempt to set ranked list on null session");
            throw new DomainException("Session cannot be null");
        }

        String sessionKey = session.getSessionKey();
        MDC.put("sessionId", sessionKey);

        int size = (rankedIds != null) ? rankedIds.size() : 0;
        log.debug("Setting ranked list (without save) for session {}: {} items", sessionKey, size);

        session.setRankedList(rankedIds);
        session.setCurrentIndex(0);
        session.updateLastActivity();
        log.debug("Ranked list set for session {} (not saved yet)", sessionKey);
    }
}