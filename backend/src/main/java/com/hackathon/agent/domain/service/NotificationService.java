package com.hackathon.agent.domain.service;

import com.hackathon.agent.application.orchestrator.AgentOrchestrator;
import com.hackathon.agent.domain.model.ManagerTask;
import com.hackathon.agent.domain.model.Session;

/**
 * Сервис для уведомления менеджеров о необходимости вмешательства в диалог.
 * <p>
 * Используется для эскалации запроса от AI-ассистента к человеку-менеджеру
 * в случаях, когда система не может обработать запрос пользователя
 * (например, ошибка, недостаток данных, сложный кейс или явный запрос клиента).
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Создание задачи (тикета) для менеджера на основе данных сессии.</li>
 *     <li>Сохранение уведомления в базе данных для последующей обработки.</li>
 *     <li>Логирование события для мониторинга и аудита.</li>
 * </ul>
 *
 * <p><b>Текущая реализация (заглушка):</b></p>
 * На данный момент сервис является заглушкой и выполняет:
 * <ul>
 *     <li>Сохранение информации о переводе в БД (в таблицу {@code manager_tasks}).</li>
 *     <li>Логирование на уровне {@code INFO} с деталями сессии и причины.</li>
 * </ul>
 * <p>
 * В будущем может быть расширено для отправки реальных уведомлений
 * (email, SMS, push-уведомления, интеграция с CRM) без изменения интерфейса.
 * </p>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * // В AgentOrchestrator при ошибке
 * notificationService.notifyManager(
 *     session,
 *     "error_NullPointerException",
 *     session.getClientPhone()
 * );
 *
 * // При явном запросе пользователя
 * if (userMessage.contains("позвать менеджера")) {
 *     notificationService.notifyManager(
 *         session,
 *         "user_requested_manager",
 *         session.getClientPhone()
 *     );
 * }
 * </pre>
 *
 * <p><b>Связь с другими компонентами:</b></p>
 * <ul>
 *     <li>Создаёт объекты {@link ManagerTask} для хранения в БД.</li>
 *     <li>Интегрируется с системой уведомлений и CRM.</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see ManagerTask
 * @see AgentOrchestrator
 * @see Session
 */
public interface NotificationService {

    /**
     * Отправляет уведомление менеджеру о необходимости обработки диалога.
     * <p>
     * Создаёт задачу для менеджера на основе переданной сессии, причины
     * и контактного телефона клиента. Сохраняет информацию в базу данных
     * и логирует событие. В будущем планируется отправка реальных уведомлений.
     * </p>
     *
     * <p><b>Действия в текущей реализации (заглушка):</b></p>
     * <ol>
     *     <li>Формирование объекта {@link ManagerTask} на основе данных сессии
     *         и переданных параметров.</li>
     *     <li>Сохранение задачи в БД через репозиторий.</li>
     *     <li>Логирование на уровне {@code INFO} с ключевыми полями
     *         (sessionKey, reason, phone, timestamp).</li>
     *     <li>Может отправлять системное событие в очередь для дальнейшей обработки.</li>
     * </ol>
     *
     * <p><b>Параметры:</b></p>
     * <ul>
     *     <li>{@code session} — объект сессии, содержащий всю историю диалога,
     *         фильтры, ранжированный список и статус. Не должен быть {@code null}.</li>
     *     <li>{@code reason} — причина перевода менеджеру (например, код ошибки,
     *         {@code "user_requested"}, {@code "no_apartments_found"}). Не {@code null}, не пусто.</li>
     *     <li>{@code phone} — номер телефона клиента для связи с ним. Может быть {@code null}
     *         или пустым, если номер не был предоставлен.</li>
     * </ul>
     *
     * <p><b>Пример вызова:</b></p>
     * <pre>
     * notificationService.notifyManager(
     *     session,
     *     "error_TimeoutException",
     *     "+79001234567"
     * );
     * </pre>
     *
     * @param session сессия, требующая вмешательства менеджера (не {@code null})
     * @param reason  причина перевода (не {@code null}, не пусто)
     * @param phone   контактный телефон клиента (может быть {@code null})
     * @throws IllegalArgumentException если {@code session} равен {@code null}
     *         или {@code reason} пустой
     * @throws RuntimeException если сохранение в БД завершилось ошибкой
     *         (реализация может обрабатывать и логировать, но пробрасывать выше)
     */
    void notifyManager(Session session, String reason, String phone);
}