package com.hackathon.agent.domain.model;

import com.hackathon.agent.application.orchestrator.SessionManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Модель данных, представляющая одно сообщение в истории диалога.
 * <p>
 * Содержит информацию о роли отправителя, тексте сообщения и временной метке.
 * Используется для хранения истории взаимодействия между пользователем и ассистентом
 * в рамках сессии чата.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Хранение каждого отдельного сообщения в диалоговой сессии.</li>
 *     <li>Предоставление контекста для AI-модели (история передаётся в запросах к GigaChat).</li>
 *     <li>Сериализация и десериализация в JSON для передачи между слоями и внешними системами.</li>
 * </ul>
 *
 * <p><b>Роли отправителей:</b></p>
 * <ul>
 *     <li>{@code "user"} — сообщение от пользователя.</li>
 *     <li>{@code "assistant"} — ответ от AI-ассистента (GigaChat).</li>
 * </ul>
 * <p>
 * Допустимо использование других ролей (например, {@code "system"}), если это требуется
 * бизнес-логикой, но в текущей реализации используются только {@code "user"} и {@code "assistant"}.
 * </p>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * // Создание сообщения пользователя
 * Message userMessage = new Message("user", "Привет, ищу квартиру", LocalDateTime.now());
 *
 * // Добавление в историю сессии
 * session.getMessages().add(userMessage);
 *
 * // Получение истории для AI
 * List&lt;Message&gt; history = session.getDialogHistory();
 * String prompt = history.stream()
 *         .map(m -> m.getRole() + ": " + m.getText())
 *         .collect(Collectors.joining("\n"));
 * </pre>
 *
 * <p><b>Связь с сессией:</b></p>
 * Объекты {@code Message} хранятся в коллекции внутри {@link Session} и управляются
 * через {@link SessionManager#addMessage}. При сохранении сессии сообщения
 * сохраняются вместе с ней.
 *
 * <p><b>Примечание по временной метке:</b></p>
 * Поле {@code timestamp} рекомендуется устанавливать при создании сообщения
 * (например, {@code LocalDateTime.now()}), чтобы сохранять хронологию диалога.
 * Это важно для отслеживания последовательности и временных интервалов.
 *
 * @author Axine
 * @since 1.0.0
 * @see Session
 * @see SessionManager#addMessage
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {

    /**
     * Роль отправителя сообщения.
     * <p>
     * Строковое значение, определяющее, кто отправил сообщение:
     * <ul>
     *     <li>{@code "user"} — пользователь.</li>
     *     <li>{@code "assistant"} — AI-ассистент.</li>
     * </ul>
     * <p>
     * Используется для правильного форматирования истории при передаче в AI-модель
     * и для отображения в интерфейсе.
     * </p>
     */
    private String role;

    /**
     * Текстовое содержимое сообщения.
     * <p>
     * Содержит непосредственно текст, отправленный пользователем или сгенерированный
     * ассистентом. Может быть пустым в исключительных случаях, но обычно содержит
     * осмысленное сообщение.
     * </p>
     */
    private String text;

    /**
     * Временная метка создания сообщения.
     * <p>
     * Дата и время, когда сообщение было добавлено в историю.
     * Используется для сортировки, анализа времени отклика и логирования.
     * Рекомендуется устанавливать при создании (например, {@code LocalDateTime.now()}).
     * </p>
     */
    private LocalDateTime timestamp;
}