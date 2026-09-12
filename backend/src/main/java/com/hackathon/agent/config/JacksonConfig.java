package com.hackathon.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
     * Создаёт и настраивает экземпляр {@link ObjectMapper}.
     * <p>
     * <b>Текущая конфигурация:</b>
     * <ul>
     *     <li>Зарегистрирован модуль {@link JavaTimeModule} — обеспечивает корректную
     *         сериализацию/десериализацию классов Java 8 Date/Time API
     *         ({@link java.time.LocalDateTime}, {@link java.time.LocalDate} и др.).</li>
     *     <li>Отключён {@link SerializationFeature#WRITE_DATES_AS_TIMESTAMPS} —
     *         даты сериализуются в ISO-8601 (например, {@code "2025-01-15T10:30:00"}),
     *         а не в виде числовых массивов.</li>
     * </ul>
     * </p>
     *
     * <p><b>Возможные расширения:</b></p>
     * <p>
     * При необходимости метод можно дополнить, например, отключением ошибок
     * на неизвестные поля:
     * </p>
     * <pre>
     * mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
     * </pre>
     *
     * @return сконфигурированный экземпляр {@link ObjectMapper}
     * @see ObjectMapper
     * @see JavaTimeModule
     * @see SerializationFeature#WRITE_DATES_AS_TIMESTAMPS
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}