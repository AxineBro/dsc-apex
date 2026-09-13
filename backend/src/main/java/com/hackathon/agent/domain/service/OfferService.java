package com.hackathon.agent.domain.service;

import com.hackathon.agent.domain.model.Apartment;
import com.hackathon.agent.domain.model.Offer;
import com.hackathon.agent.domain.model.Session;

import java.util.Optional;
import java.util.UUID;

/**
 * Сервис управления коммерческими предложениями (КП).
 * <p>
 * Определяет контракт для работы с коммерческими предложениями: создание,
 * поиск по идентификатору и поиск последнего предложения по сессии.
 * Является частью доменного слоя и абстрагирует бизнес-логику от деталей
 * хранения и генерации PDF.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Создание нового коммерческого предложения для выбранной квартиры
 *         в рамках конкретной диалоговой сессии.</li>
 *     <li>Предоставление доступа к ранее сформированным предложениям по их
 *         уникальному идентификатору.</li>
 *     <li>Поиск последнего сформированного предложения для указанной сессии
 *         (по внутреннему UUID или публичному ключу {@code X-Session-Id}).</li>
 * </ul>
 *
 * <p><b>Основные обязанности:</b></p>
 * <ul>
 *     <li>Генерация текстовой части КП на основе данных квартиры и сессии.</li>
 *     <li>Формирование PDF-документа коммерческого предложения.</li>
 *     <li>Сохранение оффера в базе данных с присвоением статуса
 *         {@link com.hackathon.agent.domain.model.OfferStatus#READY}.</li>
 *     <li>Обеспечение доступа к сохранённым офферам для последующей выдачи
 *         PDF клиенту.</li>
 * </ul>
 *
 * <p><b>Реализация:</b></p>
 * Основной реализацией интерфейса является
 * {@link com.hackathon.agent.infrastructure.offer.OfferServiceImpl},
 * которая координирует работу генератора текста
 * ({@link OfferGeneratorService}), генератора PDF
 * ({@link com.hackathon.agent.infrastructure.offer.OfferPdfGenerator})
 * и репозитория ({@link com.hackathon.agent.infrastructure.persistence.repository.OfferRepository}).
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * OfferService offerService = ...;
 *
 * // Создание нового КП
 * Offer offer = offerService.createOffer(session, apartment);
 *
 * // Получение по ID
 * Offer found = offerService.getById(offer.getId());
 *
 * // Поиск последнего КП для сессии
 * Optional&lt;Offer&gt; latest = offerService.findLatestBySessionKey(sessionKey);
 * </pre>
 *
 * <p><b>Обработка ошибок:</b></p>
 * Методы, возвращающие {@link Optional}, не выбрасывают исключений при
 * отсутствии результата. Метод {@link #getById(UUID)} выбрасывает
 * {@link com.hackathon.agent.domain.exception.DomainException}, если оффер
 * не найден или передан некорректный идентификатор.
 *
 * @author Axine
 * @since 1.0.0
 * @see Offer
 * @see com.hackathon.agent.infrastructure.offer.OfferServiceImpl
 * @see OfferGeneratorService
 * @see com.hackathon.agent.infrastructure.offer.OfferPdfGenerator
 */
public interface OfferService {

    /**
     * Создаёт новое коммерческое предложение для указанной квартиры и сессии.
     * <p>
     * Выполняет полный цикл формирования КП:
     * <ol>
     *     <li>Генерирует текстовое описание предложения на основе шаблона
     *         и данных квартиры.</li>
     *     <li>Формирует PDF-документ с использованием HTML-шаблона и
     *         зарегистрированного шрифта.</li>
     *     <li>Сохраняет полученный оффер в базе данных.</li>
     * </ol>
     * </p>
     *
     * <p><b>Статус созданного оффера:</b></p>
     * Возвращаемый объект {@link Offer} имеет статус
     * {@link com.hackathon.agent.domain.model.OfferStatus#READY}, что означает
     * готовность PDF-файла к скачиванию или отправке клиенту.
     * </p>
     *
     * <p><b>Транзакционность:</b></p>
     * В реализации
     * {@link com.hackathon.agent.infrastructure.offer.OfferServiceImpl}
     * рендеринг PDF выполняется вне транзакции БД, а сохранение — в отдельной
     * короткой транзакции. Это позволяет избежать длительного удержания
     * соединения с базой данных во время ресурсоёмкой операции генерации PDF.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * Session session = sessionManager.getCurrentSession();
     * Apartment apartment = searchService.getById(apartmentId);
     * Offer offer = offerService.createOffer(session, apartment);
     * log.info("Создан оффер: {}", offer.getId());
     * </pre>
     *
     * @param session   диалоговая сессия клиента (не {@code null});
     *                  используется для получения контактных данных,
     *                  ключа сессии и передачи контекста в генераторы
     * @param apartment выбранная квартира (не {@code null}); источник данных
     *                  для заполнения шаблона КП и PDF-документа
     * @return сохранённый оффер {@link Offer} со статусом
     *         {@link com.hackathon.agent.domain.model.OfferStatus#READY}
     * @throws com.hackathon.agent.domain.exception.DomainException
     *         если {@code session} или {@code apartment} равны {@code null},
     *         либо если на любом этапе генерации или сохранения произошла ошибка
     * @see com.hackathon.agent.infrastructure.offer.OfferServiceImpl#createOffer(Session, Apartment)
     * @see OfferGeneratorService#generateOffer(Apartment, Session)
     * @see com.hackathon.agent.infrastructure.offer.OfferPdfGenerator#generatePdf(Apartment, Session)
     */
    Offer createOffer(Session session, Apartment apartment);

