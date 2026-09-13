package com.hackathon.agent.infrastructure.persistence.repository;

import com.hackathon.agent.infrastructure.persistence.entity.OfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA репозиторий для работы с сущностью коммерческих предложений
 * {@link OfferEntity}.
 * <p>
 * Обеспечивает доступ к данным о сформированных коммерческих предложениях (КП)
 * в базе данных. Наследует стандартные CRUD-операции от {@link JpaRepository},
 * а также предоставляет специализированный метод поиска последнего по времени
 * создания оффера для указанной сессии.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Сохранение и извлечение коммерческих предложений из БД.</li>
 *     <li>Поиск последнего сформированного КП по внутреннему идентификатору сессии
 *         ({@code sessionId}).</li>
 *     <li>Обеспечение выдачи PDF-файла КП через контроллер
 *         {@link com.hackathon.agent.api.controller.OfferController}.</li>
 *     <li>Предоставление основы для административных и аналитических сценариев,
 *         связанных с офферами.</li>
 * </ul>
 *
 * <p><b>Методы:</b></p>
 * <ul>
 *     <li>{@link #findFirstBySessionIdOrderByCreatedAtDesc(UUID)} — находит последний
 *         по времени создания оффер для указанной сессии.</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * Optional&lt;OfferEntity&gt; latest = offerRepository
 *         .findFirstBySessionIdOrderByCreatedAtDesc(sessionId);
 *
 * latest.ifPresent(entity -&gt; {
 *     // работа с найденным КП
 * });
 * </pre>
 *
 * <p><b>Связь с другими компонентами:</b></p>
 * <ul>
 *     <li>Используется в
 *         {@link com.hackathon.agent.infrastructure.offer.OfferServiceImpl}
 *         для поиска последнего оффера по сессии.</li>
 *     <li>Возвращаемая сущность {@link OfferEntity} преобразуется в доменную модель
 *         {@link com.hackathon.agent.domain.model.Offer} через
 *         {@link com.hackathon.agent.infrastructure.persistence.mapper.OfferMapper}.</li>
 *     <li>Косвенно связан с
 *         {@link com.hackathon.agent.domain.service.OfferService}, который определяет
 *         контракт работы с коммерческими предложениями.</li>
 * </ul>
 *
 * <p><b>Индексы БД:</b></p>
 * В сущности {@link OfferEntity} заданы индексы по полям {@code session_id}
 * и {@code apartment_id}, что ускоряет поиск офферов по сессии и квартире.
 *
 * @author Axine
 * @since 1.0.0
 * @see OfferEntity
 * @see com.hackathon.agent.infrastructure.offer.OfferServiceImpl
 * @see com.hackathon.agent.infrastructure.persistence.mapper.OfferMapper
 * @see com.hackathon.agent.domain.service.OfferService
 */
@Repository
public interface OfferRepository extends JpaRepository<OfferEntity, UUID> {

    /**
     * Находит последний по времени создания оффер для указанной сессии.
     * <p>
     * Метод использует механизм производных запросов Spring Data JPA:
     * имя метода транслируется в запрос вида
     * {@code SELECT ... WHERE session_id = ? ORDER BY created_at DESC LIMIT 1}.
     * Если для сессии существует несколько коммерческих предложений,
     * возвращается то, у которого поле {@code createdAt} максимально.
     * </p>
     *
     * <p><b>Использование:</b></p>
     * Применяется для выдачи PDF-файла КП по публичному ключу сессии
     * ({@code X-Session-Id}). Сначала выполняется поиск сессии через
     * {@link SessionRepository#findBySessionKey(String)}, затем её внутренний
     * идентификатор передаётся в данный метод.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * Optional&lt;OfferEntity&gt; latest = offerRepository
     *         .findFirstBySessionIdOrderByCreatedAtDesc(sessionId);
     *
     * if (latest.isPresent()) {
     *     OfferEntity offer = latest.get();
     *     // отдать PDF или преобразовать в доменную модель
     * }
     * </pre>
     *
     * @param sessionId внутренний UUID сессии, для которой требуется найти
     *                  последнее коммерческое предложение. Соответствует полю
     *                  {@code sessionId} сущности {@link OfferEntity} и
     *                  {@code id} сущности
     *                  {@link com.hackathon.agent.infrastructure.persistence.entity.SessionEntity}.
     *                  Не должен быть {@code null}.
     * @return {@link Optional}, содержащий последний найденный оффер, либо пустой
     *         {@link Optional}, если для указанной сессии ещё не создано ни одного
     *         коммерческого предложения. Никогда не возвращает {@code null}.
     * @see com.hackathon.agent.infrastructure.offer.OfferServiceImpl#findLatestBySessionId(UUID)
     * @see com.hackathon.agent.infrastructure.offer.OfferServiceImpl#findLatestBySessionKey(String)
     * @see OfferEntity#getSessionId()
     */
    Optional<OfferEntity> findFirstBySessionIdOrderByCreatedAtDesc(UUID sessionId);
}