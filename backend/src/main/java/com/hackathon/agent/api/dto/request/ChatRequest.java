package com.hackathon.agent.api.dto.request;

import com.hackathon.agent.api.controller.ChatController;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) для входящего запроса на отправку сообщения в чат.
 * <p>
 * Используется в эндпоинте {@link ChatController#sendMessage} для приёма и валидации
 * текста сообщения от пользователя. Все поля проходят автоматическую валидацию
 * с помощью аннотаций из пакета {@code javax.validation}.
 * </p>
 *
 * <p><b>Пример JSON-запроса:</b></p>
 * <pre>
 * {
 *     "message": "Привет. Как дела?"
 * }
 * </pre>
 *
 * <p><b>Ограничения:</b></p>
 * <ul>
 *     <li>Поле {@code message} обязательно для заполнения (не {@code null}, не пустое).</li>
 *     <li>Максимальная длина сообщения — 2000 символов.</li>
 * </ul>
 *
 * <p>Ошибки валидации обрабатываются глобальным обработчиком исключений
 * (например, {@code @ControllerAdvice}) и возвращаются в виде структурированного
 * ответа с кодом 400 Bad Request.</p>
 *
 * @author Axine
 * @since 1.0.0
 * @see ChatController
 * @see jakarta.validation.Valid
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    /**
     * Текст сообщения пользователя.
     * <p>
     * Обязательное поле. Не может быть {@code null}, пустой строкой или состоять
     * только из пробелов. Максимальная длина — 2000 символов (включая пробелы).
     * </p>
     * <p>
     * В случае нарушения ограничений выбрасывается исключение
     * с соответствующим сообщением об ошибке.
     * </p>
     *
     * @see NotBlank
     * @see Size
     */
    @NotBlank(message = "Сообщение не может быть пустым")
    @Size(max = 2000, message = "Сообщение не должно превышать 2000 символов")
    private String message;
}