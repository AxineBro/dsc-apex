package com.hackathon.agent.scheduler;

import com.hackathon.agent.application.orchestrator.SessionManager;
import com.hackathon.agent.domain.model.Session;
import com.hackathon.agent.domain.model.SessionState;
import com.hackathon.agent.domain.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

import java.util.List;
import java.util.UUID;

/**
 * Планировщик для обработки тайм-аутов неактивных сессий.
 * <p>
 * Выполняется каждую минуту (согласно выражению {@code cron = "* /60 * * * * *"})
 * и проверяет сессии, которые превысили допустимое время бездействия.
 * Реализует двухступенчатый механизм:
 * <ol>
 * <li><b>Первое бездействие:</b> Сессии со статусом {@code AWAITING_INPUT},
 *         у которых время последней активности превышает порог, заданный в
 *         {@code app.timeout.scheduler} (см. {@link #TIMEOUT_MINUTES}),
 *         получают системное напоминание и переводятся в {@code TIMEOUT_REMINDER}.</li>
 *     <li><b>Второе бездействие:</b> Сессии в статусе {@code TIMEOUT_REMINDER},
 *         игнорирующие напоминание дольше, чем {@code TIMEOUT_WAIT_MINUTES} минут
 *         (задаётся через {@code app.timeout.reminder-wait-minutes}),
 *         передаются менеджеру через {@link NotificationService}.</li>
 * </ol>
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Автоматическое управление неактивными диалогами для предотвращения
 *         «зависания» сессий и улучшения пользовательского опыта.</li>
 *     <li>Снижение нагрузки на систему за счёт перевода неактивных сессий
 *         в завершённое состояние или передачу их менеджеру.</li>
 *     <li>Логирование всех действий для мониторинга и отладки.</li>
 * </ul>
 *
 * <p><b>Параметры конфигурации:</b></p>
 * <ul>
 *     <li>{@code app.timeout.scheduler} — время бездействия в минутах до первого напоминания
 *         (соответствует полю {@link #TIMEOUT_MINUTES}).</li>
 *     <li>{@code app.timeout.reminder-wait-minutes} — время ожидания после напоминания
 *         (в минутах), после которого сессия передаётся менеджеру
 *         (соответствует полю {@link #TIMEOUT_WAIT_MINUTES}).</li>
 *     <li>Период выполнения планировщика — каждые 60 секунд (настраивается через cron-выражение
 *         в аннотации {@code @Scheduled}).</li>
 * </ul>
 *
 * <p><b>Алгоритм работы:</b></p>
 * <ol>
 *     <li>Получение всех сессий, у которых статус {@code AWAITING_INPUT} или {@code TIMEOUT_REMINDER}
 *         и время последней активности старше {@code TIMEOUT_MINUTES} минут
 *         (через {@link SessionManager#findExpired}).</li>
 *     <li>Для каждой такой сессии:
 *         <ul>
 *             <li>Если статус {@code TIMEOUT_REMINDER} и с момента отправки напоминания
 *                 прошло более {@code TIMEOUT_WAIT_MINUTES} минут → вызывается
 *                 {@link NotificationService#notifyManager} для создания задачи менеджеру,
 *                 статус меняется на {@code TO_MANAGER}, сессия сохраняется.</li>
 *             <li>Если статус {@code AWAITING_INPUT} → отправляется сообщение-напоминание
 *                 (добавляется в историю через {@link SessionManager#addMessage}),
 *                 статус меняется на {@code TIMEOUT_REMINDER}, сохраняется метка отправки
 *                 напоминания ({@code reminderSentAt}), сессия сохраняется.</li>
 *         </ul>
 *     </li>
 * </ol>
 *
 * <p><b>Транзакция:</b></p>
 * Метод {@link #checkTimeouts} аннотирован {@code @Transactional}, что гарантирует атомарность
 * операций с каждой сессией. В случае ошибки изменения не будут зафиксированы.
 *
 * <p><b>Логирование:</b></p>
 * На уровне INFO фиксируются события:
 * <ul>
 *     <li>Отправка напоминания пользователю.</li>
 *     <li>Перевод сессии на менеджера после игнорирования напоминания.</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * Компонент автоматически активируется при старте Spring-контекста благодаря
 * аннотации {@code @Component} и настройке планировщика (требуется аннотация
 * {@code @EnableScheduling} на уровне конфигурации).
 *
 * @author Axine
 * @since 1.0.0
 * @see SessionManager
 * @see NotificationService
 * @see SessionState
 * @see Scheduled
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TimeoutScheduler {

    /**
     * Менеджер сессий для поиска, обновления и сохранения сессий.
     */
    private final SessionManager sessionManager;

    /**
     * Сервис уведомлений для создания задач менеджеру при окончательном тайм-ауте.
     */
    private final NotificationService notificationService;

    /**
     * Время бездействия в минутах до первого напоминания.
     * <p>
     * Читается из конфигурационного свойства {@code app.timeout.scheduler}.
     * </p>
     * <p>
     * <b>Пример значения:</b> {@code 30} — напоминание будет отправлено через 30 минут неактивности.
     * </p>
     */
    @Value("${app.timeout.scheduler}")
    private int TIMEOUT_MINUTES;

    /**
     * Время ожидания (в минутах) после отправки напоминания, после которого сессия
     * будет передана менеджеру, если пользователь не ответил.
     * <p>
     * Читается из конфигурационного свойства {@code app.timeout.reminder-wait-minutes}.
     * </p>
     * <p>
     * <b>Пример значения:</b> {@code 5} — если пользователь не ответил в течение 5 минут
     * после напоминания, сессия переводится менеджеру.
     * </p>
     * <p>
     * Используется в {@link #checkTimeouts()} для определения момента передачи на менеджера.
     * </p>
     */
    @Value("${app.timeout.reminder-wait-minutes}")
    private int TIMEOUT_WAIT_MINUTES;

    /**
     * Проверяет сессии на тайм-аут и выполняет соответствующие действия.
     * <p>
     * Запускается каждые 60 секунд. Поток-обезопасен благодаря использованию
     * транзакций и Spring-синхронизации.
     * </p>
     *
     * <p><b>Алгоритм детально:</b></p>
     * <ul>
     *     <li>Получение списка сессий, у которых статус {@code AWAITING_INPUT} или
     *         {@code TIMEOUT_REMINDER} и последняя активность старше TIMEOUT_MINUTES.</li>
     *     <li>Для каждой сессии:
     *         <ul>
     *             <li>Если статус {@code TIMEOUT_REMINDER} и напоминание было отправлено
     *                 более {@code TIMEOUT_WAIT_MINUTES} минут назад:
     *                 <ul>
     *                     <li>Вызов {@link NotificationService#notifyManager} с причиной {@code "timeout"}.</li>
     *                     <li>Установка статуса {@code TO_MANAGER}.</li>
     *                     <li>Сохранение сессии через {@link SessionManager#save}.</li>
     *                     <li>Логирование на уровне INFO.</li>
     *                 </ul>
     *             </li>
     *             <li>Если статус {@code AWAITING_INPUT}:
     *                 <ul>
     *                     <li>Добавление системного сообщения-напоминания через
     *                         {@link SessionManager#addMessage}.</li>
     *                     <li>Установка статуса {@code TIMEOUT_REMINDER}.</li>
     *                     <li>Установка времени отправки напоминания (текущее время).</li>
     *                     <li>Сохранение сессии через {@link SessionManager#save}.</li>
     *                     <li>Логирование на уровне INFO.</li>
     *                 </ul>
     *             </li>
     *         </ul>
     *     </li>
     * </ul>
     *
     * <p><b>Исключения:</b></p>
     * Любое исключение, возникшее в процессе (ошибка сохранения, вызов notificationService),
     * приведёт к откату транзакции, и изменения не будут зафиксированы.
     * Исключения логируются на уровне выше (например, через глобальный обработчик).
     *
     * @see SessionManager#findExpired(int)
     * @see NotificationService#notifyManager(Session, String, String)
     * @see SessionManager#addMessage(Session, String, String)
     */
    @Scheduled(cron = "*/60 * * * * *")
    @Transactional
    public void checkTimeouts() {
        String correlationId = "scheduler-" + UUID.randomUUID();
        MDC.put("correlationId", correlationId);
        long startTime = System.currentTimeMillis();

        log.info("Запуск проверки тайм-аутов сессий (timeout={} минут)", TIMEOUT_MINUTES);

        try {
            List<Session> expired = sessionManager.findExpired(TIMEOUT_MINUTES);

            int total = expired.size();
            log.info("Найдено {} сессий, требующих обработки", total);

            if (expired.isEmpty()) {
                log.debug("Нет сессий для обработки");
                return;
            }

            int reminderCount = 0;
            int transferCount = 0;
            int skippedCount = 0;

            for (Session session : expired) {
                MDC.put("sessionId", session.getSessionKey());

                try {
                    if (session.getStatus() == SessionState.TIMEOUT_REMINDER && session.getReminderSentAt() != null) {
                        LocalDateTime reminderTime = session.getReminderSentAt();
                        if (LocalDateTime.now().isAfter(session.getReminderSentAt().plusMinutes(TIMEOUT_WAIT_MINUTES))) {
                            log.info("Сессия {} игнорирует напоминание (отправлено в {}), передаю менеджеру",
                                    session.getSessionKey(), reminderTime);

                            notificationService.notifyManager(session, "timeout", null);
                            session.setStatus(SessionState.TO_MANAGER);
                            sessionManager.save(session);
                            transferCount++;
                            log.info("Сессия {} успешно передана менеджеру после тайм-аута", session.getSessionKey());
                        } else {
                            log.debug("Сессия {} ожидает напоминание (прошло {} мин)",
                                    session.getSessionKey(),
                                    java.time.Duration.between(reminderTime, LocalDateTime.now()).toMinutes());
                            skippedCount++;
                        }
                        continue;
                    }

                    if (session.getStatus() == SessionState.AWAITING_INPUT) {
                        log.info("Сессия {} неактивна более {} минут, отправляю напоминание",
                                session.getSessionKey(), TIMEOUT_MINUTES);

                        sessionManager.addMessage(session, "assistant", "Я вижу, вы изучаете варианты. Напишите, если нужна помощь.");
                        session.setStatus(SessionState.TIMEOUT_REMINDER);
                        session.setReminderSentAt(LocalDateTime.now());
                        sessionManager.save(session);
                        reminderCount++;
                        log.info("Сессии {} отправлено напоминание о таймауте", session.getSessionKey());
                    } else {
                        log.warn("Сессия {} имеет неожиданный статус {} в списке истекших, пропускаю",
                                session.getSessionKey(), session.getStatus());
                        skippedCount++;
                    }
                } catch (Exception e) {
                    log.error("Ошибка при обработке сессии {}: {}", session.getSessionKey(), e.getMessage(), e);
                } finally {
                    MDC.remove("sessionId");
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Проверка тайм-аутов завершена: обработано {}, отправлено напоминаний {}, передано менеджеру {}, пропущено {}, duration={}ms",
                    total, reminderCount, transferCount, skippedCount, duration);
        } catch (Exception e) {
            log.error("Критическая ошибка при выполнении проверки тайм-аутов: {}", e.getMessage(), e);
        } finally {
            MDC.clear();
        }
    }
}