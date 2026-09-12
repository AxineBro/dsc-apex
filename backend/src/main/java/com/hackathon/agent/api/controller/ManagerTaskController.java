package com.hackathon.agent.api.controller;

import com.hackathon.agent.infrastructure.persistence.entity.ManagerTaskEntity;
import com.hackathon.agent.infrastructure.persistence.repository.ManagerTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST-контроллер для управления задачами менеджеров.
 * <p>
 * Предоставляет эндпоинты для получения списка задач и изменения их статуса.
 * Используется внутренним интерфейсом менеджеров для обработки обращений,
 * переведённых из чат-бота ({@code ChatFacade}).
 * </p>
 *
 * <p><b>Версия API:</b> v1</p>
 * <p><b>Базовый путь:</b> {@code /api/v1/manager/tasks}</p>
 *
 * @author Axine
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/manager/tasks")
@RequiredArgsConstructor
public class ManagerTaskController {

    /**
     * Репозиторий для доступа к задачам менеджеров.
     * <p>
     * Внедряется через конструктор (Lombok {@code @RequiredArgsConstructor}).
     * </p>
     */
    private final ManagerTaskRepository repository;

    /**
     * Возвращает список задач менеджеров, отсортированных по дате создания (от новых к старым).
     * <p>
     * Используется постраничная выборка: всегда запрашивается первая страница
     * ({@link org.springframework.data.domain.PageRequest#of(int, int)} с номером {@code 0}).
     * Значение {@code limit} ограничивается сверху значением {@code 200} для защиты
     * от чрезмерно больших ответов.
     * </p>
     *
     * <p><b>Пример запроса:</b></p>
     * <pre>
     * GET /api/v1/manager/tasks?limit=20
     * </pre>
     *
     * <p><b>Пример ответа (200 OK):</b></p>
     * <pre>
     * [
     *     {
     *         "id": 42,
     *         "reason": "Клиент просит соединить с оператором",
     *         "clientPhone": "+79990000000",
     *         "status": "NEW",
     *         "createdAt": "2025-01-15T10:30:00",
     *         "dialogHistory": "..."
     *     }
     * ]
     * </pre>
     *
     * @param limit максимальное количество возвращаемых задач.
     *              Значение по умолчанию — {@code 50}; фактически применяется
     *              {@code min(limit, 200)}.
     * @return список задач {@link ManagerTaskDto}, обёрнутый в {@link java.util.List}.
     *         Может быть пустым, если задач нет.
     * @see ManagerTaskDto
     * @see ManagerTaskEntity
     */
    @GetMapping
    public List<ManagerTaskDto> list(@RequestParam(defaultValue = "50") int limit) {
        return repository.findAll(
                        PageRequest.of(0, Math.min(limit, 200), Sort.by("createdAt").descending()))
                .map(this::toDto)
                .toList();
    }

    /**
     * Обновляет статус задачи менеджера по её идентификатору.
     * <p>
     * Тело запроса представляет собой произвольную карту (JSON-объект) с полем
     * {@code status}. Если поле отсутствует, статус сохраняется прежним
     * (используется {@link ManagerTaskEntity#getStatus()}).
     * </p>
     *
     * <p><b>Пример запроса:</b></p>
     * <pre>
     * PATCH /api/v1/manager/tasks/42
     * Content-Type: application/json
     *
     * {
     *     "status": "IN_PROGRESS"
     * }
     * </pre>
     *
     * <p><b>Пример ответа (200 OK):</b></p>
     * <pre>
     * {
     *     "id": 42,
     *     "reason": "Клиент просит соединить с оператором",
     *     "clientPhone": "+79990000000",
     *     "status": "IN_PROGRESS",
     *     "createdAt": "2025-01-15T10:30:00",
     *     "dialogHistory": "..."
     * }
     * </pre>
     *
     * @param id   идентификатор задачи, передаваемый в пути запроса.
     * @param body тело запроса, содержащее поле {@code status}.
     *             Может быть пустым — тогда статус не изменяется.
     * @return обновлённое представление задачи {@link ManagerTaskDto}.
     * @throws java.util.NoSuchElementException если задача с указанным {@code id}
     *                                          не найдена (возбуждается {@link java.util.Optional#orElseThrow()}).
     * @see ManagerTaskDto
     * @see ManagerTaskEntity
     */
    @PatchMapping("/{id}")
    public ManagerTaskDto setStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        ManagerTaskEntity e = repository.findById(id).orElseThrow();
        e.setStatus(body.getOrDefault("status", e.getStatus()));
        return toDto(repository.save(e));
    }

    /**
     * Преобразует сущность задачи в DTO для передачи клиенту.
     * <p>
     * Используется для отделения персистентной модели ({@link ManagerTaskEntity})
     * от публичного контракта API ({@link ManagerTaskDto}).
     * </p>
     *
     * @param t сущность задачи (не {@code null}).
     * @return DTO {@link ManagerTaskDto} со всеми полями, необходимыми клиенту.
     * @see ManagerTaskDto
     * @see ManagerTaskEntity
     */
    private ManagerTaskDto toDto(ManagerTaskEntity t) {
        return new ManagerTaskDto(t.getId(), t.getReason(), t.getClientPhone(),
                t.getStatus(), t.getCreatedAt(), t.getDialogHistory());
    }

    /**
     * DTO-представление задачи менеджера, возвращаемое клиенту.
     * <p>
     * Содержит только те поля, которые необходимы внешнему интерфейсу,
     * и скрывает детали персистентной сущности {@link ManagerTaskEntity}.
     * </p>
     *
     * @param id             уникальный идентификатор задачи.
     * @param reason         причина создания задачи (например, запрос клиента на связь с оператором).
     * @param clientPhone    контактный телефон клиента.
     * @param status         текущий статус задачи (например, {@code NEW}, {@code IN_PROGRESS}, {@code DONE}).
     * @param createdAt      дата и время создания задачи.
     * @param dialogHistory  история диалога клиента с ботом, сохранённая в задаче.
     * @see ManagerTaskEntity
     */
    public record ManagerTaskDto(Long id, String reason, String clientPhone,
                                 String status, java.time.LocalDateTime createdAt,
                                 String dialogHistory) {}
}