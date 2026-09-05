package com.hackathon.agent.infrastructure.persistence.repository;

import com.hackathon.agent.infrastructure.persistence.entity.SessionEntity;
import com.hackathon.agent.infrastructure.persistence.impl.SessionManagerImpl;
import com.hackathon.agent.infrastructure.persistence.mapper.SessionMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA репозиторий для работы с сущностью сессий {@link SessionEntity}.
 * <p>
 * Обеспечивает доступ к данным сессий в базе данных, включая поиск по ключу сессии,
 * поиск по статусам и времени последней активности. Используется в {@link SessionManagerImpl}
 * для выполнения всех операций сохранения, загрузки и поиска сессий.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>CRUD-операции с сессиями (наследуется от {@link JpaRepository}).</li>
 *     <li>Поиск сессии по публичному ключу {@code sessionKey} для восстановления состояния
 *         при получении запроса с заголовком {@code X-Session-Id}.</li>
 *     <li>Поиск сессий с определёнными статусами и активностью старше заданного порога
 *         для фоновой очистки неактивных сессий (используется в планировщике).</li>
 * </ul>
 *
 * <p><b>Методы:</b></p>
 * <ul>
 *     <li>{@link #findBySessionKey(String)} — находит сессию по её строковому ключу,
 *         возвращая {@link Optional} для безопасной обработки отсутствия результата.</li>
 *     <li>{@link #findByStatusInAndLastActivityAtBefore(List, LocalDateTime)} — находит все сессии,
 *         статус которых входит в переданный список, а время последней активности
 *         меньше (раньше) указанного порога. Используется для поиска «зависших» сессий.</li>
 * </ul>
 *
 * <p><b>Использование в сервисах:</b></p>
 * <pre>
 * // Получение сессии по ключу
 * SessionEntity entity = repository.findBySessionKey(sessionKey)
 *         .orElseThrow(() -> new SessionNotFoundException(sessionKey));
 *
 * // Поиск сессий для очистки (таймаут = 30 минут)
 * List&lt;SessionEntity&gt; expired = repository.findByStatusInAndLastActivityAtBefore(
 *         List.of("AWAITING_INPUT", "TIMEOUT_REMINDER"),
 *         LocalDateTime.now().minusMinutes(30)
 * );
 * </pre>
 *
 * <p><b>Индексы БД:</b></p>
 * Для эффективной работы метода {@code findBySessionKey} рекомендуется создать
 * уникальный индекс на колонке {@code session_key} (в сущности уже задано
 * {@code unique = true}). Для метода {@code findByStatusInAndLastActivityAtBefore}
 * может быть полезен составной индекс на полях {@code status} и {@code last_activity_at}.
 *
 * @author Axine
 * @since 1.0.0
 * @see SessionEntity
 * @see SessionManagerImpl
 * @see SessionMapper
*/
@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, UUID> {

    /**
     * Находит сессию по её публичному ключу.
     * <p>
     * Ключ сессии (обычно UUID в строковом представлении) передаётся клиентом
     * в заголовке {@code X-Session-Id} при каждом запросе. Метод возвращает
     * {@link Optional}, позволяя обработать ситуацию отсутствия сессии без
     * выбрасывания исключения на уровне репозитория.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * Optional&lt;SessionEntity&gt; opt = repository.findBySessionKey("550e8400-e29b-41d4-a716-446655440000");
     * if (opt.isPresent()) {
     *     SessionEntity session = opt.get();
     *     // работа с сессией
     * } else {
     *     // создать новую
     * }
     * </pre>
     *
     * @param sessionKey публичный ключ сессии (не {@code null}, не пустой)
     * @return {@code Optional} с сущностью, если найдена, иначе пустой {@code Optional}
     */
    Optional<SessionEntity> findBySessionKey(String sessionKey);

    /**
     * Находит все сессии, статус которых входит в заданный список, а время последней
     * активности меньше (раньше) указанного порога.
     * <p>
     * Используется для обнаружения «зависших» или неактивных сессий, которые превысили
     * допустимый тайм-аут бездействия. Такие сессии могут быть переведены в статус
     * {@code EXPIRED} или удалены фоновым планировщиком.
     * </p>
     *
     * <p><b>Ограничения:</b></p>
     * <ul>
     *     <li>Обычно ищутся сессии со статусами {@code AWAITING_INPUT} и {@code TIMEOUT_REMINDER},
     *         так как они считаются «живыми», но могут быть неактивны.</li>
     *     <li>Сессии со статусами {@code FINISHED}, {@code TO_MANAGER}, {@code NEW}
     *         обычно не подлежат автоматической очистке.</li>
     * </ul>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * // Найти сессии, неактивные более 30 минут
     * LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
     * List&lt;SessionEntity&gt; expired = repository.findByStatusInAndLastActivityAtBefore(
     *         List.of("AWAITING_INPUT", "TIMEOUT_REMINDER"),
     *         threshold
     * );
     * // затем перевести их в статус "EXPIRED"
     * </pre>
     *
     * @param statuses список строковых статусов (например, {@code "AWAITING_INPUT"}) — не {@code null}
     * @param time     пороговое время (включительно не включается, т.е. сессии, у которых
     *                 {@code lastActivityAt < time}) — не {@code null}
     * @return список сессий, удовлетворяющих условиям (может быть пустым, но не {@code null})
     */
    List<SessionEntity> findByStatusInAndLastActivityAtBefore(List<String> statuses, LocalDateTime time);
}