package com.hackathon.agent.api.dto.response;

import com.hackathon.agent.api.controller.ChatController;
import com.hackathon.agent.api.dto.request.ChatRequest;
import com.hackathon.agent.application.facade.ChatFacade;
import com.hackathon.agent.domain.model.SessionState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO (Data Transfer Object) для ответа на запрос отправки сообщения в чат.
 * <p>
 * Содержит текст ответа бота и идентификатор текущей сессии, который клиент
 * должен использовать в последующих запросах (в заголовке {@code X-Session-Id}).
 * Возвращается эндпоинтом {@link ChatController#sendMessage}.
 * </p>
 *
 * <p><b>Пример JSON-ответа (200 OK):</b></p>
 * <pre>
 * {
 *     "reply": "Привет! Чем могу помочь?",
 *     "sessionId": "550e8400-e29b-41d4-a716-446655440000"
 * }
 * </pre>
 *
 * <p><b>Особенности:</b></p>
 * <ul>
 *     <li>Поле {@code sessionId} всегда присутствует, даже если клиент не передал его в запросе
 *         (в этом случае генерируется новый UUID).</li>
 *     <li>Поле {@code reply} содержит сгенерированный ботом ответ, может быть пустым
 *         только в исключительных случаях (обрабатывается на уровне бизнес-логики).</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see ChatController
 * @see ChatRequest
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    /**
     * Текстовый ответ бота на сообщение пользователя.
     * <p>
     * Содержит сгенерированное сообщение, которое должно быть отображено пользователю.
     * Может быть пустой строкой, если бот не сгенерировал ответ (например, при ошибке),
     * но в штатной ситуации содержит осмысленный текст.
     * </p>
     *
     * @see ChatFacade#processMessage(String, String)
     */
    private String reply;

    /**
     * Идентификатор сессии, в рамках которой был обработан запрос.
     * <p>
     * Всегда возвращается клиенту, даже если был передан в запросе или сгенерирован автоматически.
     * Клиент обязан сохранять этот идентификатор и передавать его в заголовке
     * {@code X-Session-Id} при всех последующих вызовах {@link ChatController#sendMessage}
     * для сохранения контекста диалога.
     * </p>
     * <p>
     * Формат: стандартный UUID (например, {@code 550e8400-e29b-41d4-a716-446655440000}).
     * </p>
     *
     * @see UUID#randomUUID()
     */
    private String sessionId;

    /**
     * Текущее состояние диалога.
     * <p>
     * Отражает, на каком этапе жизненного цикла находится сессия после обработки
     * текущего сообщения: активный диалог, показ списка квартир, готовое предложение,
     * передача менеджеру или завершение.
     * </p>
     * <p>
     * Значение возвращается в виде строкового кода (см. {@link SessionState#getCode()}).
     * Клиент может использовать его для изменения поведения интерфейса
     * (например, отключить ввод при {@code TO_MANAGER} или {@code FINISHED},
     * показать кнопки выбора при {@code SHOWING_LIST}).
     * </p>
     *
     * @see SessionState
     * @see SessionState#fromCode(String)
     */
    private String state;

    /**
     * Признак того, что диалог был передан живому менеджеру.
     * <p>
     * Булево значение: {@code true} — бот инициировал или выполнил передачу диалога
     * сотруднику (оператору, менеджеру); {@code false} — диалог продолжает вести бот.
     * </p>
     * <p>
     * Если значение {@code true}, клиенту следует ожидать подключения менеджера
     * и, возможно, изменить интерфейс (например, показать уведомление).
     * </p>
     */
    private boolean transferredToManager;

    /**
     * Причина передачи диалога менеджеру.
     * <p>
     * Строковое описание причины, по которой бот передал диалог человеку.
     * Заполняется только если {@link #transferredToManager} равно {@code true}.
     * Примеры: {@code "user_request"}, {@code "low_confidence"}, {@code "complex_question"},
     * {@code "out_of_scope"} и т.п.
     * </p>
     * <p>
     * Может быть {@code null}, если передача не выполнялась или причина не указана.
     * </p>
     */
    private String managerReason;

    /**
     * Список вложений, прикреплённых к ответу бота.
     * <p>
     * Содержит объекты {@link ChatAttachment}, которые могут представлять собой
     * изображения, документы, ссылки или другие файлы, отправляемые пользователю
     * вместе с текстовым ответом.
     * </p>
     * <p>
     * Если вложений нет, возвращается пустой список (не {@code null}).
     * </p>
     *
     * @see ChatAttachment
     */
    private List<ChatAttachment> attachments;

    public ChatResponse(String reply, String sessionId) {
        this(reply, sessionId, null, false, null, List.of());
    }
}