package com.hackathon.agent.infrastructure.datasource.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.agent.domain.exception.ApartmentNotFoundException;
import com.hackathon.agent.domain.model.Apartment;
import com.hackathon.agent.domain.model.Filters;
import com.hackathon.agent.domain.service.ApartmentSearchService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Реализация сервиса поиска квартир {@link ApartmentSearchService}, загружающая данные
 * из статического JSON-файла, расположенного в classpath.
 * <p>
 * Используется в качестве заглушки (mock) для разработки и тестирования, когда нет
 * доступа к реальной ERP-системе. Данные загружаются однократно при инициализации
 * компонента через {@link PostConstruct}. Предоставляет базовую фильтрацию по
 * параметрам {@link Filters}, а также механизм расширенного поиска для увеличения
 * числа результатов.
 * </p>
 *
 * <p><b>Источник данных:</b></p>
 * <ul>
 *     <li>Файл: {@code classpath:data/apartments.json}.</li>
 *     <li>Формат: JSON-массив объектов {@link Apartment}.</li>
 *     <li>Загрузка выполняется один раз при старте приложения.</li>
 * </ul>
 *
 * <p><b>Особенности реализации:</b></p>
 * <ul>
 *     <li>Поиск {@link #findApartments} возвращает только квартиры со статусом
 *         {@code "свободна"} (регистр важен).</li>
 *     <li>Метод {@link #expandSearch} расширяет диапазоны фильтров на фиксированные
 *         проценты/значения и выполняет повторный поиск.</li>
 *     <li>Метод {@link #getById} возвращает {@code null}, если квартира с указанным ID
 *         не найдена (вместо выбрасывания исключения).</li>
 * </ul>
 *
 * <p><b>Обработка ошибок:</b></p>
 * При ошибке загрузки JSON-файла логируется ошибка, и список квартир инициализируется
 * пустым списком. Это позволяет приложению запуститься, но все поисковые запросы
 * будут возвращать пустые результаты.
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * ApartmentSearchService searchService = new JsonApartmentSearchService();
 * Filters filters = new Filters();
 * filters.setPriceMin(BigDecimal.valueOf(5000000));
 * filters.setPriceMax(BigDecimal.valueOf(10000000));
 * List&lt;Apartment&gt; results = searchService.findApartments(filters);
 * </pre>
 *
 * @author Axine
 * @since 1.0.0
 * @see ApartmentSearchService
 * @see Apartment
 * @see Filters
 */
@Service
@Slf4j
public class JsonApartmentSearchService implements ApartmentSearchService {

    /**
     * Список всех квартир, загруженных из JSON-файла.
     * <p>
     * Инициализируется в методе {@link #loadApartments()} и больше не изменяется.
     * Может быть пустым в случае ошибки загрузки.
     * </p>
     */
    private List<Apartment> apartments;

    /**
     * Путь к файлу с данными о квартирах в формате JSON, расположенному в classpath.
     * <p>
     * Значение задаётся через конфигурационное свойство {@code app.data.apartments}.
     * По умолчанию ожидается файл {@code data/apartments.json} в ресурсах приложения.
     * Используется в методе {@link #loadApartments()} для загрузки списка квартир
     * при инициализации сервиса.
     * </p>
     * <p>
     * <b>Пример значения:</b> {@code "data/apartments.json"}
     * </p>
     *
     * @see #loadApartments()
     * @see org.springframework.core.io.ClassPathResource
     */
    @Value("${app.data.apartments}")
    private String DATA_APARTMENTS;

    /**
     * Загружает список квартир из JSON-файла при инициализации компонента.
     * <p>
     * Аннотирована {@link PostConstruct}, поэтому вызывается автоматически
     * после внедрения зависимостей.
     * </p>
     *
     * <p><b>Алгоритм:</b></p>
     * <ol>
     *     <li>Создаёт экземпляр {@link ObjectMapper} для десериализации JSON.</li>
     *     <li>Загружает ресурс {@code data/apartments.json} из classpath.</li>
     *     <li>Десериализует содержимое в список {@link Apartment}.</li>
     *     <li>Логирует количество загруженных записей.</li>
     *     <li>В случае ошибки (файл не найден, невалидный JSON) логирует ошибку
     *         и устанавливает пустой список.</li>
     * </ol>
     */
    @PostConstruct
    public void loadApartments() {
        long start = System.currentTimeMillis();
        try {
            ObjectMapper mapper = new ObjectMapper();
            var resource = new ClassPathResource(DATA_APARTMENTS);
            apartments = mapper.readValue(resource.getInputStream(), new TypeReference<>() {});
            long duration = System.currentTimeMillis() - start;
            log.info("Loaded {} apartments from JSON in {} ms", apartments.size(), duration);
        } catch (IOException e) {
            log.error("Failed to load apartments from JSON: {}", e.getMessage(), e);
            apartments = List.of();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Реализация выполняет фильтрацию по статусу {@code "свободна"} и по всем
     * переданным критериям из объекта {@link Filters}. Квартиры с другими статусами
     * (например, {@code "забронирована"}, {@code "продана"}) исключаются из результатов.
     * </p>
     *
     * @param filters объект с критериями поиска (может быть {@code null} — тогда
     *                возвращаются все свободные квартиры)
     * @return список свободных квартир, соответствующих фильтрам (не {@code null},
     *         может быть пустым)
     */
    @Override
    public List<Apartment> findApartments(Filters filters) {
        long start = System.currentTimeMillis();
        log.debug("Searching apartments with filters: {}", filters);

        if (log.isDebugEnabled()) {
            log.debug("Filters: areaMin={}, areaMax={}, priceMin={}, priceMax={}, roomsMin={}, roomsMax={}, floor={}, complex={}",
                    filters != null ? filters.getAreaMin() : null,
                    filters != null ? filters.getAreaMax() : null,
                    filters != null ? filters.getPriceMin() : null,
                    filters != null ? filters.getPriceMax() : null,
                    filters != null ? filters.getRoomsMin() : null,
                    filters != null ? filters.getRoomsMax() : null,
                    filters != null ? filters.getFloor() : null,
                    filters != null ? filters.getComplex() : null);
        }
        List<Apartment> result = apartments.stream()
                .filter(a -> "свободна".equals(a.getStatus()))
                .filter(a -> matchFilters(a, filters))
                .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - start;
        log.info("Found {} apartments matching filters in {} ms", result.size(), duration);

        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Расширяет переданные фильтры следующим образом:
     * <ul>
     *     <li>Площадь: ±10% (умножение на 0.9 и 1.1).</li>
     *     <li>Цена: ±10%.</li>
     *     <li>Количество комнат: ±1 (минимум не ниже 1).</li>
     *     <li>Этаж: ±2 (без проверки на допустимые границы).</li>
     * </ul>
     * <p>
     * Все расширения применяются только к заданным полям фильтра (если значение
     * не {@code null}). После расширения выполняется обычный поиск через
     * {@link #findApartments}.
     * </p>
     *
     * @param filters исходные фильтры (не {@code null})
     * @return список квартир, найденных по расширенным критериям
     */
    @Override
    public List<Apartment> expandSearch(Filters filters) {
        long start = System.currentTimeMillis();
        log.debug("Expanding search with original filters: {}", filters);

        Filters expanded = new Filters(
                filters.getAreaMin() != null ? filters.getAreaMin().multiply(BigDecimal.valueOf(0.9)) : null,
                filters.getAreaMax() != null ? filters.getAreaMax().multiply(BigDecimal.valueOf(1.1)) : null,
                filters.getFloor() != null ? filters.getFloor() - 2 : null,
                filters.getPriceMin() != null ? filters.getPriceMin().multiply(BigDecimal.valueOf(0.9)) : null,
                filters.getPriceMax() != null ? filters.getPriceMax().multiply(BigDecimal.valueOf(1.1)) : null,
                filters.getRoomsMin() != null ? filters.getRoomsMin() - 1 : null,
                filters.getRoomsMax() != null ? filters.getRoomsMax() + 1 : null,
                filters.getComplex()
        );

        log.debug("Expanded filters: areaMin={}, areaMax={}, priceMin={}, priceMax={}, roomsMin={}, roomsMax={}, floor={}",
                expanded.getAreaMin(), expanded.getAreaMax(),
                expanded.getPriceMin(), expanded.getPriceMax(),
                expanded.getRoomsMin(), expanded.getRoomsMax(),
                expanded.getFloor());

        List<Apartment> result = findApartments(expanded);
        long duration = System.currentTimeMillis() - start;
        log.info("Expanded search found {} apartments in {} ms", result.size(), duration);
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Реализация выполняет поиск по идентификатору в загруженном списке квартир.
     * При отсутствии квартиры с указанным ID возвращает {@code null} вместо
     * выбрасывания исключения. Это поведение отличается от типичного для продакшена,
     * но допустимо для упрощённой реализации (заглушки).
     * </p>
     *
     * @param id уникальный идентификатор квартиры (не {@code null})
     * @return объект {@link Apartment} или {@code null}, если квартира не найдена
     */
    @Override
    public Apartment getById(Long id) {
        log.debug("Fetching apartment by id: {}", id);
        return apartments.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Apartment not found with id: {}", id);
                    return new ApartmentNotFoundException(id);
                });
    }

    @Override
    public int countBookedMatches(Filters filters) {
        if (apartments == null) return 0;
        return (int) apartments.stream()
                .filter(a -> !"свободна".equals(a.getStatus()))
                .filter(a -> matchFilters(a, filters))
                .count();
    }

    /**
     * Проверяет, соответствует ли квартира заданным фильтрам.
     * <p>
     * Внутренний вспомогательный метод, применяемый в {@link #findApartments}.
     * Сравнивает каждое поле фильтра с соответствующим полем квартиры.
     * Если поле фильтра равно {@code null}, ограничение не применяется.
     * </p>
     *
     * @param a квартира для проверки (не {@code null})
     * @param f объект фильтров (может быть {@code null} — тогда всегда возвращает {@code true})
     * @return {@code true}, если квартира соответствует всем указанным критериям,
     *         иначе {@code false}
     */
    private boolean matchFilters(Apartment a, Filters f) {
        if (f == null) return true;
        if (f.getAreaMin() != null && a.getArea().compareTo(f.getAreaMin()) < 0) return false;
        if (f.getAreaMax() != null && a.getArea().compareTo(f.getAreaMax()) > 0) return false;
        if (f.getFloor() != null && !a.getFloor().equals(f.getFloor())) return false;
        if (f.getPriceMin() != null && a.getPrice().compareTo(f.getPriceMin()) < 0) return false;
        if (f.getPriceMax() != null && a.getPrice().compareTo(f.getPriceMax()) > 0) return false;
        if (f.getRoomsMin() != null && a.getRooms() < f.getRoomsMin()) return false;
        if (f.getRoomsMax() != null && a.getRooms() > f.getRoomsMax()) return false;

        if (f.getComplex() != null && !f.getComplex().isBlank()) {
            String want = f.getComplex().trim().toLowerCase(Locale.ROOT);
            String have = a.getComplexName() != null ? a.getComplexName().toLowerCase(Locale.ROOT) : "";
            if (!have.contains(want)) return false;
        }
        return true;
    }
}