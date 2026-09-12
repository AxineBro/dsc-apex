package com.hackathon.agent.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.agent.domain.exception.UnknownSessionStateException;
import com.hackathon.agent.domain.model.Message;
import com.hackathon.agent.domain.model.Session;
import com.hackathon.agent.domain.model.SessionState;
import com.hackathon.agent.infrastructure.persistence.entity.SessionEntity;
import com.hackathon.agent.infrastructure.persistence.impl.SessionManagerImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Маппер для преобразования между доменной моделью {@link Session} и JPA-сущностью {@link SessionEntity}.
 * <p>
 * Обеспечивает двустороннее преобразование с сериализацией/десериализацией сложных JSON-полей
 * (история диалога и ранжированный список) через {@link ObjectMapper}.
 * Используется в {@link SessionManagerImpl} для сохранения и загрузки сессий из базы данных.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Разделение доменного слоя и инфраструктурного (JPA).</li>
 *     <li>Преобразование перечислений {@link SessionState} в строки и обратно.</li>
 *     <li>Сериализация списков в JSON-строки для хранения в текстовых колонках БД.</li>
 *     <li>Десериализация JSON-строк обратно в объекты списков.</li>
 * </ul>
 *
 * <p><b>JSON-поля:</b></p>
 * <ul>
 *     <li>{@code dialogHistoryJson} — массив объектов {@link Message}.</li>
 *     <li>{@code rankedListJson} — массив чисел (ID квартир).</li>
 * </ul>
 *
 * <p><b>Обработка ошибок:</b></p>
 * При ошибках сериализации/десериализации методы возвращают пустые списки
 * (для десериализации) или {@code null} (для сериализации), а ошибка логируется
 * в вызывающем коде (предполагается, что маппер не логирует самостоятельно).
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * SessionMapper mapper = ...;
 * Session session = new Session(...);
 * SessionEntity entity = mapper.toEntity(session);
 * repository.save(entity);
 *
 * Session restored = mapper.toDomain(entity);
 * </pre>
 *
 * @author Axine
 * @since 1.0.0
 * @see Session
 * @see SessionEntity
 * @see SessionManagerImpl
 */
@Component
@RequiredArgsConstructor
public class SessionMapper {

    private final ObjectMapper objectMapper;

    /**
     * Преобразует JPA-сущность {@link SessionEntity} в доменную модель {@link Session}.
     * <p>
     * Десериализует JSON-поля {@code dialogHistoryJson} и {@code rankedListJson}
     * в соответствующие списки. Статус преобразуется из строки в перечисление
     * {@link SessionState}. Если JSON-поля отсутствуют или повреждены, возвращаются
     * пустые списки. Значения счётчиков и флагов нормализуются (устанавливаются значения
     * по умолчанию при {@code null}).
     * </p>
     *
     * @param entity JPA-сущность (может быть {@code null})
     * @return доменная сессия или {@code null}, если входная сущность равна {@code null}
     */
    public Session toDomain(SessionEntity entity) {
        if (entity == null) return null;

        List<Message> dialogHistory = parseDialogHistory(entity.getDialogHistoryJson());
        List<Long> rankedList = parseRankedList(entity.getRankedListJson());
        try {
            return Session.builder()
                    .id(entity.getId())
                    .sessionKey(entity.getSessionKey())
                    .status(SessionState.valueOf(entity.getStatus()))
                    .areaMin(entity.getAreaMin())
                    .areaMax(entity.getAreaMax())
                    .priceMin(entity.getPriceMin())
                    .priceMax(entity.getPriceMax())
                    .roomsMin(entity.getRoomsMin())
                    .roomsMax(entity.getRoomsMax())
                    .floor(entity.getFloor())
                    .complex(entity.getComplex())
                    .counter(entity.getCounter() != null ? entity.getCounter() : 0)
                    .rankedList(rankedList)
                    .dialogHistory(dialogHistory)
                    .lastActivityAt(entity.getLastActivityAt())
                    .createdAt(entity.getCreatedAt())
                    .clarificationCount(entity.getClarificationCount())
                    .expandedOnce(entity.getExpandedOnce() != null ? entity.getExpandedOnce() : false)
                    .currentIndex(entity.getCurrentIndex() != null ? entity.getCurrentIndex() : 0)
                    .reminderSentAt(entity.getReminderSentAt())
                    .clientPhone(entity.getClientPhone())
                    .version(entity.getVersion())
                    .build();
        } catch (IllegalArgumentException e) {
            throw new UnknownSessionStateException(entity.getStatus());
        }
    }

