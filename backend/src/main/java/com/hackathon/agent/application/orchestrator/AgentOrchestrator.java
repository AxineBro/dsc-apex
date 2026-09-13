package com.hackathon.agent.application.orchestrator;

import com.hackathon.agent.application.dto.ProcessResult;
import com.hackathon.agent.application.facade.ChatFacade;
import com.hackathon.agent.domain.model.Session;
import com.hackathon.agent.domain.model.SessionState;
import com.hackathon.agent.domain.service.OfferService;
import com.hackathon.agent.infrastructure.ai.GigaChatClient;
import com.hackathon.agent.infrastructure.ai.prompt.SystemPromptProvider;
import com.hackathon.agent.infrastructure.ai.tools.GigaChatTools;
import com.hackathon.agent.infrastructure.catalog.ComplexCatalog;
import com.hackathon.agent.api.dto.response.ChatAttachment;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Оркестратор агентов, отвечающий за обработку входящих сообщений пользователя
 * с использованием AI-модели GigaChat.
 * <p>
 * Координирует весь цикл обработки: от добавления сообщения в историю диалога
 * до получения ответа от внешнего сервиса и обновления состояния сессии.
 * Выступает в роли центрального звена между фасадом чата и внешними API.
 * </p>
 *
 * <p><b>Основные обязанности:</b></p>
 * <ul>
 *     <li>Сохранение сообщения пользователя в истории сессии.</li>
 *     <li>Обновление времени последней активности сессии.</li>
 *     <li>Проверка завершённости сессии и блокировка дальнейшей обработки.</li>
 *     <li>Формирование системного промпта через {@link SystemPromptProvider}.</li>
 *     <li>Вызов GigaChat с историей диалога и доступными инструментами.</li>
 *     <li>Сохранение ответа ассистента в историю.</li>
 *     <li>Управление статусом сессии (переход в {@code AWAITING_INPUT} при успешном ответе,
 *         или в {@code TO_MANAGER} при критической ошибке).</li>
 *     <li>Обработка исключений с автоматическим переключением сессии на менеджера.</li>
 * </ul>
 *
 * <p><b>Поток выполнения:</b></p>
 * <ol>
 *     <li>Добавление сообщения пользователя в историю диалога
 *         ({@link SessionManager#addMessage}).</li>
 *     <li>Обновление временной метки последней активности
 *         ({@link SessionManager#updateLastActivity}).</li>
 *     <li>Проверка, не завершена ли сессия (статус {@link SessionState#FINISHED}).
 *         Если да — возвращается стандартное сообщение.</li>
 *     <li>Установка текущей сессии в контекст ({@link SessionManager#setCurrentSession}).</li>
 *     <li>Получение системного промпта из {@link SystemPromptProvider}.</li>
 *     <li>Сохранение старого статуса сессии для логики обновления.</li>
 *     <li>Вызов {@link GigaChatClient#call} с промптом, историей и инструментами.</li>
 *     <li>Если ответ непустой, добавление его в историю как сообщение ассистента.</li>
 *     <li>Обновление статуса: если статус не изменился или был {@code NEW},
 *         устанавливается {@code AWAITING_INPUT}.</li>
 *     <li>Сохранение сессии через {@link SessionManager#save}.</li>
 *     <li>Возврат ответа клиенту.</li>
 *     <li>В случае исключения — вызов {@link #transferToManager} с передачей
 *         причины ошибки и возврат стандартного сообщения о необходимости
 *         связаться со специалистом.</li>
 *     <li>В блоке {@code finally} очистка текущей сессии из контекста
 *         ({@link SessionManager#clearCurrentSession}).</li>
 * </ol>
 *
 * <p><b>Транзакция:</b></p>
 * Метод {@link #process} аннотирован {@code @Transactional}, что гарантирует
 * атомарность всех операций с сессией и историей сообщений. В случае ошибки
 * изменения не будут сохранены, а сессия перейдёт в состояние {@code TO_MANAGER}.
 *
 * <p><b>Обработка ошибок:</b></p>
 * Любое исключение, возникшее при вызове GigaChat или в процессе обработки,
 * перехватывается и инициирует передачу сессии менеджеру через вызов
 * {@link GigaChatTools#transferToManager}. Статус сессии устанавливается
 * в {@link SessionState#TO_MANAGER}, после чего клиенту возвращается
 * стандартное сообщение о необходимости связаться со специалистом.
 *
 * <p><b>Зависимости:</b></p>
 * <ul>
 *     <li>{@link GigaChatClient} — клиент для взаимодействия с AI-моделью.</li>
 *     <li>{@link SystemPromptProvider} — провайдер системного промпта.</li>
 *     <li>{@link GigaChatTools} — набор инструментов, доступных модели
 *         (например, для передачи менеджеру).</li>
 *     <li>{@link SessionManager} — менеджер для работы с сессиями
 *         (CRUD, история, контекст).</li>
 * </ul>
 *
 * <p><b>Примечание:</b> Лимиты циклов и количества выдаваемых квартир настраиваются
 * через конфигурационные параметры {@code app.max.cycles} и {@code app.flat.limit}
 * и используются внутри {@link GigaChatTools}.</p>
 *
 * @author Axine
 * @since 1.0.0
 * @see ChatFacade
 * @see SessionManager
 * @see GigaChatClient
 * @see SystemPromptProvider
 * @see GigaChatTools
 * @see SessionState
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentOrchestrator {

    private final GigaChatClient gigaChatClient;
    private final SystemPromptProvider promptProvider;
    private final GigaChatTools tools;
    private final SessionManager sessionManager;
    private final ComplexCatalog complexCatalog;
    private final OfferService offerService;

    /**
     * Пороговое значение времени выполнения (в миллисекундах) для вызова GigaChat,
     * превышение которого считается "медленным" и логируется как предупреждение.
     * <p>
     * Используется для мониторинга производительности внешнего AI-сервиса.
     * Если вызов занимает больше указанного времени, в лог записывается предупреждение
     * (WARN) с указанием длительности.
     * </p>
     * <p>
     * Значение задаётся через свойство {@code app.logging.slow-threshold-ms}
     * в конфигурационном файле (application.yml/properties).
     * Рекомендуемое значение: 5000 (5 секунд).
     * </p>
     *
     * @see AgentOrchestrator#process(Session, String)
     */
    @Value("${app.logging.slow-threshold-ms}")
    private long slowThresholdMs;

    /**
     * Обрабатывает сообщение и возвращает расширенный результат: ответ бота,
     * статус сессии, признак и причину перевода на менеджера, а также вложения.
     * <p>
     * Обёртка над {@link #process(Session, String)}: делегирует основную
     * обработку, затем собирает {@link ProcessResult}. Если сессия переведена
     * на менеджера ({@link SessionState#TO_MANAGER}), извлекает причину через
     * {@link GigaChatTools#drainLastTransferReason()} (при отсутствии —
     * {@code "unknown"}). Формирует вложения: изображения ЖК
     * ({@link ComplexCatalog#attachmentsFor(Session)}) и, при статусе
     * {@link SessionState#OFFER_READY}, ссылку на PDF последнего КП.
     * </p>
     * <p>
     * Выполняется в транзакции ({@code @Transactional}). Исключения не
     * пробрасываются — {@link #process} обрабатывает их самостоятельно и
     * возвращает fallback-ответ.
     * </p>
     *
     * @param session     объект сессии (не {@code null})
     * @param userMessage текст сообщения пользователя (не {@code null}, не пустой)
     * @return {@link ProcessResult} с ответом бота и метаданными; никогда не {@code null}
     * @see #process(Session, String)
     * @see ProcessResult
     * @see ChatFacade#processMessageRich(String, String)
     */
    @Transactional
    public ProcessResult processRich(Session session, String userMessage) {
        String reply;
        try {
            reply = process(session, userMessage);
        } finally {
            if (session != null && session.getStatus() != SessionState.TO_MANAGER) {
                tools.drainLastTransferReason();
            }
        }

        boolean transferred = session.getStatus() == SessionState.TO_MANAGER;
        String reason = null;
        if (transferred) {
            reason = tools.drainLastTransferReason();
            if (reason == null) reason = "unknown";
        }

        List<ChatAttachment> attachments = new ArrayList<>(complexCatalog.attachmentsFor(session));

        if (session.getStatus() == SessionState.OFFER_READY) {
            offerService.findLatestBySessionId(session.getId()).ifPresent(offer ->
                    attachments.add(new ChatAttachment(
                            "offer_pdf",
                            "/api/v1/offers/" + offer.getId() + "/pdf",
                            "Коммерческое предложение (PDF)"
                    ))
            );
        }
        return new ProcessResult(reply, session.getStatus(), transferred, reason, attachments);
    }


    /**
     * Обрабатывает сообщение пользователя в контексте сессии.
     * <p>
     * Основной метод оркестратора, выполняющий всю бизнес-логику обработки.
     * Подробное описание алгоритма приведено в документации класса.
     * </p>
     *
     * <p><b>Параметры:</b></p>
     * <ul>
     *     <li>{@code session} — объект сессии, содержащий историю диалога,
     *         статус и другую метаинформацию.</li>
     *     <li>{@code userMessage} — текст сообщения от пользователя.
     *         Не должен быть {@code null} или пустым.</li>
     * </ul>
     *
     * <p><b>Возвращаемое значение:</b></p>
     * Ответное сообщение от AI-модели или стандартное сообщение об ошибке
     * (если произошла передача менеджеру).
     *
     * <p><b>Исключения:</b></p>
     * Все исключения перехватываются внутри метода, обрабатываются
     * через {@link #transferToManager} и не пробрасываются наружу.
     * Вместо этого возвращается пользовательское сообщение.
     *
     * @param session     объект сессии (не {@code null})
     * @param userMessage текст сообщения пользователя (не {@code null}, не пустой)
     * @return ответное сообщение (может быть пустым, если модель не вернула ответ,
     *         но обычно содержит текст)
     * @see SessionManager#addMessage
     * @see SessionManager#updateLastActivity
     * @see SessionManager#setCurrentSession
     * @see SystemPromptProvider#getPrompt
     * @see GigaChatClient#call
     * @see SessionManager#save
     * @see #transferToManager
     */
    @Transactional
    public String process(Session session, String userMessage) {
        String sessionKey = session.getSessionKey();
        MDC.put("sessionId", sessionKey);
        long startTime = System.currentTimeMillis();

        log.info("Orchestrator started for session: {}", sessionKey);

        try {
            sessionManager.addMessage(session, "user", userMessage);
            log.debug("User message added to history for session {}", sessionKey);

            sessionManager.updateLastActivity(session);
            log.debug("Last activity updated for session {}", sessionKey);

            if (session.isFinished()) {
                log.warn("Session {} is already finished, skipping processing", sessionKey);
                return "Диалог уже завершён.";
            }

            sessionManager.setCurrentSession(session);
            log.debug("Current session set in context: {}", sessionKey);

            String systemPrompt = promptProvider.getPrompt();
            log.debug("System prompt obtained (length={})", systemPrompt != null ? systemPrompt.length() : 0);

            long callStart = System.currentTimeMillis();
            String reply = gigaChatClient.call(systemPrompt, session.getDialogHistory(), tools);
            long callDuration = System.currentTimeMillis() - callStart;

            if (callDuration > slowThresholdMs) {
                log.warn("GigaChat call for session {} took {}ms (threshold {}ms)",
                        sessionKey, callDuration, slowThresholdMs);
            } else {
                log.debug("GigaChat call for session {} completed in {}ms", sessionKey, callDuration);
            }

            if (reply != null && !reply.isBlank()) {
                sessionManager.addMessage(session, "assistant", reply);
                log.debug("Assistant reply added to history (length={})", reply.length());
            } else {
                log.warn("Received empty or null reply from GigaChat for session {}", sessionKey);
            }

            SessionState oldStatus = session.getStatus();
            if (session.getStatus() == SessionState.NEW) {
                session.setStatus(SessionState.AWAITING_INPUT);
                log.debug("Status updated from {} to {}", oldStatus, SessionState.AWAITING_INPUT);
            } else {
                log.debug("Status remains {} (was {})", session.getStatus(), oldStatus);
            }

            sessionManager.save(session);
            log.debug("Session {} saved successfully", sessionKey);

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("Orchestrator finished successfully for session {}: duration={}ms, replyLength={}",
                    sessionKey, totalDuration, reply != null ? reply.length() : 0);

            return reply;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Exception in orchestrator for session {}: duration={}ms, error={}",
                    sessionKey, duration, e.getMessage(), e);
            String fallbackReply = transferToManager(session, "error_" + e.getClass().getSimpleName());
            log.warn("Session {} transferred to manager due to error", sessionKey);
            return fallbackReply;
        } finally {
            sessionManager.clearCurrentSession();
        }
    }

    /**
     * Переводит сессию в состояние передачи менеджеру при возникновении ошибки.
     * <p>
     * Этот метод вызывается из блока {@code catch} метода {@link #process}
     * в случае любого исключения. Он:
     * <ol>
     *     <li>Вызывает {@link GigaChatTools#transferToManager} с указанием причины
     *         ошибки (для логирования или нотификации).</li>
     *     <li>Устанавливает статус сессии в {@link SessionState#TO_MANAGER}.</li>
     *     <li>Сохраняет изменения сессии через {@link SessionManager#save}.</li>
     * </ol>
     * </p>
     *
     * <p><b>Возвращаемое значение:</b></p>
     * Стандартное сообщение для пользователя, информирующее о необходимости
     * связаться со специалистом.
     *
     * @param session объект сессии, которую необходимо перевести в состояние менеджера
     * @param reason  причина перевода (например, класс ошибки), передаётся в инструмент
     * @return стандартное пользовательское сообщение
     * @see GigaChatTools#transferToManager
     * @see SessionState#TO_MANAGER
     */
    private String transferToManager(Session session, String reason) {
        tools.transferToManager(reason, null);
        session.setStatus(SessionState.TO_MANAGER);
        sessionManager.save(session);
        return "К сожалению, я не смог подобрать варианты. С вами свяжется наш специалист.";
    }
}