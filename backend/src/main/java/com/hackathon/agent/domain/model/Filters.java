package com.hackathon.agent.domain.model;

import com.hackathon.agent.domain.exception.InvalidFiltersException;
import com.hackathon.agent.infrastructure.catalog.ComplexCatalog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) для параметров фильтрации квартир при поиске.
 * <p>
 * Содержит набор критериев, которые пользователь или система могут задать
 * для сужения результатов поиска по квартирам. Все поля являются опциональными
 * ({@code null} означает, что фильтр не применяется).
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Инкапсуляция фильтров для передачи между слоями (контроллер → сервис → репозиторий).</li>
 *     <li>Поддержка динамического построения запросов к БД или внешнему API на основе переданных параметров.</li>
 *     <li>Предоставление метода {@link #isEmpty()} для проверки наличия активных фильтров.</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * Объект {@code Filters} обычно создаётся на основе запроса пользователя
 * (из параметров URL или тела запроса) и передаётся в сервисный слой:
 * <pre>
 * // Пример создания фильтров
 * Filters filters = Filters.builder()
 *         .priceMin(BigDecimal.valueOf(5000000))
 *         .priceMax(BigDecimal.valueOf(15000000))
 *         .roomsMin(2)
 *         .roomsMax(3)
 *         .build();
 *
 * // Проверка на пустоту
 * if (!filters.isEmpty()) {
 *     List&lt;Apartment&gt; results = apartmentService.search(filters);
 * }
 * </pre>
 *
 * <p><b>Примечание:</b></p>
 * Все фильтры объединены логическим "И" — результат должен удовлетворять
 * всем указанным условиям. Отсутствие фильтра (null) означает, что ограничение
 * не применяется.
 *
 * @author Axine
 * @since 1.0.0
 * @see Apartment
 * @see InvalidFiltersException
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Filters {

    /**
     * Минимальная площадь квартиры в квадратных метрах (включительно).
     * <p>
     * Если задано, возвращаются только квартиры с площадью {@code >= areaMin}.
     * </p>
     * <p>
     * <b>Пример:</b> {@code areaMin = 40.0} означает поиск квартир площадью от 40 м².
     * </p>
     *
     * @see #areaMax
     */
    private BigDecimal areaMin;

    /**
     * Максимальная площадь квартиры в квадратных метрах (включительно).
     * <p>
     * Если задано, возвращаются только квартиры с площадью {@code <= areaMax}.
     * </p>
     * <p>
     * <b>Пример:</b> {@code areaMax = 60.0} означает поиск квартир площадью до 60 м².
     * </p>
     *
     * @see #areaMin
     */
    private BigDecimal areaMax;

    /**
     * Конкретный этаж, на котором должна располагаться квартира.
     * <p>
     * Если задано, возвращаются только квартиры на указанном этаже.
     * </p>
     * <p>
     * <b>Пример:</b> {@code floor = 5} — только квартиры на 5-м этаже.
     * </p>
     */
    private Integer floor;

    /**
     * Минимальная цена квартиры в рублях (включительно).
     * <p>
     * Если задано, возвращаются только квартиры с ценой {@code >= priceMin}.
     * </p>
     * <p>
     * <b>Пример:</b> {@code priceMin = 10000000} — квартиры от 10 млн руб.
     * </p>
     *
     * @see #priceMax
     */
    private BigDecimal priceMin;

    /**
     * Максимальная цена квартиры в рублях (включительно).
     * <p>
     * Если задано, возвращаются только квартиры с ценой {@code <= priceMax}.
     * </p>
     * <p>
     * <b>Пример:</b> {@code priceMax = 15000000} — квартиры до 15 млн руб.
     * </p>
     *
     * @see #priceMin
     */
    private BigDecimal priceMax;

    /**
     * Минимальное количество комнат (включительно).
     * <p>
     * Если задано, возвращаются только квартиры с числом комнат {@code >= roomsMin}.
     * </p>
     * <p>
     * <b>Пример:</b> {@code roomsMin = 2} — квартиры от 2 комнат.
     * </p>
     *
     * @see #roomsMax
     */
    private Integer roomsMin;

    /**
     * Максимальное количество комнат (включительно).
     * <p>
     * Если задано, возвращаются только квартиры с числом комнат {@code <= roomsMax}.
     * </p>
     * <p>
     * <b>Пример:</b> {@code roomsMax = 3} — квартиры до 3 комнат включительно.
     * </p>
     *
     * @see #roomsMin
     */
    private Integer roomsMax;

    /**
     * Название или идентификатор жилого комплекса, к которому относится квартира.
     * <p>
     * Используется для фильтрации и группировки квартир в каталоге
     * ({@link ComplexCatalog}). Значение может быть {@code null}, если квартира
     * не привязана к конкретному ЖК или информация ещё не заполнена.
     * </p>
     * <p>
     * <b>Пример:</b> {@code complex = "Северный парк"} — квартира относится
     * к жилому комплексу «Северный парк».
     * </p>
     *
     * @see ComplexCatalog
     */
    private String complex;

    /**
     * Проверяет, задан ли хотя бы один фильтр (не {@code null}).
     * <p>
     * Используется для определения, нужно ли применять фильтрацию при поиске.
     * Метод возвращает {@code true}, если все поля равны {@code null}.
     * </p>
     *
     * <p><b>Пример использования:</b></p>
     * <pre>
     * Filters filters = new Filters();
     * boolean empty = filters.isEmpty(); // true
     *
     * filters.setPriceMin(BigDecimal.TEN);
     * empty = filters.isEmpty(); // false
     * </pre>
     *
     * @return {@code true}, если ни один фильтр не установлен, иначе {@code false}
     */
    public boolean isEmpty() {
        return areaMin == null && areaMax == null && floor == null &&
                priceMin == null && priceMax == null &&
                roomsMin == null && roomsMax == null &&
                (complex == null || complex.isBlank());
    }
}