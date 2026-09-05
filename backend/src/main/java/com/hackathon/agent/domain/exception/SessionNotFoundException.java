package com.hackathon.agent.domain.exception;

import com.hackathon.agent.api.controller.ChatController;
import com.hackathon.agent.application.orchestrator.SessionManager;

/**
 * Исключение, выбрасываемое при попытке обращения к несуществующей сессии.
 * <p>
 * Является частным случаем {@link DomainException} и используется в бизнес-логике
 * для сигнализации о том, что запрошенная сессия с указанным ключом отсутствует
 * в системе (в памяти, кэше или базе данных).
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Инкапсуляция ошибки "сессия не найдена" на уровне домена.</li>
 *     <li>Обеспечение контролируемой реакции при попытке продолжить диалог
 *         с невалидным идентификатором сессии (например, истёкшая или удалённая сессия).</li>
 *     <li>Упрощение обработки ошибок в сервисном слое и контроллерах за счёт
 *         использования иерархии исключений.</li>
 * </ul>
 *
 * <p><b>Типичные сценарии использования:</b></p>
 * <pre>
 * // В менеджере сессий при получении по ключу
 * Session session = sessionCache.get(sessionKey);
 * if (session == null) {
 *     throw new SessionNotFoundException(sessionKey);
 * }
 * return session;
 *
 * // При валидации заголовка X-Session-Id в контроллере
 * if (!sessionManager.exists(sessionId)) {
 *     throw new SessionNotFoundException(sessionId);
 * }
 * </pre>
 *
 * <p><b>Обработка на уровне контроллера:</b></p>
 * Рекомендуется перехватывать данное исключение в глобальном обработчике
 * (например, {@code @ControllerAdvice}) и возвращать клиенту HTTP-статус
 * {@code 404 Not Found} с телом ответа, содержащим сообщение об ошибке.
 * Это даст клиенту понять, что переданный идентификатор сессии недействителен,
 * и необходимо инициализировать новую сессию через эндпоинт {@code /api/v1/chat/init}.
 *
 * <p><b>Отличие от автоматического создания сессии:</b></p>
 * В {@link ChatController#sendMessage} при отсутствии заголовка {@code X-Session-Id}
 * автоматически создаётся новая сессия. Данное исключение выбрасывается только в случаях,
 * когда сессия явно запрашивается (например, {@code getOrCreate} может вернуть {@code null}
 * или при явной проверке существования).
 *
 * @author Axine
 * @since 1.0.0
 * @see DomainException
 * @see SessionManager#getOrCreate(String)
 */
public class SessionNotFoundException extends DomainException {

    /**
     * Создаёт исключение для несуществующей сессии по её ключу.
     * <p>
     * Формирует сообщение вида:
     * {@code "Сессия с ключом <sessionKey> не найдена"},
     * где {@code sessionKey} — переданный идентификатор.
     * Это сообщение будет передано клиенту после обработки исключения,
     * чтобы указать на невалидный идентификатор.
     * </p>
     *
     * @param sessionKey ключ сессии, которая не была найдена
     */
    public SessionNotFoundException(String sessionKey) {
        super("Сессия с ключом " + sessionKey + " не найдена");
    }
}