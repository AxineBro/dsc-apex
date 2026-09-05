package com.hackathon.agent.api.dto.response;

import com.hackathon.agent.api.controller.ChatController;
import com.hackathon.agent.api.dto.request.ChatRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO (Data Transfer Object) для ответа на запрос инициализации новой чат-сессии.
 * <p>
 * Используется в эндпоинте {@link ChatController#initSession} для возврата
 * клиенту свежего идентификатора сессии. Этот идентификатор должен быть передан
 * в заголовке {@code X-Session-Id} при всех последующих вызовах
 * {@link ChatController#sendMessage} для поддержания контекста диалога.
 * </p>
 *
 * <p><b>Пример JSON-ответа (200 OK):</b></p>
 * <pre>
 * {
 *     "sessionId": "550e8400-e29b-41d4-a716-446655440000"
 * }
 * </pre>
 *
 * <p><b>Примечание:</b></p>
 * Инициализация сессии не является обязательной — {@link ChatController#sendMessage}
 * автоматически создаёт новую сессию при отсутствии валидного {@code sessionId}.
 * Однако данный эндпоинт может быть полезен для предварительного получения
 * идентификатора и его сохранения на клиентской стороне.
 *
 * @author Axine
 * @since 1.0.0
 * @see ChatController#initSession()
 * @see ChatController#sendMessage(String, ChatRequest)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionInitResponse {

    /**
     * Уникальный идентификатор созданной сессии.
     * <p>
     * Генерируется с помощью {@link UUID#randomUUID()} и имеет стандартный
     * строковый формат (например, {@code 550e8400-e29b-41d4-a716-446655440000}).
     * Этот идентификатор должен быть передан клиентом в каждом последующем
     * запросе к {@link ChatController#sendMessage} в заголовке {@code X-Session-Id}.
     * </p>
     *
     * @see ChatController#initSession()
     */
    private String sessionId;
}