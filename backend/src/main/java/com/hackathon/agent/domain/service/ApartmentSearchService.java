package com.hackathon.agent.domain.service;

import com.hackathon.agent.domain.exception.ApartmentNotFoundException;
import com.hackathon.agent.domain.exception.InvalidFiltersException;
import com.hackathon.agent.domain.exception.NoApartmentsFoundException;
import com.hackathon.agent.domain.model.Apartment;
import com.hackathon.agent.domain.model.Filters;

import java.util.List;

/**
 * Сервис для поиска и получения квартир из внешней ERP-системы.
 * <p>
 * Предоставляет методы для фильтрации квартир по заданным критериям,
 * расширенного поиска (с более широкими параметрами) и получения
 * конкретной квартиры по её идентификатору.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Абстракция доступа к данным о квартирах (источник — ERP-система через API).</li>
 *     <li>Реализация поиска с учётом бизнес-правил (фильтрация, ранжирование, расширение).</li>
 *     <li>Предоставление единого интерфейса для сервисов подбора и бронирования.</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * ApartmentSearchService searchService = ...;
 *
 * // Поиск по фильтрам
 * Filters filters = new Filters();
 * filters.setPriceMin(BigDecimal.valueOf(5000000));
 * filters.setRoomsMin(2);
 * List&lt;Apartment&gt; results = searchService.findApartments(filters);
 *
 * // Расширенный поиск (если результатов мало)
 * List&lt;Apartment&gt; expanded = searchService.expandSearch(filters);
 *
 * // Получение конкретной квартиры
 * Apartment apartment = searchService.getById(1001L);
 * </pre>
 *
 * <p><b>Обработка ошибок:</b></p>
 * <ul>
 *     <li>При отсутствии квартиры с указанным ID выбрасывается {@link ApartmentNotFoundException}.</li>
 *     <li>При некорректных фильтрах выбрасывается {@link InvalidFiltersException}.</li>
 *     <li>При отсутствии результатов поиска может возвращаться пустой список (не {@code null})
 *         или выбрасываться {@link NoApartmentsFoundException} — зависит от реализации.</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see Apartment
 * @see Filters
 * @see ApartmentNotFoundException
 * @see InvalidFiltersException
 * @see NoApartmentsFoundException
 */
public interface ApartmentSearchService {

    /**
     * Выполняет поиск квартир по заданным фильтрам.
     * <p>
     * Возвращает список квартир, соответствующих всем указанным критериям фильтрации.
     * Фильтры применяются с использованием логического "И" (все условия должны быть выполнены).
     * Если фильтры не заданы (все поля {@code null}), возвращаются все доступные квартиры
     * (или ограниченное количество, определяемое реализацией).
     * </p>
     *
     * <p><b>Поведение при отсутствии результатов:</b></p>
     * В зависимости от реализации может возвращаться пустой список или выбрасываться
     * {@link NoApartmentsFoundException}. Рекомендуется возвращать пустой список
     * для упрощения обработки на клиенте, если не требуется явное уведомление.
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * Filters filters = new Filters();
     * filters.setPriceMin(BigDecimal.valueOf(3000000));
     * filters.setPriceMax(BigDecimal.valueOf(8000000));
     * filters.setRoomsMin(1);
     * List&lt;Apartment&gt; apartments = searchService.findApartments(filters);
     * </pre>
     *
     * @param filters объект с критериями поиска (может быть {@code null} — тогда поиск без ограничений)
     * @return список квартир, соответствующих фильтрам (может быть пустым, но не {@code null})
     * @throws InvalidFiltersException если фильтры содержат логически неверные значения
     *         (например, {@code priceMin} > {@code priceMax})
     */
    List<Apartment> findApartments(Filters filters);

