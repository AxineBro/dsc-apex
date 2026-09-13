package com.hackathon.agent.infrastructure.offer;

import com.hackathon.agent.domain.exception.DomainException;
import com.hackathon.agent.domain.model.Apartment;
import com.hackathon.agent.domain.model.Offer;
import com.hackathon.agent.domain.model.OfferStatus;
import com.hackathon.agent.domain.model.Session;
import com.hackathon.agent.domain.service.OfferGeneratorService;
import com.hackathon.agent.domain.service.OfferService;
import com.hackathon.agent.infrastructure.persistence.entity.SessionEntity;
import com.hackathon.agent.infrastructure.persistence.mapper.OfferMapper;
import com.hackathon.agent.infrastructure.persistence.repository.OfferRepository;
import com.hackathon.agent.infrastructure.persistence.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Реализация сервиса управления коммерческими предложениями {@link OfferService}.
 * <p>
 * Отвечает за полный цикл работы с КП: генерацию текстового описания, рендеринг
 * PDF-документа, сохранение в базе данных и последующее извлечение. Координирует
 * работу нескольких компонентов ({@link OfferGeneratorService},
 * {@link OfferPdfGenerator}) и репозиториев ({@link OfferRepository},
 * {@link SessionRepository}).
 * </p>
 *
 * <p><b>Основные обязанности:</b></p>
 * <ul>
 *     <li>Создание коммерческого предложения для выбранной квартиры и сессии:
 *         генерация текста, рендер PDF, сохранение в БД.</li>
 *     <li>Получение оффера по его идентификатору (UUID).</li>
 *     <li>Поиск последнего КП по внутреннему ID сессии или по публичному ключу
 *         сессии ({@code X-Session-Id}).</li>
 *     <li>Логирование ключевых этапов обработки и метрик производительности
 *         (длительность генерации, размер PDF).</li>
 * </ul>
 *
 * <p><b>Архитектурные особенности:</b></p>
 * <ul>
 *     <li><b>Транзакции:</b> Сам процесс создания оффера выполняется вне транзакции
 *         (рендер PDF — тяжёлая операция, которая может занимать секунды).
 *         Сохранение в БД вынесено в отдельный короткий транзакционный метод
 *         {@link #saveInTransaction(Offer)}, чтобы не удерживать соединение с БД
 *         на время рендеринга.</li>
 *     <li><b>Маппинг:</b> Преобразование между доменной моделью {@link Offer} и
 *         JPA-сущностью {@code OfferEntity} выполняется через {@link OfferMapper}.</li>
 *     <li><b>MDC-контекст:</b> Идентификатор сессии добавляется в MDC на время
 *         выполнения операции для сквозного логирования.</li>
 *     <li><b>Отказоустойчивость:</b> Все непредвиденные исключения оборачиваются
 *         в {@link DomainException} с сохранением исходной причины.</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * OfferService offerService = ...;
 *
 * // Создание КП для выбранной квартиры
 * Offer offer = offerService.createOffer(session, apartment);
 *
 * // Получение по ID
 * Offer found = offerService.getById(offerId);
 *
 * // Поиск последнего КП по ключу сессии
 * Optional&lt;Offer&gt; latest = offerService.findLatestBySessionKey(sessionKey);
 * </pre>
 *
 * <p><b>Зависимости:</b></p>
 * <ul>
 *     <li>{@link OfferRepository} — доступ к данным коммерческих предложений.</li>
 *     <li>{@link SessionRepository} — используется для поиска сессии по ключу при
 *         {@link #findLatestBySessionKey(String)}.</li>
 *     <li>{@link OfferMapper} — преобразование между доменной моделью и сущностью.</li>
 *     <li>{@link OfferPdfGenerator} — рендеринг PDF-документа.</li>
 *     <li>{@link OfferGeneratorService} — формирование текста КП по шаблону.</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see OfferService
 * @see Offer
 * @see OfferPdfGenerator
 * @see OfferGeneratorService
 * @see OfferMapper
 * @see OfferRepository
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;
    private final SessionRepository sessionRepository;
    private final OfferMapper offerMapper;
    private final OfferPdfGenerator pdfGenerator;
    private final OfferGeneratorService offerGeneratorService;

    /**
     * {@inheritDoc}
     * <p>
     * Реализация выполняет полный цикл создания коммерческого предложения:
     * генерацию текста, рендеринг PDF и сохранение в базе данных. Рендеринг PDF
     * выполняется <b>вне</b> транзакции БД, а сохранение — в отдельном
     * коротком транзакционном методе {@link #saveInTransaction(Offer)}.
     * </p>
     *
     * <p><b>Алгоритм:</b></p>
     * <ol>
     *     <li>Проверка входных параметров: {@code session} и {@code apartment}
     *         не должны быть {@code null}.</li>
     *     <li>Установка идентификатора сессии в MDC для логирования.</li>
     *     <li>Генерация текстовой части КП через {@link OfferGeneratorService}.</li>
     *     <li>Рендеринг PDF-документа через {@link OfferPdfGenerator}.</li>
     *     <li>Сборка доменной модели {@link Offer} со статусом
     *         {@link OfferStatus#READY}.</li>
     *     <li>Сохранение оффера через {@link #saveInTransaction(Offer)} с
     *         преобразованием в сущность и обратно.</li>
     *     <li>Логирование успешного завершения с указанием размера PDF
     *         и общей длительности операции.</li>
     * </ol>
     *
     * <p><b>Обработка ошибок:</b></p>
     * <ul>
     *     <li>{@link DomainException} пробрасывается без изменений (например,
     *         ошибки при генерации PDF или текста).</li>
     *     <li>Все прочие исключения оборачиваются в {@link DomainException}
     *         с сообщением «Не удалось создать коммерческое предложение» и
     *         сохранением исходной причины.</li>
     *     <li>В блоке {@code finally} выполняется очистка MDC от ключа
     *         {@code sessionId}.</li>
     * </ul>
     *
     * <p><b>Возвращаемое значение:</b></p>
     * Сохранённый оффер с заполненным {@code id}, {@code createdAt} и
     * установленным статусом {@code READY}.
     *
     * @param session   диалоговая сессия клиента (не {@code null}); используется
     *                  для получения ключа сессии, внутреннего ID и передачи
     *                  контекста в генераторы
     * @param apartment выбранная квартира (не {@code null}); источник данных
     *                  для заполнения шаблона КП и PDF
     * @return сохранённый оффер {@link Offer} со статусом {@link OfferStatus#READY}
     * @throws DomainException если {@code session} или {@code apartment} равны
     *         {@code null}, либо если на любом этапе генерации или сохранения
     *         произошла ошибка
     * @see #saveInTransaction(Offer)
     * @see OfferPdfGenerator#generatePdf(Apartment, Session)
     * @see OfferGeneratorService#generateOffer(Apartment, Session)
     */
    @Override
    public Offer createOffer(Session session, Apartment apartment) {
        if (session == null || apartment == null) {
            throw new DomainException("Session and Apartment must not be null");
        }
        MDC.put("sessionId", session.getSessionKey());
        long start = System.currentTimeMillis();

        try {
            String offerText = offerGeneratorService.generateOffer(apartment, session);
            log.debug("Текст КП сформирован, длина {} символов", offerText.length());

            byte[] pdf = pdfGenerator.generatePdf(apartment, session);

            Offer offer = Offer.builder()
                    .sessionId(session.getId())
                    .apartmentId(apartment.getId())
                    .offerText(offerText)
                    .pdfData(pdf)
                    .createdAt(LocalDateTime.now())
                    .status(OfferStatus.READY)
                    .build();

            Offer saved = saveInTransaction(offer);

            log.info("Оффер создан: id={}, apartmentId={}, pdfSize={}B, duration={}ms",
                    saved.getId(), apartment.getId(), pdf.length, System.currentTimeMillis() - start);
            return saved;
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("Ошибка создания оффера для сессии {}: {}", session.getSessionKey(), e.getMessage(), e);
            throw new DomainException("Не удалось создать коммерческое предложение", e);
        } finally {
            MDC.remove("sessionId");
        }
    }

    /**
     * Сохраняет коммерческое предложение в базе данных в рамках транзакции.
     * <p>
     * Вспомогательный метод, выделенный отдельно для того, чтобы длительная
     * операция рендеринга PDF ({@link #createOffer}) не выполнялась внутри
     * транзакции БД. Это позволяет не удерживать соединение с базой данных
     * на всё время генерации и избежать проблем с тайм-аутами пула соединений.
     * </p>
     *
     * <p><b>Алгоритм:</b></p>
     * <ol>
     *     <li>Преобразование доменной модели {@link Offer} в JPA-сущность через
     *         {@link OfferMapper#toEntity}.</li>
     *     <li>Сохранение сущности через {@link OfferRepository#save}.</li>
     *     <li>Обратное преобразование сохранённой сущности в доменную модель
     *         через {@link OfferMapper#toDomain}. Это гарантирует, что
     *         возвращаемый объект содержит актуальные поля, сгенерированные БД
     *         (например, {@code id} и {@code createdAt}).</li>
     * </ol>
     *
     * <p><b>Транзакционность:</b></p>
     * Метод аннотирован {@code @Transactional}, поэтому выполняется в рамках
     * отдельной короткой транзакции. При вызове извне класса (например, через
     * прокси) аннотация вступает в силу. Внутренние вызовы из того же класса
     * её игнорируют, поэтому метод вызывается именно из {@link #createOffer},
     * который сам не является транзакционным.
     * </p>
     *
     * @param offer доменная модель оффера для сохранения (не {@code null})
     * @return сохранённый оффер с заполненными полями {@code id} и
     *         {@code createdAt}
     * @see OfferMapper#toEntity(Offer)
     * @see OfferMapper#toDomain(com.hackathon.agent.infrastructure.persistence.entity.OfferEntity)
     * @see OfferRepository#save
     */
    @Transactional
    protected Offer saveInTransaction(Offer offer) {
        return offerMapper.toDomain(offerRepository.save(offerMapper.toEntity(offer)));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Реализация выполняет поиск оффера в БД по его идентификатору. Если запись
     * не найдена, выбрасывается {@link DomainException} с указанием ID.
     * </p>
     *
     * <p><b>Поведение при {@code null}:</b></p>
     * Если {@code offerId} равен {@code null}, сразу выбрасывается
     * {@link DomainException} без обращения к репозиторию.
     * </p>
     *
     * @param offerId уникальный идентификатор оффера (UUID); не должен быть
     *                {@code null}
     * @return найденный оффер {@link Offer}
     * @throws DomainException если {@code offerId} равен {@code null} или
     *         оффер с указанным ID не найден в базе данных
     * @see OfferRepository#findById
     */
    @Override
    public Offer getById(UUID offerId) {
        if (offerId == null) throw new DomainException("Offer id must not be null");
        return offerRepository.findById(offerId)
                .map(offerMapper::toDomain)
                .orElseThrow(() -> new DomainException("Offer not found: " + offerId));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Реализация ищет последний по времени создания оффер для указанной сессии
     * через {@link OfferRepository#findFirstBySessionIdOrderByCreatedAtDesc}.
     * Возвращает {@link Optional}, что позволяет вызывающему коду обработать
     * ситуацию отсутствия оффера без выбрасывания исключения.
     * </p>
     *
     * <p><b>Поведение при {@code null}:</b></p>
     * Если {@code sessionId} равен {@code null}, возвращается пустой
     * {@link Optional} без обращения к репозиторию.
     * </p>
     *
     * @param sessionId внутренний UUID сессии; может быть {@code null}
     * @return {@link Optional} с последним оффером сессии, либо пустой
     *         {@link Optional}, если офферов для сессии нет или {@code sessionId}
     *         равен {@code null}
     * @see OfferRepository#findFirstBySessionIdOrderByCreatedAtDesc(UUID)
     */
    @Override
    public Optional<Offer> findLatestBySessionId(UUID sessionId) {
        if (sessionId == null) return Optional.empty();
        return offerRepository.findFirstBySessionIdOrderByCreatedAtDesc(sessionId)
                .map(offerMapper::toDomain);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Реализация выполняет двухступенчатый поиск:
     * <ol>
     *     <li>Находит сущность сессии по публичному ключу через
     *         {@link SessionRepository#findBySessionKey(String)}.</li>
     *     <li>Извлекает внутренний {@code id} сессии и делегирует поиск
     *         оффера в {@link #findLatestBySessionId(UUID)}.</li>
     * </ol>
     * </p>
     *
     * <p><b>Поведение при некорректном ключе:</b></p>
     * Если {@code sessionKey} равен {@code null} или состоит только из пробелов,
     * возвращается пустой {@link Optional} без обращения к репозиторию.
     * Если сессия с указанным ключом не найдена, также возвращается пустой
     * {@link Optional} (исключение не выбрасывается).
     * </p>
     *
     * <p><b>Использование:</b></p>
     * Применяется в контроллере
     * {@link com.hackathon.agent.api.controller.OfferController#downloadBySessionKey}
     * для выдачи PDF-файла, когда клиент передаёт только {@code X-Session-Id},
     * не зная внутренний UUID оффера.
     * </p>
     *
     * @param sessionKey публичный ключ сессии ({@code X-Session-Id}); может быть
     *                   {@code null} или пустым
     * @return {@link Optional} с последним оффером сессии, либо пустой
     *         {@link Optional}, если ключ невалиден, сессия не найдена или
     *         у сессии нет офферов
     * @see #findLatestBySessionId(UUID)
     * @see SessionRepository#findBySessionKey(String)
     * @see com.hackathon.agent.api.controller.OfferController#downloadBySessionKey(String)
     */
    @Override
    public Optional<Offer> findLatestBySessionKey(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) return Optional.empty();
        return sessionRepository.findBySessionKey(sessionKey)
                .map(SessionEntity::getId)
                .flatMap(this::findLatestBySessionId);
    }
}