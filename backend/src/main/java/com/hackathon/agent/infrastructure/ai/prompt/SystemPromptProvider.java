package com.hackathon.agent.infrastructure.ai.prompt;

import com.hackathon.agent.application.orchestrator.AgentOrchestrator;
import com.hackathon.agent.infrastructure.ai.GigaChatClient;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Провайдер системного промпта для AI-модели GigaChat.
 * <p>
 * Загружает системную инструкцию (prompt) из внешнего файла при запуске приложения
 * и предоставляет её для использования в {@link AgentOrchestrator}. Системный промпт
 * задаёт роль, поведение, ограничения и инструкции для AI-ассистента, определяя
 * его стиль общения и логику обработки запросов.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Централизованное хранение и загрузка системного промпта из файла.</li>
 *     <li>Обеспечение гибкости: изменение промпта без перекомпиляции кода
 *         (достаточно заменить файл или изменить путь в конфигурации).</li>
 *     <li>Загрузка промпта при старте приложения (однократно) с использованием
 *         {@link PostConstruct} для гарантии готовности компонента.</li>
 *     <li>Предоставление дефолтного промпта в случае ошибки загрузки, чтобы
 *         система оставалась работоспособной.</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * SystemPromptProvider promptProvider = ...;
 * String systemPrompt = promptProvider.getPrompt();
 * // Передаётся в GigaChatClient.call() как системная инструкция
 * </pre>
 *
 * <p><b>Конфигурация:</b></p>
 * Путь к файлу промпта задаётся в application.properties или application.yml:
 * <pre>
 * app.prompt.path=prompts/system_prompt.txt
 * </pre>
 * <p>
 * Файл должен находиться в classpath (например, в {@code src/main/resources/prompts/}).
 * Рекомендуемый формат — plain text (UTF-8).
 * </p>
 *
 * <p><b>Обработка ошибок:</b></p>
 * Если файл не найден или не может быть прочитан, логируется ошибка,
 * и используется дефолтный промпт, чтобы не останавливать работу приложения.
 * Это обеспечивает отказоустойчивость и позволяет системе работать даже
 * при проблемах с конфигурацией.
 *
 * @author Axine
 * @since 1.0.0
 * @see AgentOrchestrator
 * @see GigaChatClient
 */
@Component
@Slf4j
public class SystemPromptProvider {

    /**
     * Загруженный системный промпт.
     * <p>
     * Устанавливается в методе {@link #loadPrompt()}, вызываемом после инициализации
     * компонента. Содержит полный текст инструкций для AI-модели.
     * </p>
     */
    @Getter
    private String prompt;

    /**
     * Путь к файлу с системным промптом в classpath.
     * <p>
     * Читается из конфигурационного свойства {@code app.prompt.path}.
     * </p>
     * <p>
     * <b>Пример значения:</b> {@code "prompts/system_prompt.txt"}
     * </p>
     */
    @Value("${app.prompt.path}")
    private String promptPath;

    /**
     * Загружает системный промпт из файла при инициализации компонента.
     * <p>
     * Аннотирована {@link PostConstruct}, поэтому вызывается автоматически
     * после внедрения зависимостей и до первого использования компонента.
     * </p>
     *
     * <p><b>Алгоритм:</b></p>
     * <ol>
     *     <li>Создаёт ресурс {@link ClassPathResource} по пути {@link #promptPath}.</li>
     *     <li>Читает содержимое файла в строку с кодировкой UTF-8.</li>
     *     <li>Сохраняет строку в поле {@link #prompt}.</li>
     *     <li>Логирует успешную загрузку с длиной промпта.</li>
     *     <li>В случае ошибки (IOException) логирует ошибку и устанавливает
     *         дефолтное значение промпта, чтобы система могла продолжить работу.</li>
     * </ol>
     *
     * <p><b>Дефолтный промпт:</b></p>
     * {@code "Ты — AI-агент по продаже квартир. Следуй инструкциям."}
     *
     * @throws IllegalStateException если промпт не был загружен даже после вызова
     *         (практически не возникает, т.к. всегда устанавливается дефолтное значение)
     */
    @PostConstruct
    public void loadPrompt() {
        long startTime = System.currentTimeMillis();
        try {
            var resource = new ClassPathResource(promptPath);
            prompt = resource.getContentAsString(StandardCharsets.UTF_8);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Системный промпт загружен из '{}', длина: {} символов, duration={}ms",
                    promptPath, prompt.length(), duration);
            if (log.isDebugEnabled()) {
                log.debug("Содержимое системного промпта:\n{}", prompt);
            }
        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Не удалось загрузить системный промпт из '{}' (duration={}ms): {}",
                    promptPath, duration, e.getMessage(), e);
            prompt = "Ты — AI-агент по продаже квартир. Следуй инструкциям.";
            log.warn("Установлен дефолтный системный промпт, длина: {} символов", prompt.length());
        }
    }
}