    /**
     * Возвращает коммерческое предложение по его уникальному идентификатору.
     * <p>
     * Используется для получения ранее созданного оффера, например, при
     * необходимости повторно выдать PDF-файл клиенту или отобразить детали
     * предложения.
     * </p>
     *
     * <p><b>Поведение при отсутствии:</b></p>
     * Если оффер с указанным идентификатором не найден в базе данных,
     * выбрасывается {@link com.hackathon.agent.domain.exception.DomainException}
     * с сообщением, содержащим переданный ID. Если {@code offerId} равен
     * {@code null}, исключение выбрасывается без обращения к репозиторию.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * try {
     *     Offer offer = offerService.getById(offerId);
     *     byte[] pdf = offer.getPdfData();
     * } catch (DomainException e) {
     *     // обработка отсутствия оффера
     * }
     * </pre>
     *
     * @param offerId уникальный идентификатор оффера (UUID); не должен быть
     *                {@code null}
     * @return найденный оффер {@link Offer} с заполненными полями, включая
     *         {@code pdfData}
     * @throws com.hackathon.agent.domain.exception.DomainException
     *         если {@code offerId} равен {@code null} или оффер с указанным ID
     *         не найден
     * @see com.hackathon.agent.infrastructure.offer.OfferServiceImpl#getById(UUID)
     */
    Offer getById(UUID offerId);

    /**
     * Находит последнее по времени создания коммерческое предложение
     * для указанной сессии (по внутреннему идентификатору).
     * <p>
     * Используется, когда известен внутренний UUID сессии (например, из
     * доменной модели {@link Session}). Возвращает {@link Optional}, что
     * позволяет обработать отсутствие оффера без выбрасывания исключения.
     * </p>
     *
     * <p><b>Поведение при отсутствии:</b></p>
     * Если для сессии ещё не создано ни одного коммерческого предложения,
     * возвращается пустой {@link Optional}. Если {@code sessionId} равен
     * {@code null}, также возвращается пустой {@link Optional} без обращения
     * к репозиторию.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * Optional&lt;Offer&gt; latest = offerService.findLatestBySessionId(session.getId());
     * latest.ifPresent(offer -&gt; {
     *     // работа с последним оффером сессии
     * });
     * </pre>
     *
     * @param sessionId внутренний UUID сессии; может быть {@code null}
     * @return {@link Optional} с последним оффером сессии, либо пустой
     *         {@link Optional}, если офферов нет или {@code sessionId} равен
     *         {@code null}
     * @see com.hackathon.agent.infrastructure.offer.OfferServiceImpl#findLatestBySessionId(UUID)
     * @see com.hackathon.agent.infrastructure.persistence.repository.OfferRepository#findFirstBySessionIdOrderByCreatedAtDesc(UUID)
     */
    Optional<Offer> findLatestBySessionId(UUID sessionId);

    /**
     * Находит последнее по времени создания коммерческое предложение
     * для указанной сессии по её публичному ключу ({@code X-Session-Id}).
     * <p>
     * Используется, когда клиент передаёт только строковый ключ сессии
     * (например, в заголовке {@code X-Session-Id}), а внутренний UUID сессии
     * неизвестен. Метод выполняет двухступенчатый поиск:
     * <ol>
     *     <li>Находит сущность сессии по ключу.</li>
     *     <li>Извлекает внутренний ID и делегирует поиск оффера в
     *         {@link #findLatestBySessionId(UUID)}.</li>
     * </ol>
     * </p>
     *
     * <p><b>Поведение при отсутствии:</b></p>
     * Если ключ сессии равен {@code null} или пуст, сессия с таким ключом
     * не найдена, либо для сессии нет офферов — возвращается пустой
     * {@link Optional}. Исключения не выбрасываются.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * Optional&lt;Offer&gt; latest = offerService.findLatestBySessionKey(sessionKey);
     * if (latest.isPresent()) {
     *     byte[] pdf = latest.get().getPdfData();
     * }
     * </pre>
     *
     * @param sessionKey публичный ключ сессии ({@code X-Session-Id});
     *                   может быть {@code null} или пустым
     * @return {@link Optional} с последним оффером сессии, либо пустой
     *         {@link Optional}, если ключ невалиден, сессия не найдена или
     *         у сессии нет офферов
     * @see com.hackathon.agent.infrastructure.offer.OfferServiceImpl#findLatestBySessionKey(String)
     * @see com.hackathon.agent.infrastructure.persistence.repository.SessionRepository#findBySessionKey(String)
     * @see #findLatestBySessionId(UUID)
     */
    Optional<Offer> findLatestBySessionKey(String sessionKey);
}