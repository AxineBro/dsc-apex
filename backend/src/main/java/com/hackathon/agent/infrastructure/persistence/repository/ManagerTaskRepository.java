package com.hackathon.agent.infrastructure.persistence.repository;

import com.hackathon.agent.infrastructure.notification.LoggingNotificationService;
import com.hackathon.agent.infrastructure.persistence.entity.ManagerTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA репозиторий для работы с сущностью задач менеджеров {@link ManagerTaskEntity}.
 * <p>
 * Обеспечивает базовые CRUD-операции, а также возможность расширения методами поиска
 * и фильтрации. Используется в сервисе уведомлений {@link LoggingNotificationService}
 * для сохранения задач, создаваемых при передаче диалога менеджеру.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Сохранение и извлечение задач менеджеров из базы данных.</li>
 *     <li>Предоставление стандартных методов доступа (save, findById, findAll, delete и т.д.)</li>
 *     <li>Основа для построения административных интерфейсов (список задач, фильтрация по статусу, дате и т.д.).</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * ManagerTaskRepository repository = ...;
 * ManagerTaskEntity task = ManagerTaskEntity.builder()
 *         .sessionId(session.getId())
 *         .clientPhone("+79001234567")
 *         .reason("user_requested")
 *         .status("NEW")
 *         .build();
 * repository.save(task);
 *
 * // Поиск всех задач
 * List&lt;ManagerTaskEntity&gt; allTasks = repository.findAll();
 * </pre>
 *
 * <p><b>Связь с другими компонентами:</b></p>
 * <ul>
 *     <li>Используется в {@link LoggingNotificationService} для сохранения задач.</li>
 *     <li>Может использоваться в контроллерах для отображения списка задач менеджерам.</li>
 * </ul>
 *
 * <p><b>Расширение:</b></p>
 * При необходимости можно добавлять пользовательские методы запросов, например:
 * <ul>
 *     <li>{@code List<ManagerTaskEntity> findByStatus(String status);}</li>
 *     <li>{@code List<ManagerTaskEntity> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);}</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see ManagerTaskEntity
 * @see LoggingNotificationService
 */
@Repository
public interface ManagerTaskRepository extends JpaRepository<ManagerTaskEntity, Long> {
}