    /**
     * Выполняет расширенный поиск квартир с более широкими критериями.
     * <p>
     * Используется в сценариях, когда основной поиск ({@link #findApartments}) вернул
     * недостаточно результатов (например, 0 или менее заданного порога). Расширенный поиск
     * ослабляет ограничения: увеличивает диапазоны цен и площадей, может включать соседние
     * районы или дома-аналоги.
     * </p>
     *
     * <p><b>Логика расширения (зависит от реализации):</b></p>
     * <ul>
     *     <li>Увеличение диапазона цен на заданный процент (например, ±20%).</li>
     *     <li>Расширение диапазона площади.</li>
     *     <li>Включение квартир с этажами "рядом" с желаемым.</li>
     *     <li>Добавление аналогичных ЖК.</li>
     * </ul>
     * <p>
     * Возвращаемый список может включать больше вариантов, чем {@code findApartments},
     * но уже с меньшим соответствием исходным критериям.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * Filters filters = new Filters();
     * filters.setPriceMin(BigDecimal.valueOf(5000000));
     * filters.setRoomsMin(2);
     * // Если результатов мало, расширяем поиск
     * List&lt;Apartment&gt; expanded = searchService.expandSearch(filters);
     * </pre>
     *
     * @param filters исходные фильтры, которые будут расширены (не {@code null})
     * @return список квартир, найденных по расширенным критериям
     *         (может быть пустым, но не {@code null})
     * @throws InvalidFiltersException если фильтры некорректны или не содержат
     *         обязательных параметров для расширения (например, все поля {@code null})
     */
    List<Apartment> expandSearch(Filters filters);

    /**
     * Возвращает квартиру по её уникальному идентификатору.
     * <p>
     * Получает полную информацию об объекте из ERP-системы.
     * Используется для отображения деталей квартиры, проверки статуса
     * и выполнения операций бронирования.
     * </p>
     *
     * <p><b>Поведение при отсутствии:</b></p>
     * Если квартира с указанным ID не найдена, выбрасывается
     * {@link ApartmentNotFoundException}. Это гарантирует, что вызывающий код
     * всегда получает валидный объект, если нет исключения.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * try {
     *     Apartment apartment = searchService.getById(1001L);
     *     // работа с квартирой
     * } catch (ApartmentNotFoundException e) {
     *     // обработка ошибки
     * }
     * </pre>
     *
     * @param id уникальный идентификатор квартиры в ERP-системе (не {@code null})
     * @return объект {@link Apartment} с полной информацией
     * @throws IllegalArgumentException если {@code id} равен {@code null}
     * @throws ApartmentNotFoundException если квартира с указанным ID не найдена
     */
    Apartment getById(Long id);

    /**
     * Возвращает количество не свободных квартир, удовлетворяющих заданным фильтрам.
     * <p>
     * Используется для оценки текущего спроса и доступности вариантов: например,
     * чтобы понять, сколько похожих квартир уже забронировано/продано, прежде чем предлагать
     * клиенту новые варианты или переходить к расширенному поиску
     * ({@link #expandSearch(Filters)}).
     * </p>
     *
     * <p><b>Особенности подсчёта:</b></p>
     * <ul>
     *     <li>Учитываются только квартиры со статусом отличным от «свободна».</li>
     *     <li>Фильтры применяются по тем же правилам, что и в {@link #findApartments}.</li>
     *     <li>Если {@code filters} пуст (все поля {@code null}), возвращается
     *         общее количество не свободных квартир.</li>
     * </ul>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * Filters filters = new Filters();
     * filters.setRoomsMin(2);
     * filters.setPriceMax(BigDecimal.valueOf(8000000));
     * int booked = searchService.countBookedMatches(filters);
     * if (booked > 10) {
     *     // спрос высокий — расширяем критерии поиска
     * }
     * </pre>
     *
     * @param filters фильтры поиска (не {@code null}; пустые поля игнорируются)
     * @return количество не свободных квартир, соответствующих фильтрам
     *         (может быть {@code 0}, но не отрицательным)
     * @throws InvalidFiltersException если фильтры содержат некорректные
     *         значения (например, {@code priceMin > priceMax})
     * @see #findApartments(Filters)
     * @see #expandSearch(Filters)
     */
    int countBookedMatches(Filters filters);
}