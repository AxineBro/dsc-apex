package com.hackathon.agent.api.dto.response;

import com.hackathon.agent.api.controller.ChatController;
import com.hackathon.agent.api.dto.request.ChatRequest;
import com.hackathon.agent.application.facade.ChatFacade;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}