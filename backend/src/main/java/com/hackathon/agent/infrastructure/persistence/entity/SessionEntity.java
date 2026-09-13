package com.hackathon.agent.infrastructure.persistence.entity;

import com.hackathon.agent.application.orchestrator.SessionManager;
import com.hackathon.agent.domain.model.Message;
import com.hackathon.agent.infrastructure.persistence.mapper.SessionMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA-сущность для хранения состояния диалоговой сессии в базе данных.
 * <p>
 * Отображается на таблицу {@code sessions}. Содержит все параметры подбора квартир,
 * историю диалога, ранжированный список, счётчики и временные метки.
 * Используется в {@link SessionManager} для сохранения и восстановления сессий.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Персистентное хранение состояния сессии между запросами.</li>
 *     <li>Обеспечение масштабируемости — сессии не зависят от памяти одного экземпляра приложения.</li>
 *     <li>Возможность восстановления диалога после перезапуска сервиса.</li>
 *     <li>Основа для анализа истории диалогов и статистики.</li>
 * </ul>
 *
 * <p><b>Связь с доменной моделью:</b></p>
 * Преобразование между доменным объектом {@link com.hackathon.agent.domain.model.Session}
 * и данной сущностью выполняется через {@link SessionMapper}, что обеспечивает
 * разделение слоёв (Domain ↔ Infrastructure).
 *
 * <p><b>Структура таблицы:</b></p>
 * <ul>
 *     <li>{@code id} — первичный ключ типа UUID (генерируется автоматически).</li>
 *     <li>{@code session_key} — строковый ключ для передачи клиенту (уникальный, не {@code null}).</li>
 *     <li>{@code status} — статус сессии (например, "NEW", "AWAITING_INPUT", "FINISHED").</li>
 *     <li>Поля фильтров: {@code area_min}, {@code area_max}, {@code price_min}, {@code price_max},
 *         {@code rooms_min}, {@code rooms_max}, {@code floor}.</li>
 *     <li>{@code counter} — счётчик циклов обработки.</li>
 *     <li>{@code ranked_list_json} — JSON-массив ID квартир в порядке ранжирования (TEXT).</li>
 *     <li>{@code dialog_history_json} — JSON-массив сообщений {@link Message} (TEXT).</li>
 *     <li>{@code last_activity_at} — время последней активности.</li>
 *     <li>{@code created_at} — время создания (устанавливается автоматически).</li>
 *     <li>{@code clarification_count} — количество заданных уточняющих вопросов.</li>
 *     <li>{@code expanded_once} — флаг расширенного поиска.</li>
 *     <li>{@code current_index} — текущий индекс в ранжированном списке.</li>
 *     <li>{@code reminder_sent_at} — время отправки напоминания о тайм-ауте.</li>
 *     <li>{@code version} — версия для оптимистичной блокировки.</li>
 * </ul>
 *
 * <p><b>Оптимистичная блокировка:</b></p>
 * Поле {@code version} аннотировано {@code @Version} и автоматически инкрементируется
 * при каждом обновлении. Это предотвращает конкурентное изменение одной сессии
 * разными потоками (гарантирует атомарность операций).
 *
 * <p><b>JSON-поля:</b></p>
 * Поля {@code rankedListJson} и {@code dialogHistoryJson} хранят сложные структуры
 * в текстовом формате. При сохранении/загрузке они сериализуются/десериализуются
 * с помощью {@link com.fasterxml.jackson.databind.ObjectMapper}. Это позволяет
 * избежать создания дополнительных таблиц и упрощает схему БД.
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * SessionEntity entity = SessionEntity.builder()
 *         .sessionKey("550e8400-e29b-41d4-a716-446655440000")
 *         .status("NEW")
 *         .lastActivityAt(LocalDateTime.now())
 *         .build();
 * repository.save(entity);
 * </pre>
 *
 * @author Axine
 * @since 1.0.0
 * @see com.hackathon.agent.domain.model.Session
 * @see SessionMapper
 * @see com.hackathon.agent.application.orchestrator.SessionManager
 */
