package com.hackathon.agent.domain.exception;

import com.hackathon.agent.domain.model.SessionState;

/**
 * Исключение, выбрасываемое при попытке преобразования неизвестного строкового кода
 * в значение перечисления {@link SessionState}.
 * <p>
 * Является частным случаем {@link DomainException} и используется в бизнес-логике
 * для сигнализации о том, что переданный извне (например, из базы данных или API)
 * строковый статус сессии не соответствует ни одному из допустимых значений
 * перечисления {@link SessionState}.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Инкапсуляция ошибки "неизвестный статус" на уровне домена.</li>
 *     <li>Обеспечение контролируемой реакции при десериализации статуса сессии
 *         из строковых представлений (например, из JSON-поля БД или запроса).</li>
 *     <li>Упрощение обработки ошибок в мапперах и сервисах за счёт
 *         использования иерархии исключений.</li>
 * </ul>
 *
 * <p><b>Типичные сценарии использования:</b></p>
 * <pre>
 * // В методе SessionState.fromCode(String)
 * public static SessionState fromCode(String code) {
 *     for (SessionState state : values()) {
 *         if (state.getCode().equals(code)) {
 *             return state;
 *         }
 *     }
 *     throw new UnknownSessionStateException(code);
 * }
 *
 * // При загрузке сессии из БД (в SessionMapper)
 * try {
 *     SessionState status = SessionState.valueOf(entity.getStatus());
 * } catch (IllegalArgumentException e) {
 *     throw new UnknownSessionStateException(entity.getStatus());
 * }
 * </pre>
 *
 * <p><b>Обработка на уровне контроллера:</b></p>
 * Рекомендуется перехватывать данное исключение в глобальном обработчике
 * (например, {@code @ControllerAdvice}) и возвращать клиенту HTTP-статус
 * {@code 400 Bad Request} с телом ответа, содержащим сообщение об ошибке.
 * Это информирует клиента о том, что переданный статус некорректен, и позволяет
 * избежать внутренних ошибок сервера.
 *
 * <p><b>Связь с другими классами:</b></p>
 * <ul>
 *     <li>Используется в {@link com.hackathon.agent.infrastructure.persistence.mapper.SessionMapper}
 *         при преобразовании {@code SessionEntity} в {@link com.hackathon.agent.domain.model.Session}.</li>
 *     <li>Может использоваться в любом месте, где производится парсинг статуса из строки.</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see DomainException
 * @see SessionState
 * @see com.hackathon.agent.infrastructure.persistence.mapper.SessionMapper#toDomain
 */
public class UnknownSessionStateException extends DomainException {

    /**
     * Создаёт исключение для неизвестного строкового кода статуса сессии.
     * <p>
     * Формирует сообщение вида:
     * {@code "Неизвестный статус сессии: <code>"},
     * где {@code code} — переданный строковый идентификатор.
     * Это сообщение будет передано клиенту после обработки исключения,
     * чтобы указать на некорректное значение статуса.
     * </p>
     *
     * @param code строковый код статуса, который не удалось распознать
     *             (не {@code null}, не пустой)
     */
    public UnknownSessionStateException(String code) {
        super("Неизвестный статус сессии: " + code);
    }
}