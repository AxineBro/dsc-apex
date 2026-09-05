package com.hackathon.agent.config;

import com.hackathon.agent.application.orchestrator.AgentOrchestrator;
import com.hackathon.agent.infrastructure.ai.GigaChatClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс для настройки клиента GigaChat (Spring AI).
 * <p>
 * Обеспечивает создание и настройку бина {@link ChatClient}, который используется
 * для взаимодействия с AI-моделью GigaChat через стандартный интерфейс Spring AI.
 * </p>
 *
 * <p><b>Основные обязанности:</b></p>
 * <ul>
 *     <li>Создание экземпляра {@link ChatClient} с использованием билдера,
 *         автоматически сконфигурированного Spring Boot (через автоконфигурацию Spring AI).</li>
 *     <li>Интеграция с остальными компонентами системы через внедрение зависимостей
 *         (билдер предоставляется Spring-контекстом).</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * Созданный бин {@code ChatClient} инжектируется в сервисы, такие как
 * {@link GigaChatClient} или {@link AgentOrchestrator}, для выполнения вызовов
 * к AI-модели. Конфигурация билдера (например, базовый URL, API-ключи, тайм-ауты)
 * обычно задаётся через внешние свойства (application.yml) благодаря автоконфигурации
 * Spring AI.
 *
 * <p><b>Зависимости:</b></p>
 * <ul>
 *     <li>{@link ChatClient.Builder} — предоставляется автоматически Spring Boot
 *         при наличии стартера Spring AI для GigaChat.</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see ChatClient
 * @see GigaChatClient
 * @see AgentOrchestrator
 */
@Configuration
public class GigaChatConfig {

    /**
     * Создаёт бин {@link ChatClient}, используя стандартный билдер.
     * <p>
     * Метод использует готовый экземпляр {@link ChatClient.Builder}, который
     * уже сконфигурирован на основе свойств приложения (например, параметры
     * подключения к GigaChat). Дополнительная кастомизация билдера может быть
     * применена при необходимости (например, добавление перехватчиков, тайм-аутов).
     * </p>
     *
     * <p><b>Возвращаемое значение:</b></p>
     * Готовый к использованию экземпляр {@link ChatClient}, который инкапсулирует
     * логику взаимодействия с AI-моделью. Этот бин является синглтоном
     * и будет использоваться во всех компонентах, требующих доступа к GigaChat.
     * </p>
     *
     * @param builder автоматически внедряемый билдер, настроенный через Spring AI
     * @return экземпляр {@link ChatClient}
     * @see ChatClient.Builder#build()
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}