@Entity
@Table(name = "sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionEntity {

    /**
     * Уникальный идентификатор сессии (первичный ключ).
     * <p>
     * Генерируется автоматически с использованием стратегии {@link GenerationType#UUID}.
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Публичный строковый ключ сессии, передаваемый клиенту в заголовке {@code X-Session-Id}.
     * <p>
     * Уникален (ограничение {@code UNIQUE}) и не может быть {@code null}.
     * Длина — 36 символов (стандартный UUID в строковом представлении).
     * </p>
     */
    @Column(name = "session_key", length = 36, nullable = false, unique = true)
    private String sessionKey;

    /**
     * Текущий статус сессии в строковом представлении.
     * <p>
     * Не может быть {@code null}. Значения соответствуют {@link com.hackathon.agent.domain.model.SessionState}.
     * </p>
     * <p>
     * <b>Примеры:</b> {@code "NEW"}, {@code "AWAITING_INPUT"}, {@code "FINISHED"}.
     * </p>
     */
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    /**
     * ID квартиры, для которой сформировано последнее коммерческое предложение (КП)
     * в рамках сессии.
     * <p>
     * Соответствует доменному полю
     * {@link com.hackathon.agent.domain.model.Session#getSelectedApartmentId()}.
     * Заполняется при успешном создании КП.
     * </p>
     * <p>
     * Значение {@code null} означает, что КП в рамках сессии ещё не формировалось.
     * </p>
     */
    @Column(name = "selected_apartment_id")
    private Long selectedApartmentId;

    /**
     * Контактный телефон клиента, полученный в процессе диалога.
     * <p>
     * Сохраняется для передачи менеджеру при переводе сессии в статус
     * {@code TO_MANAGER}, а также может использоваться для обратной связи
     * или подтверждения записи. Заполняется на основе сообщений пользователя
     * после уточняющего вопроса ассистента.
     * </p>
     * <p>
     * Длина поля ограничена 50 символами, что достаточно для хранения
     * номера телефона в международном формате (например, +7XXXXXXXXXX).
     * Значение {@code null} означает, что телефон ещё не был предоставлен
     * пользователем или не был распознан.
     * </p>
     * <p>
     * Данное поле соответствует доменному полю {@code clientPhone} в
     * {@link com.hackathon.agent.domain.model.Session} и используется
     * для синхронизации состояния между слоями через {@link SessionMapper}.
     * </p>
     *
     * @see SessionMapper
     */
    @Column(name = "client_phone", length = 50)
    private String clientPhone;

    // -------------------- Параметры подбора (фильтры) --------------------

    @Column(name = "area_min")
    private BigDecimal areaMin;
    @Column(name = "area_max")
    private BigDecimal areaMax;
    @Column(name = "price_min")
    private BigDecimal priceMin;
    @Column(name = "price_max")
    private BigDecimal priceMax;
    @Column(name = "rooms_min")
    private Integer roomsMin;
    @Column(name = "rooms_max")
    private Integer roomsMax;
    private Integer floor;
    @Column(name = "complex")
    private String complex;

    // -------------------- Служебные счётчики --------------------

    /**
     * Счётчик обработанных циклов диалога.
     * <p>
     * Значение по умолчанию в БД — 0 (определено через {@code columnDefinition}).
     * Инкрементируется при каждом вызове обработки в {@link com.hackathon.agent.application.orchestrator.AgentOrchestrator}.
     * </p>
     */
    @Column(name = "counter", columnDefinition = "INT DEFAULT 0")
    private Integer counter;

    // -------------------- JSON-поля --------------------

    /**
     * JSON-массив идентификаторов квартир в порядке ранжирования.
     * <p>
     * Хранится как текст (TEXT) и содержит, например, {@code [1001, 1002, 1003]}.
     * Десериализуется в {@code List<Long>} при загрузке через {@link SessionMapper}.
     * </p>
     */
    @Column(name = "ranked_list_json", columnDefinition = "TEXT")
    private String rankedListJson;  // JSON-массив ID квартир

    /**
     * JSON-массив сообщений истории диалога.
     * <p>
     * Хранится как текст (TEXT) и содержит массив объектов {@link Message}
     * в формате {@code [{"role":"user","text":"Привет","timestamp":"..."}, ...]}.
     * Десериализуется в {@code List<Message>} при загрузке.
     * </p>
     */
    @Column(name = "dialog_history_json", columnDefinition = "TEXT")
    private String dialogHistoryJson;

    // -------------------- Временные метки --------------------

    /**
     * Время последней активности сессии.
     * <p>
     * Обновляется при каждом взаимодействии. Используется для выявления неактивных сессий.
     * </p>
     */
    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    /**
     * Время создания сессии.
     * <p>
     * Устанавливается автоматически при вставке записи с помощью {@link CreationTimestamp}.
     * Поле доступно только для чтения ({@code updatable = false}).
     * </p>
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // -------------------- Дополнительные параметры --------------------

    /**
     * Счётчик уточняющих вопросов, заданных AI.
     * <p>
     * Используется для ограничения числа вопросов (максимум 2).
     * </p>
     */
    @Column(name = "clarification_count")
    private Integer clarificationCount;

    /**
     * Флаг расширенного поиска (было ли расширение применено).
     * <p>
     * {@code true} — расширение уже выполнялось в рамках текущей сессии.
     * </p>
     */
    @Column(name = "expanded_once")
    private Boolean expandedOnce;

    /**
     * Текущий индекс в ранжированном списке для пошагового показа квартир.
     */
    @Column(name = "current_index")
    private Integer currentIndex;

    /**
     * Время отправки последнего напоминания о тайм-ауте.
     * <p>
     * Используется для контроля частоты напоминаний.
     * </p>
     */
    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    // -------------------- Оптимистичная блокировка --------------------

    /**
     * Версия сущности для оптимистичной блокировки.
     * <p>
     * Автоматически инкрементируется при каждом обновлении.
     * Обеспечивает целостность при конкурентном доступе.
     * </p>
     */
    @Version
    private Integer version;
}
