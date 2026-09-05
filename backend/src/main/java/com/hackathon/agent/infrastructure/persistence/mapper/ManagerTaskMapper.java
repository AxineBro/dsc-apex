package com.hackathon.agent.infrastructure.persistence.mapper;

import com.hackathon.agent.domain.model.ManagerTask;
import com.hackathon.agent.infrastructure.notification.LoggingNotificationService;
import com.hackathon.agent.infrastructure.persistence.entity.ManagerTaskEntity;
import org.springframework.stereotype.Component;

/**
 * Маппер для преобразования между доменной моделью {@link ManagerTask}
 * и JPA-сущностью {@link ManagerTaskEntity}.
 * <p>
 * Обеспечивает разделение слоёв приложения: доменный слой работает с объектами
 * {@link ManagerTask}, а инфраструктурный — с {@link ManagerTaskEntity}.
 * Маппер используется в {@link LoggingNotificationService} для сохранения
 * задач менеджеров в базе данных.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Преобразование доменного объекта в JPA-сущность для сохранения в БД.</li>
 *     <li>Преобразование JPA-сущности в доменный объект для бизнес-логики.</li>
 *     <li>Инкапсуляция логики маппинга, что упрощает тестирование и поддержку.</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * ManagerTaskMapper mapper = ...;
 * ManagerTask task = ManagerTask.builder()
 *         .sessionId(session.getId())
 *         .reason("user_requested")
 *         .status("NEW")
 *         .build();
 * ManagerTaskEntity entity = mapper.toEntity(task);
 * repository.save(entity);
 * </pre>
 *
 * <p><b>Связь с другими компонентами:</b></p>
 * <ul>
 *     <li>Используется в {@link LoggingNotificationService} для сохранения задач.</li>
 *     <li>Может использоваться в административных интерфейсах для отображения списка задач.</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see ManagerTask
 * @see ManagerTaskEntity
 * @see LoggingNotificationService
 */
@Component
public class ManagerTaskMapper {

    /**
     * Преобразует JPA-сущность {@link ManagerTaskEntity} в доменную модель {@link ManagerTask}.
     * <p>
     * Выполняет прямое сопоставление полей. Принимает во внимание все поля сущности.
     * </p>
     *
     * @param entity JPA-сущность (может быть {@code null})
     * @return доменный объект или {@code null}, если входная сущность равна {@code null}
     */
    public ManagerTask toDomain(ManagerTaskEntity entity) {
        if (entity == null) return null;
        return ManagerTask.builder()
                .id(entity.getId())
                .sessionId(entity.getSessionId())
                .clientPhone(entity.getClientPhone())
                .reason(entity.getReason())
                .selectedApartmentId(entity.getSelectedApartmentId())
                .dialogHistory(entity.getDialogHistory())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * Преобразует доменную модель {@link ManagerTask} в JPA-сущность {@link ManagerTaskEntity}.
     * <p>
     * Выполняет прямое сопоставление полей. Используется перед сохранением в базу данных.
     * </p>
     *
     * @param domain доменный объект (может быть {@code null})
     * @return JPA-сущность или {@code null}, если входной объект равен {@code null}
     */
    public ManagerTaskEntity toEntity(ManagerTask domain) {
        if (domain == null) return null;
        return ManagerTaskEntity.builder()
                .id(domain.getId())
                .sessionId(domain.getSessionId())
                .clientPhone(domain.getClientPhone())
                .reason(domain.getReason())
                .selectedApartmentId(domain.getSelectedApartmentId())
                .dialogHistory(domain.getDialogHistory())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}