package com.hackathon.agent.infrastructure.ai;

import com.hackathon.agent.application.orchestrator.AgentOrchestrator;
import com.hackathon.agent.domain.model.Message;
import com.hackathon.agent.infrastructure.ai.scoring.GigaChatScoringService;
import com.hackathon.agent.infrastructure.ai.tools.GigaChatTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Клиент для взаимодействия с AI-моделью GigaChat через Spring AI.
 * <p>
 * Обеспечивает унифицированный интерфейс для вызова GigaChat в двух режимах:
 * <ul>
 *     <li><b>Диалоговый режим</b> — для основного чата с пользователем. Принимает
 *         системный промпт, историю сообщений и набор инструментов (tools).</li>
 *     <li><b>Режим scoring</b> — для оценки квартир по неметрическим параметрам.
 *         Принимает промпт и возвращает ответ в виде строки (ожидается JSON).</li>
 * </ul>
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Абстракция над {@link ChatClient} для упрощения вызовов и обработки ошибок.</li>
 *     <li>Преобразование внутренней модели {@link Message} в формат Spring AI
 *         ({@link org.springframework.ai.chat.messages.Message}).</li>
 *     <li>Единая обработка исключений и возврат понятных пользователю сообщений
 *         при сбоях (таймауты, сетевые ошибки, пустые ответы).</li>
 *     <li>Поддержка инструментов (function calling) для выполнения операций
 *         бизнес-логики из контекста AI-агента.</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * GigaChatClient client = ...;
 * List&lt;Message&gt; history = session.getDialogHistory();
 *
 * // Основной вызов с инструментами
 * String reply = client.call(
 *     "Ты — AI-агент по продаже квартир...",
 *     history,
 *     gigaChatTools
 * );
 *
 * // Вызов для scoring
 * String scoringResult = client.callForScoring(
 *     "Оцени квартиры: ..."
 * );
 * </pre>
 *
 * <p><b>Обработка ошибок:</b></p>
 * <ul>
 *     <li>Ошибки логируются на уровне ERROR.</li>
 *     <li>В диалоговом режиме при сбое возвращается дружелюбное сообщение,
 *         чтобы не прерывать диалог.</li>
 *     <li>В режиме scoring при ошибке возвращается пустой массив {@code "[]"},
 *         чтобы scoring мог продолжить работу с дефолтными значениями.</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see ChatClient
 * @see GigaChatTools
 * @see Message
 * @see AgentOrchestrator
 * @see GigaChatScoringService
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GigaChatClient {

    private final ChatClient chatClient;

    /**
     * Выполняет диалоговый вызов GigaChat с системным промптом, историей сообщений и инструментами.
     * <p>
     * Основной метод для взаимодействия с AI-моделью в чат-режиме. Преобразует историю
     * диалога из внутреннего формата {@link Message} в сообщения Spring AI,
     * добавляет системный промпт, подключает инструменты {@link GigaChatTools}
     * и выполняет запрос.
     * </p>
     *
     * <p><b>Алгоритм:</b></p>
     * <ol>
     *     <li>Преобразование списка {@code history} в массив
     *         {@link org.springframework.ai.chat.messages.Message}:
     *         <ul>
     *             <li>Роль {@code "user"} → {@link UserMessage}.</li>
     *             <li>Роль {@code "assistant"} → {@link AssistantMessage}.</li>
     *             <li>Остальные (в т.ч. {@code null}) → {@link UserMessage} (fallback).</li>
     *         </ul>
     *     </li>
     *     <li>Вызов {@link ChatClient} с системным промптом, сообщениями и инструментами.</li>
     *     <li>Извлечение текста ответа из {@link ChatResponse}.</li>
     *     <li>При пустом ответе или исключении возвращается стандартное сообщение об ошибке,
     *         а исключение логируется.</li>
     * </ol>
     *
     * <p><b>Параметры:</b></p>
     * <ul>
     *     <li>{@code systemPrompt} — системная инструкция для AI, задающая роль и правила поведения.</li>
     *     <li>{@code history} — история диалога (список сообщений от пользователя и ассистента).
     *         Может быть {@code null} или пустым — тогда вызывается без истории.</li>
     *     <li>{@code tools} — объект с методами, аннотированными {@code @Tool},
     *         которые могут быть вызваны моделью.</li>
     * </ul>
     *
     * <p><b>Возвращаемое значение:</b></p>
     * Текстовый ответ от AI-модели или сообщение об ошибке.
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * String reply = client.call(
     *     "Ты — помощник по подбору квартир. Отвечай вежливо.",
     *     session.getDialogHistory(),
     *     gigaChatTools
     * );
     * </pre>
     *
     * @param systemPrompt системная инструкция (не {@code null}, но может быть пустой)
     * @param history      история диалога (может быть {@code null} или пустой)
     * @param tools        инструменты для AI (не {@code null})
     * @return ответ модели в виде строки (всегда не {@code null})
     */
    public String call(String systemPrompt, List<Message> history, GigaChatTools tools) {
        if (log.isDebugEnabled()) {
            log.debug("GigaChat call: systemPrompt length={}, history size={}, tools available",
                    systemPrompt != null ? systemPrompt.length() : 0,
                    history != null ? history.size() : 0);
        }

        long startTime = System.currentTimeMillis();
        try {
            var springMessages = history.stream()
                    .map(msg -> {
                        if ("user".equals(msg.getRole())) {
                            return new UserMessage(msg.getText());
                        } else if ("assistant".equals(msg.getRole())) {
                            return new AssistantMessage(msg.getText());
                        } else {
                            return new UserMessage(msg.getText());
                        }
                    })
                    .toList();

            ChatResponse response = chatClient.prompt()
                    .system(systemPrompt)
                    .messages(springMessages.toArray(new org.springframework.ai.chat.messages.Message[0]))
                    .tools(tools)
                    .call()
                    .chatResponse();

            long duration = System.currentTimeMillis() - startTime;
            if (duration > 5000) {
                log.warn("GigaChat call took {}ms (threshold 5000ms)", duration);
            } else {
                log.debug("GigaChat call completed in {}ms", duration);
            }

            if (response == null || response.getResult() == null) {
                log.warn("GigaChat вернул пустой ответ");
                return "Извините, я не смог обработать ваш запрос.";
            }

            String content = response.getResult().getOutput().getText();
            if (content == null || content.isBlank()) {
                log.warn("GigaChat вернул пустой текст ответа");
                return "Ответ получен, но он пустой.";
            }
            log.debug("GigaChat response length: {}", content.length());

            return content;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Ошибка при вызове GigaChat (duration={}ms): {}", duration, e.getMessage(), e);
            return "Произошла ошибка при обращении к сервису. Попробуйте позже.";
        }
    }

    /**
     * Выполняет вызов GigaChat для задачи scoring (оценки квартир).
     * <p>
     * Упрощённый метод, предназначенный для пакетной оценки квартир по неметрическим
     * параметрам. Принимает готовый промпт (с описанием квартир и инструкцией),
     * отправляет его в GigaChat и возвращает ответ в виде строки.
     * </p>
     *
     * <p><b>Особенности:</b></p>
     * <ul>
     *     <li>Не использует системный промпт и историю — только пользовательское сообщение.</li>
     *     <li>Ожидается, что ответ будет в формате JSON-массива чисел (например, {@code [85, 70, 92]}).</li>
     *     <li>При ошибке возвращается строка {@code "[]"}, чтобы вызывающий код мог
     *         корректно обработать ситуацию (например, присвоить дефолтные оценки).</li>
     * </ul>
     *
     * <p><b>Параметры:</b></p>
     * <ul>
     *     <li>{@code prompt} — полный текст запроса к модели, содержащий инструкции
     *         и данные о квартирах.</li>
     * </ul>
     *
     * <p><b>Возвращаемое значение:</b></p>
     * Ответ модели в виде строки (обычно JSON-массив), либо {@code "[]"} в случае ошибки.
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * String prompt = "Оцени квартиры: ID 1: этаж 5, вид парк, парковка есть; ...";
     * String response = client.callForScoring(prompt);
     * // response = "[90, 75, 88]"
     * </pre>
     *
     * @param prompt промпт для модели (не {@code null}, не пустой)
     * @return ответ модели в виде строки (всегда не {@code null})
     */
    public String callForScoring(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            log.warn("Вызов callForScoring с пустым промптом");
            return "[]";
        }
        log.debug("Scoring prompt length: {}", prompt.length());

        long startTime = System.currentTimeMillis();

        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            long duration = System.currentTimeMillis() - startTime;
            if (duration > 3000) {
                log.warn("Scoring call took {}ms (threshold 3000ms)", duration);
            } else {
                log.debug("Scoring call completed in {}ms", duration);
            }
            if (result == null || result.isBlank()) {
                log.warn("Scoring вернул пустой ответ, возвращаем '[]'");
                return "[]";
            }
            log.debug("Scoring response length: {}", result.length());

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Ошибка при вызове scoring (duration={}ms): {}", duration, e.getMessage(), e);
            return "[]";
        }
    }
}