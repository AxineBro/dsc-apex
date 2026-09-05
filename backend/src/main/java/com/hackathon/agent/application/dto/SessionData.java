package com.hackathon.agent.application.dto;

import com.hackathon.agent.application.facade.ChatFacade;
import com.hackathon.agent.application.orchestrator.AgentOrchestrator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @deprecated Данный DTO не используется в текущей версии приложения.
 * Все сервисы ({@link ChatFacade}, {@link AgentOrchestrator}) работают напрямую
 * с доменной моделью {@link com.hackathon.agent.domain.model.Session}.
 * Класс оставлен только для обратной совместимости и будет удален в следующих релизах.
 *
 * @author Axine
 * @since 1.0.0
 */
@Deprecated
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionData {

    /**
     * Внутренний технический идентификатор сессии.
     * <p>
     * Генерируется автоматически при создании новой записи.
     * Используется для идентификации сессии в базе данных
     * и внутренних сервисах. Отличается от {@link #sessionKey},
     * который передаётся клиенту.
     * </p>
     *
     * @see UUID#randomUUID()
     */
    private UUID id;

    /**
     * Публичный строковый идентификатор сессии.
     * <p>
     * Передаётся клиенту в заголовке {@code X-Session-Id}.
     * Должен быть уникальным и соответствовать формату UUID.
     * Используется для сопоставления запроса с конкретной сессией
     * в {@link ChatFacade}.
     * </p>
     */
    private String sessionKey;

    /**
     * Текущий статус сессии.
     * <p>
     * Может принимать значения, определённые в бизнес-логике.
     * Статус позволяет управлять жизненным циклом сессии и определять,
     * может ли она обрабатывать новые сообщения.
     * </p>
     */
    private String status;

    /**
     * Дата и время последнего взаимодействия с сессией.
     * <p>
     * Обновляется при каждом запросе, связанном с данной сессией.
     * Используется для определения неактивных сессий и их автоматического
     * завершения.
     * </p>
     *
     * @see LocalDateTime
     * @see java.time.LocalDateTime#now()
     */
    private LocalDateTime lastActivityAt;
}