    /**
     * Преобразует доменную модель {@link Session} в JPA-сущность {@link SessionEntity}.
     * <p>
     * Сериализует списки {@code dialogHistory} и {@code rankedList} в JSON-строки.
     * Статус преобразуется из перечисления в строковое представление.
     * Если список {@code null} или пуст, в БД сохраняется {@code null}.
     * </p>
     *
     * @param domain доменная сессия (может быть {@code null})
     * @return JPA-сущность или {@code null}, если входной объект равен {@code null}
     */
    public SessionEntity toEntity(Session domain) {
        if (domain == null) return null;

        return SessionEntity.builder()
                .id(domain.getId())
                .sessionKey(domain.getSessionKey())
                .status(domain.getStatus().name())
                .areaMin(domain.getAreaMin())
                .areaMax(domain.getAreaMax())
                .priceMin(domain.getPriceMin())
                .priceMax(domain.getPriceMax())
                .roomsMin(domain.getRoomsMin())
                .roomsMax(domain.getRoomsMax())
                .floor(domain.getFloor())
                .complex(domain.getComplex())
                .counter(domain.getCounter())
                .rankedListJson(serializeRankedList(domain.getRankedList()))
                .dialogHistoryJson(serializeDialogHistory(domain.getDialogHistory()))
                .lastActivityAt(domain.getLastActivityAt())
                .createdAt(domain.getCreatedAt())
                .clarificationCount(domain.getClarificationCount())
                .expandedOnce(domain.getExpandedOnce())
                .currentIndex(domain.getCurrentIndex())
                .reminderSentAt(domain.getReminderSentAt())
                .clientPhone(domain.getClientPhone())
                .version(domain.getVersion())
                .build();
    }

    /**
     * Десериализует JSON-строку в список сообщений {@link Message}.
     * <p>
     * Ожидается JSON-массив объектов с полями {@code role}, {@code text}, {@code timestamp}.
     * При ошибке парсинга возвращается пустой список.
     * </p>
     *
     * @param json JSON-строка (может быть {@code null} или пустой)
     * @return список сообщений (всегда не {@code null}, может быть пустым)
     */
    private List<Message> parseDialogHistory(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Сериализует список сообщений в JSON-строку.
     * <p>
     * Если список {@code null} или пуст, возвращает {@code null}.
     * При ошибке сериализации возвращает {@code null}.
     * </p>
     *
     * @param history список сообщений (может быть {@code null})
     * @return JSON-строка или {@code null}
     */
    private String serializeDialogHistory(List<Message> history) {
        if (history == null || history.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(history);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Десериализует JSON-строку в список Long (ID квартир).
     * <p>
     * Ожидается JSON-массив чисел. При ошибке парсинга возвращается пустой список.
     * </p>
     *
     * @param json JSON-строка (может быть {@code null} или пустой)
     * @return список ID (всегда не {@code null}, может быть пустым)
     */
    private List<Long> parseRankedList(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Сериализует список Long в JSON-строку.
     * <p>
     * Если список {@code null} или пуст, возвращает {@code null}.
     * При ошибке сериализации возвращает {@code null}.
     * </p>
     *
     * @param list список ID (может быть {@code null})
     * @return JSON-строка или {@code null}
     */
    private String serializeRankedList(List<Long> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}