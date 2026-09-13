package com.hackathon.agent.infrastructure.persistence.mapper;

import com.hackathon.agent.domain.model.Offer;
import com.hackathon.agent.domain.model.OfferStatus;
import com.hackathon.agent.infrastructure.persistence.entity.OfferEntity;
import org.springframework.stereotype.Component;

/**
 * Маппер для преобразования между доменной моделью {@link Offer} и JPA-сущностью
 * {@link OfferEntity}.
 * <p>
 * Обеспечивает двустороннее преобразование данных коммерческого предложения (КП)
 * между слоями приложения: доменным (бизнес-логика) и инфраструктурным (база данных).
 * Используется в сервисах, работающих с офферами, для сохранения и загрузки
 * коммерческих предложений.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Разделение доменной модели {@link Offer} и JPA-сущности {@link OfferEntity}.</li>
 *     <li>Преобразование перечисления {@link OfferStatus} в строковый код и обратно.</li>
 *     <li>Обеспечение единообразного маппинга всех полей коммерческого предложения.</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * OfferMapper mapper = ...;
 * Offer domain = mapper.toDomain(entity);
 * OfferEntity entity = mapper.toEntity(domain);
 * </pre>
 *
 * <p><b>Обработка ошибок:</b></p>
 * <ul>
 *     <li>Если входной объект равен {@code null}, возвращается {@code null}.</li>
 *     <li>При преобразовании статуса из строки используется
 *         {@link OfferStatus#fromCode(String)}. Если код неизвестен, будет выброшено
 *         {@link IllegalArgumentException} (см. документацию {@link OfferStatus#fromCode}).</li>
 *     <li>Если статус в сущности равен {@code null}, по умолчанию используется
 *         {@link OfferStatus#NEW}.</li>
 * </ul>
 *
 * <p><b>Связь с другими компонентами:</b></p>
 * <ul>
 *     <li>Используется в
 *         {@link com.hackathon.agent.infrastructure.offer.OfferServiceImpl}
 *         для преобразования сущностей при сохранении и загрузке.</li>
 *     <li>Связан с {@link OfferEntity} (JPA-сущность) и {@link Offer} (доменная модель).</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see Offer
 * @see OfferEntity
 * @see OfferStatus
 * @see com.hackathon.agent.infrastructure.offer.OfferServiceImpl
 */
@Component
public class OfferMapper {

    /**
     * Преобразует JPA-сущность {@link OfferEntity} в доменную модель {@link Offer}.
     * <p>
     * Выполняет прямое сопоставление всех полей. Статус преобразуется из строкового
     * кода в значение перечисления {@link OfferStatus} с помощью
     * {@link OfferStatus#fromCode(String)}. Если поле {@code status} в сущности
     * равно {@code null}, по умолчанию устанавливается {@link OfferStatus#NEW}.
     * </p>
     *
     * <p><b>Обработка {@code null}:</b></p>
     * Если входная сущность равна {@code null}, метод возвращает {@code null},
     * не выполняя никаких действий.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * OfferEntity entity = offerRepository.findById(offerId).orElse(null);
     * Offer domain = mapper.toDomain(entity);
     * </pre>
     *
     * @param entity JPA-сущность коммерческого предложения; может быть {@code null}
     * @return доменная модель {@link Offer} или {@code null}, если входная сущность
     *         равна {@code null}
     * @throws IllegalArgumentException если строковый код статуса в сущности
     *         не соответствует ни одному значению {@link OfferStatus}
     * @see OfferStatus#fromCode(String)
     */
    public Offer toDomain(OfferEntity entity) {
        if (entity == null) return null;
        return Offer.builder()
                .id(entity.getId())
                .sessionId(entity.getSessionId())
                .apartmentId(entity.getApartmentId())
                .offerText(entity.getOfferText())
                .pdfData(entity.getPdfData())
                .createdAt(entity.getCreatedAt())
                .status(entity.getStatus() != null
                        ? OfferStatus.fromCode(entity.getStatus())
                        : OfferStatus.NEW)
                .build();
    }

    /**
     * Преобразует доменную модель {@link Offer} в JPA-сущность {@link OfferEntity}.
     * <p>
     * Выполняет прямое сопоставление всех полей. Статус преобразуется из значения
     * перечисления {@link OfferStatus} в строковый код с помощью
     * {@link OfferStatus#getCode()}. Если поле {@code status} в доменной модели
     * равно {@code null}, по умолчанию используется код {@link OfferStatus#NEW}.
     * </p>
     *
     * <p><b>Обработка {@code null}:</b></p>
     * Если входная доменная модель равна {@code null}, метод возвращает {@code null},
     * не выполняя никаких действий.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * Offer domain = offerService.createOffer(session, apartment);
     * OfferEntity entity = mapper.toEntity(domain);
     * offerRepository.save(entity);
     * </pre>
     *
     * @param domain доменная модель коммерческого предложения; может быть {@code null}
     * @return JPA-сущность {@link OfferEntity} или {@code null}, если входная модель
     *         равна {@code null}
     * @see OfferStatus#getCode()
     */
    public OfferEntity toEntity(Offer domain) {
        if (domain == null) return null;
        return OfferEntity.builder()
                .id(domain.getId())
                .sessionId(domain.getSessionId())
                .apartmentId(domain.getApartmentId())
                .offerText(domain.getOfferText())
                .pdfData(domain.getPdfData())
                .createdAt(domain.getCreatedAt())
                .status(domain.getStatus() != null ? domain.getStatus().getCode() : OfferStatus.NEW.getCode())
                .build();
    }
}