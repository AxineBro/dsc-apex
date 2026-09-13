package com.hackathon.agent.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA-сущность, представляющая коммерческое предложение (КП) в базе данных.
 * <p>
 * Отображается на таблицу {@code offers}. Содержит снимок сформированного
 * коммерческого предложения, включая текстовое описание и сгенерированный
 * PDF-файл в виде байтового массива. Используется для хранения и выдачи
 * PDF-документов клиентам через REST-контроллер
 * {@link com.hackathon.agent.api.controller.OfferController}.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Персистентное хранение коммерческих предложений, созданных в рамках
 *         диалоговых сессий.</li>
 *     <li>Обеспечение возможности повторной выдачи PDF-файла по запросу
 *         (например, при повторном обращении клиента).</li>
 *     <li>Предоставление данных для административных и аналитических задач
 *         (статистика по сформированным КП, конверсия и т.п.).</li>
 *     <li>Связь с сессией ({@code session_id}) и квартирой ({@code apartment_id})
 *         для контекстного поиска и аудита.</li>
 * </ul>
 *
 * <p><b>Связь с доменной моделью:</b></p>
 * Преобразование между доменным объектом
 * {@link com.hackathon.agent.domain.model.Offer} и данной сущностью выполняется
 * через {@link com.hackathon.agent.infrastructure.persistence.mapper.OfferMapper},
 * что обеспечивает разделение слоёв (Domain ↔ Infrastructure).
 *
 * <p><b>Структура таблицы {@code offers}:</b></p>
 * <ul>
 *     <li>{@code id} — первичный ключ типа UUID (генерируется автоматически).</li>
 *     <li>{@code session_id} — внутренний идентификатор сессии (UUID, обязательное поле).</li>
 *     <li>{@code apartment_id} — идентификатор квартиры (Long, обязательное поле).</li>
 *     <li>{@code offer_text} — текст коммерческого предложения (TEXT, опционально).</li>
 *     <li>{@code pdf_data} — PDF-файл в виде BLOB (опционально, может быть {@code null}
 *         до генерации).</li>
 *     <li>{@code created_at} — временная метка создания (устанавливается автоматически).</li>
 *     <li>{@code status} — строковый статус оффера (до 20 символов, обязательное поле).</li>
 * </ul>
 *
 * <p><b>Индексы:</b></p>
 * Для ускорения поиска по сессии и квартире созданы индексы:
 * <ul>
 *     <li>{@code idx_offers_session_id} — по колонке {@code session_id}.</li>
 *     <li>{@code idx_offers_apartment_id} — по колонке {@code apartment_id}.</li>
 * </ul>
 *
 * <p><b>Жизненный цикл:</b></p>
 * <ol>
 *     <li>Создание записи при вызове
 *         {@link com.hackathon.agent.domain.service.OfferService#createOffer}
 *         с начальным статусом {@code NEW} (PDF ещё не сгенерирован).</li>
 *     <li>Генерация PDF и обновление статуса на {@code READY}.</li>
 *     <li>При потере актуальности (бронь квартиры, истечение срока) статус
 *         переводится в {@code EXPIRED}.</li>
 * </ol>
 *
 * <p><b>Примечание по хранению PDF:</b></p>
 * В текущей реализации PDF-файл хранится непосредственно в базе данных как BLOB.
 * Для продакшен-окружения рекомендуется вынести бинарные данные в объектное
 * хранилище (например, S3, MinIO), а в сущности хранить только ключ или URL.
 * Это снизит нагрузку на БД и упростит масштабирование.
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * OfferEntity entity = OfferEntity.builder()
 *         .sessionId(session.getId())
 *         .apartmentId(apartment.getId())
 *         .offerText(offerText)
 *         .pdfData(pdfBytes)
 *         .status(OfferStatus.READY.getCode())
 *         .build();
 * offerRepository.save(entity);
 * </pre>
 *
 * @author Axine
 * @since 1.0.0
 * @see com.hackathon.agent.domain.model.Offer
 * @see com.hackathon.agent.domain.model.OfferStatus
 * @see com.hackathon.agent.infrastructure.persistence.mapper.OfferMapper
 * @see com.hackathon.agent.infrastructure.offer.OfferServiceImpl
 * @see com.hackathon.agent.infrastructure.persistence.repository.OfferRepository
 */
@Entity
@Table(name = "offers", indexes = {
        @Index(name = "idx_offers_session_id", columnList = "session_id"),
        @Index(name = "idx_offers_apartment_id", columnList = "apartment_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferEntity {

    /**
     * Уникальный идентификатор коммерческого предложения (первичный ключ).
     * <p>
     * Генерируется автоматически с использованием стратегии
     * {@link GenerationType#UUID}. Соответствует полю {@code id} доменной модели
     * {@link com.hackathon.agent.domain.model.Offer}.
     * </p>
     * <p>
     * Используется для ссылок на оффер в API (например, для скачивания PDF
     * по эндпоинту {@code /api/v1/offers/{offerId}/pdf}).
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Внутренний идентификатор сессии, к которой привязано коммерческое предложение.
     * <p>
     * Соответствует полю {@code id} сущности
     * {@link com.hackathon.agent.infrastructure.persistence.entity.SessionEntity}
     * и полю {@code sessionId} доменной модели
     * {@link com.hackathon.agent.domain.model.Offer}.
     * </p>
     * <p>
     * Обязательное поле (колонка {@code NOT NULL}). Позволяет восстановить контекст
     * диалога, в рамках которого было сформировано КП, и найти все офферы сессии.
     * </p>
     */
    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    /**
     * Идентификатор квартиры, для которой сформировано коммерческое предложение.
     * <p>
     * Соответствует полю {@code id} доменной модели
     * {@link com.hackathon.agent.domain.model.Apartment}.
     * </p>
     * <p>
     * Обязательное поле (колонка {@code NOT NULL}). Используется для получения
     * детальной информации о квартире при необходимости, а также для аналитики
     * по востребованности объектов.
     * </p>
     */
    @Column(name = "apartment_id", nullable = false)
    private Long apartmentId;

    /**
     * Текстовое содержимое коммерческого предложения.
     * <p>
     * Представляет собой заполненный шаблон КП, содержащий описание квартиры,
     * условия покупки и иную информацию, сгенерированную на основе данных сессии
     * и выбранного объекта. Хранится в колонке типа {@code TEXT}, что позволяет
     * сохранять длинные многострочные тексты.
     * </p>
     * <p>
     * Может быть {@code null}, если текст по какой-то причине не был сформирован,
     * однако в штатном режиме заполняется при создании оффера.
     * </p>
     *
     * @see com.hackathon.agent.domain.service.OfferGeneratorService#generateOffer
     */
    @Column(name = "offer_text", columnDefinition = "TEXT")
    private String offerText;

    /**
     * PDF-файл коммерческого предложения, хранящийся в базе данных как BLOB.
     * <p>
     * Содержит бинарные данные сгенерированного PDF-документа. Может быть
     * {@code null}, если PDF ещё не создан (статус {@code NEW}) или генерация
     * не удалась. После успешной генерации заполняется, и статус переводится
     * в {@code READY}.
     * </p>
     * <p>
     * <b>В продакшене доработаем:</b> вынести бинарные данные в объектное
     * хранилище (S3, MinIO), а в сущности хранить только ключ или URL. Это
     * снизит нагрузку на БД и упростит резервное копирование.
     * </p>
     *
     * @see com.hackathon.agent.infrastructure.offer.OfferPdfGenerator#generatePdf
     */
    @Lob
    @Column(name = "pdf_data")
    private byte[] pdfData;

    /**
     * Временная метка создания коммерческого предложения.
     * <p>
     * Устанавливается автоматически при вставке записи с помощью аннотации
     * {@link CreationTimestamp} (Hibernate). Поле доступно только для чтения
     * ({@code updatable = false}).
     * </p>
     * <p>
     * Используется для сортировки офферов, анализа давности предложений
     * и в методе
     * {@link com.hackathon.agent.infrastructure.persistence.repository.OfferRepository#findFirstBySessionIdOrderByCreatedAtDesc}
     * для поиска последнего КП сессии.
     * </p>
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Текущий статус коммерческого предложения в строковом представлении.
     * <p>
     * Обязательное поле (колонка {@code NOT NULL}), длина — до 20 символов.
     * Значения соответствуют кодам перечисления
     * {@link com.hackathon.agent.domain.model.OfferStatus}:
     * <ul>
     *     <li>{@code NEW} — оффер создан, PDF ещё не сгенерирован.</li>
     *     <li>{@code READY} — PDF успешно сгенерирован и доступен для скачивания.</li>
     *     <li>{@code EXPIRED} — оффер устарел (квартира забронирована, клиент отказался
     *         или истёк срок действия).</li>
     * </ul>
     * </p>
     * <p>
     * Преобразование в доменное перечисление выполняется в
     * {@link com.hackathon.agent.infrastructure.persistence.mapper.OfferMapper#toDomain}
     * с помощью {@link com.hackathon.agent.domain.model.OfferStatus#fromCode(String)}.
     * </p>
     *
     * @see com.hackathon.agent.domain.model.OfferStatus
     */
    @Column(name = "status", length = 20, nullable = false)
    private String status;
}