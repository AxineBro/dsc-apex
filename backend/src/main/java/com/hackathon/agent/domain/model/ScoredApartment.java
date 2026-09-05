package com.hackathon.agent.domain.model;

import com.hackathon.agent.application.orchestrator.SessionManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object), представляющий квартиру с числовой оценкой (скором)
 * для ранжирования в результатах поиска.
 * <p>
 * Используется в алгоритмах ранжирования и подбора квартир для клиента.
 * Каждый объект содержит исходную квартиру ({@link Apartment}) и вычисленный балл,
 * который определяет релевантность или приоритет квартиры относительно запроса пользователя.
 * Список таких объектов может быть отсортирован по убыванию скора для выдачи наиболее
 * подходящих вариантов.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Инкапсуляция пары "квартира + скор" для передачи между слоями приложения.</li>
 *     <li>Хранение ранжированного списка в сессии через {@link SessionManager#setRankedList}.</li>
 *     <li>Возврат клиенту с сортировкой по скору для отображения лучших предложений.</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * // Ранжирование списка квартир
 * List&lt;ScoredApartment&gt; ranked = apartments.stream()
 *         .map(a -> new ScoredApartment(a, calculateScore(a, userPreferences)))
 *         .sorted(Comparator.comparing(ScoredApartment::getScore).reversed())
 *         .collect(Collectors.toList());
 *
 * // Сохранение в сессии
 * sessionManager.setRankedList(session, ranked.stream()
 *         .map(ScoredApartment::getApartment)
 *         .map(Apartment::getId)
 *         .collect(Collectors.toList()));
 *
 * // Возврат клиенту (без скора, только квартиры с порядком)
 * return ranked.stream().map(ScoredApartment::getApartment).collect(Collectors.toList());
 * </pre>
 *
 * <p><b>Значение скора:</b></p>
 * <ul>
 *     <li>Чем выше скор, тем более подходящая квартира для клиента.</li>
 *     <li>Может рассчитываться на основе близости к бюджету, предпочтениям по площади,
 *         этажу, наличию парковки и других факторов.</li>
 *     <li>Часто используется в диапазоне от 0 до 100, но может быть любым целым числом.</li>
 *     <li>Скор {@code null} означает, что оценка не была вычислена — такие объекты
 *         обычно исключаются из ранжирования или помещаются в конец списка.</li>
 * </ul>
 *
 * <p><b>Примечание:</b></p>
 * Объект содержит {@code Apartment} целиком, а не только его ID, чтобы избежать
 * дополнительных запросов к БД при отображении результатов.
 *
 * @author Axine
 * @since 1.0.0
 * @see Apartment
 * @see SessionManager#setRankedList
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScoredApartment {

    /**
     * Квартира, для которой вычислен скор.
     * <p>
     * Содержит полную информацию об объекте недвижимости.
     * Не может быть {@code null} — каждая оценка привязана к конкретной квартире.
     * </p>
     *
     * @see Apartment
     */
    private Apartment apartment;

    /**
     * Числовая оценка (скор) квартиры.
     * <p>
     * Определяет релевантность квартиры для текущего запроса пользователя.
     * Чем выше значение, тем более подходящей считается квартира.
     * </p>
     * <p>
     * Может быть {@code null} в исключительных случаях (например, если оценка
     * ещё не вычислена). При ранжировании {@code null} обычно обрабатывается
     * как минимальное значение (помещается в конец списка).
     * </p>
     * <p>
     * <b>Пример:</b> скор = 95 означает высокую степень соответствия запросу,
     * скор = 20 — низкую.
     * </p>
     */
    private Integer score;
}