package com.hackathon.agent.infrastructure.persistence.entity;

import com.hackathon.agent.domain.model.ManagerTask;
import com.hackathon.agent.infrastructure.notification.LoggingNotificationService;
import com.hackathon.agent.infrastructure.persistence.mapper.ManagerTaskMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA-сущность для хранения задач (тикетов), создаваемых при передаче диалога менеджеру.
 * <p>
 * Отображается на таблицу {@code manager_tasks} в базе данных. Используется в
 * {@link LoggingNotificationService} для сохранения информации о необходимости
 * вмешательства человека в диалог.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Хранение заявок на обработку менеджером в реляционной БД.</li>
 *     <li>Обеспечение возможности отслеживания статуса и истории задач.</li>
 *     <li>Служит основой для построения интерфейса менеджера (список задач, фильтрация, поиск).</li>
 * </ul>
 *
 * <p><b>Связь с доменной моделью:</b></p>
 * Преобразование между доменным объектом {@link com.hackathon.agent.domain.model.ManagerTask}
 * и данной сущностью выполняется через {@link ManagerTaskMapper}, что позволяет
 * сохранять разделение слоёв (Domain ↔ Infrastructure).
 *
 * <p><b>Структура таблицы:</b></p>
 * <ul>
 *     <li>{@code id} — первичный ключ, автоинкремент.</li>
 *     <li>{@code session_id} — идентификатор сессии чата (UUID, обязательное поле).</li>
 *     <li>{@code client_phone} — номер телефона клиента (до 20 символов, опционально).</li>
 *     <li>{@code reason} — причина перевода (до 100 символов, обязательно).</li>
 *     <li>{@code selected_apartment_id} — ID выбранной квартиры (опционально).</li>
 *     <li>{@code dialog_history} — полная история диалога в текстовом формате (TEXT).</li>
 *     <li>{@code status} — статус обработки задачи (например, "NEW", "IN_PROGRESS", "COMPLETED").</li>
 *     <li>{@code created_at} — временная метка создания (устанавливается автоматически).</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * ManagerTaskEntity task = ManagerTaskEntity.builder()
 *         .sessionId(session.getId())
 *         .clientPhone("+79001234567")
 *         .reason("user_requested_manager")
 *         .dialogHistory(jsonHistory)
 *         .status("NEW")
 *         .build();
 * repository.save(task);
 * </pre>
 *
 * @author Axine
 * @since 1.0.0
 * @see LoggingNotificationService
 * @see ManagerTask
 * @see ManagerTaskMapper
 */
@Entity
@Table(name = "manager_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerTaskEntity {

    /**
     * Уникальный идентификатор записи (первичный ключ).
     * <p>
     * Генерируется базой данных с использованием стратегии {@link GenerationType#IDENTITY}
     * (автоинкремент).
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Идентификатор сессии чата, к которой относится задача.
     * <p>
     * Соответствует полю {@code id} в доменной модели {@link com.hackathon.agent.domain.model.Session}.
     * Является обязательным полем (колонка {@code NOT NULL}).
     * </p>
     * <p>
     * Используется для связи задачи с конкретным диалогом и восстановления истории
     * при необходимости.
     * </p>
     */
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    /**
     * Контактный телефон клиента для связи с ним.
     * <p>
     * Опциональное поле (может быть {@code null}), если номер не был предоставлен.
     * Максимальная длина — 20 символов.
     * </p>
     */
    @Column(name = "client_phone", length = 20)
    private String clientPhone;

    /**
     * Причина перевода диалога менеджеру.
     * <p>
     * Обязательное поле (не может быть {@code null}). Максимальная длина — 100 символов.
     * </p>
     * <p>
     * Примеры значений:
     * <ul>
     *     <li>{@code "user_requested"} — клиент явно попросил менеджера.</li>
     *     <li>{@code "error_NullPointerException"} — техническая ошибка.</li>
     *     <li>{@code "no_apartments_found"} — не найдено подходящих квартир.</li>
     *     <li>{@code "limit_cycles"} — превышен лимит циклов подбора.</li>
     *     <li>{@code "too_many_questions"} — превышено количество уточнений.</li>
     * </ul>
     * </p>
     */
    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    /**
     * Идентификатор квартиры, выбранной пользователем (если применимо).
     * <p>
     * Опциональное поле. Используется, если клиент уже выбрал конкретную квартиру,
     * но требуется консультация менеджера по ней (например, для уточнения условий).
     * </p>
     */
    @Column(name = "selected_apartment_id")
    private Long selectedApartmentId;

    /**
     * Полная история диалога в виде текста (обычно в формате JSON).
     * <p>
     * Хранит последовательность сообщений между пользователем и ассистентом,
     * чтобы менеджер мог восстановить контекст без повторного опроса клиента.
     * </p>
     * <p>
     * Тип колонки — {@code TEXT} (неограниченная длина).
     * </p>
     */
    @Column(name = "dialog_history", columnDefinition = "TEXT")
    private String dialogHistory;

    /**
     * Текущий статус обработки задачи менеджером.
     * <p>
     * Строковое поле длиной до 20 символов. Рекомендуемые значения:
     * <ul>
     *     <li>{@code "NEW"} — создана, ожидает обработки.</li>
     *     <li>{@code "IN_PROGRESS"} — менеджер взял в работу.</li>
     *     <li>{@code "COMPLETED"} — задача решена.</li>
     *     <li>{@code "CANCELLED"} — отменена (например, клиент передумал).</li>
     * </ul>
     * </p>
     * <p>
     * В будущем рекомендуется заменить строку на перечисление (enum) для типобезопасности.
     * </p>
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * Временная метка создания задачи (дата и время).
     * <p>
     * Устанавливается автоматически при вставке записи с помощью аннотации
     * {@link CreationTimestamp} (Hibernate). Поле доступно только для чтения
     * ({@code updatable = false}).
     * </p>
     * <p>
     * Используется для сортировки задач, мониторинга времени отклика (SLA)
     * и анализа нагрузки на менеджеров.
     * </p>
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}