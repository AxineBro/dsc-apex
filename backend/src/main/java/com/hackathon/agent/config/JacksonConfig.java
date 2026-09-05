package com.hackathon.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс для настройки Jackson {@link ObjectMapper}.
 * <p>
 * Предоставляет кастомизированный экземпляр {@link ObjectMapper} для сериализации
 * и десериализации JSON-данных во всём приложении. Используется для тонкой настройки
 * поведения Jackson (форматы дат, обработка неизвестных свойств, настройки инъекций и т.д.)
 * </p>
 *
 * <p><b>Основные обязанности:</b></p>
 * <ul>
 *     <li>Создание единственного экземпляра {@link ObjectMapper} с необходимыми
 *         настройками.</li>
 *     <li>Потенциальное расширение конфигурации (добавление модулей, регистрация
 *         десериализаторов, настройка форматтеров дат).</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * Бин {@code objectMapper} автоматически используется Spring для:
 * <ul>
 *     <li>Сериализации/десериализации в REST-контроллерах (аргументы {@code @RequestBody},
 *         {@code @ResponseBody}).</li>
 *     <li>Работы с кэшами, очередями сообщений и другими компонентами, где требуется JSON.</li>
 * </ul>
 *
 * <p><b>Кастомизация:</b></p>
 * В текущей реализации создаётся стандартный объект с настройками по умолчанию.
 * Для расширения можно добавить, например:
 * <pre>
 * objectMapper
 *     .registerModule(new JavaTimeModule())
 *     .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
 *     .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
 * </pre>
 *
 * <p><b>Примечания:</b></p>
 * <ul>
 *     <li>Если в приложении требуется особая конфигурация (например, работа с LocalDateTime,
 *         игнорирование неизвестных полей), она добавляется именно здесь.</li>
 *     <li>Если бин не объявлен явно, Spring Boot создаёт свой {@link ObjectMapper}
 *         с настройками по умолчанию. Явное объявление даёт полный контроль.</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see ObjectMapper
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor
 */
@Configuration
public class JacksonConfig {

    /**
     * Создаёт экземпляр {@link ObjectMapper} с настройками по умолчанию.
     * <p>
     * В текущей реализации возвращается новый объект со стандартной конфигурацией.
     * Для более сложных требований можно дополнить метод регистрацией модулей
     * (например, для работы с Java 8 Date/Time API) или настройкой сериализации.
     * </p>
     *
     * <p><b>Пример расширенной настройки:</b></p>
     * <pre>
     * ObjectMapper mapper = new ObjectMapper();
     * mapper.registerModule(new JavaTimeModule());
     * mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
     * mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
     * return mapper;
     * </pre>
     *
     * @return сконфигурированный экземпляр {@link ObjectMapper}
     * @see ObjectMapper#ObjectMapper()
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}