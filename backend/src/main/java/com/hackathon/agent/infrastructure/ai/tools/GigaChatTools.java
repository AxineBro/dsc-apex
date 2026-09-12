package com.hackathon.agent.infrastructure.ai.tools;

import com.hackathon.agent.api.dto.response.ChatResponse;
import com.hackathon.agent.application.orchestrator.AgentOrchestrator;
import com.hackathon.agent.application.orchestrator.SessionManager;
import com.hackathon.agent.domain.exception.ApartmentNotFoundException;
import com.hackathon.agent.domain.model.Apartment;
import com.hackathon.agent.domain.model.Filters;
import com.hackathon.agent.domain.model.ScoredApartment;
import com.hackathon.agent.domain.model.Session;
import com.hackathon.agent.domain.model.SessionState;
import com.hackathon.agent.domain.service.ApartmentSearchService;
import com.hackathon.agent.domain.service.NotificationService;
import com.hackathon.agent.domain.service.OfferGeneratorService;
import com.hackathon.agent.domain.service.ScoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Набор инструментов (tools) для AI-агента, предоставляющий функциональность
 * для взаимодействия с бизнес-логикой чат-бота.
 * <p>
 * Класс регистрируется как Spring-компонент и аннотируется {@link Tool} над методами,
 * что позволяет AI-модели (GigaChat) вызывать эти методы в процессе генерации ответа.
 * Каждый метод представляет собой отдельное действие, которое может выполнить агент:
 * поиск квартир, оценка, генерация предложения, передача менеджеру и т.д.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Интеграция AI-модели с бизнес-сервисами через функциональные вызовы.</li>
 *     <li>Выполнение операций, требующих доступа к внешним системам (ERP, БД, уведомления).</li>
 *     <li>Управление состоянием сессии (сохранение, обновление, чтение).</li>
 *     <li>Обеспечение безопасного и контролируемого выполнения действий в контексте текущей сессии.</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * Методы вызываются AI-агентом на основе сгенерированных инструкций.
 * Например, модель может решить выполнить {@link #searchApartments} для получения
 * списка квартир по запросу пользователя, затем {@link #rateApartments} для их
 * ранжирования, а затем {@link #generateOffer} для создания коммерческого предложения.
 *
 * <p><b>Взаимодействие с сессией:</b></p>
 * Почти все методы используют текущую сессию, получаемую через
 * {@link SessionManager#getCurrentSession()}, которая устанавливается в контексте потока
 * перед вызовом инструментов (в {@link AgentOrchestrator}). Это гарантирует,
 что операции выполняются в правильном контексте.
 *
 * <p><b>Обработка ошибок:</b></p>
 * В случае отсутствия сессии или критической ошибки методы логируют предупреждение
 * и возвращают пустой список или сообщение об ошибке. Это предотвращает сбои AI-агента.
 *
 * @author Axine
 * @since 1.0.0
 * @see AgentOrchestrator
 * @see SessionManager
 * @see ApartmentSearchService
 * @see ScoringService
 * @see OfferGeneratorService
 * @see NotificationService
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GigaChatTools {

    private final SessionManager sessionManager;
    private final ApartmentSearchService searchService;
    private final ScoringService scoringService;
    private final OfferGeneratorService offerService;
    private final NotificationService notificationService;

    /**
     * Максимальное количество циклов подбора (расширений поиска) в рамках одной сессии.
     * При достижении лимита сессия передаётся менеджеру.
     * Значение читается из конфигурации: {@code app.max.cycles}.
     */
    @Value("${app.max.cycles}")
    private int MAX_CYCLES;

    /**
     * Количество квартир, возвращаемых методом {@link #getTopApartments}.
     * Значение читается из конфигурации: {@code app.flat.limit}.
     */
    @Value("${app.flat.limit}")
    private int FLAT_LIMIT;

    /**
     * Причина последней передачи диалога менеджеру в текущем потоке (запросе).
     * <p>
     * Позволяет передать причину из глубины бизнес-логики (где вызывается
     * {@code transferToManager}) наверх — в слой API, где она попадает
     * в {@link ChatResponse}, без «протаскивания» через сигнатуры методов.
     * </p>
     * <p>
     * {@link ThreadLocal} обеспечивает изоляцию между параллельными запросами.
     * Устанавливается через {@link ThreadLocal#set(Object)} в момент передачи менеджеру,
     * читается при сборке {@link ChatResponse}. <b>Обязательно</b> очищать
     * через {@link ThreadLocal#remove()} по завершении обработки запроса
     * (в {@code finally}-блоке), иначе значение протечёт в следующий запрос из пула потоков.
     * </p>
     * <p>
     * Если не был установлен — {@link ThreadLocal#get()} вернёт {@code null},
     * и {@link ChatResponse} останется {@code null}.
     * </p>
     */
    private final ThreadLocal<String> lastTransferReason = new ThreadLocal<>();

    /**
     * Маскирует номер телефона для безопасного логирования.
     * <p>
     * Оставляет видимыми только последние 4 символа, остальные заменяет
     * на {@code "***"}. Используется при выводе телефона клиента в лог,
     * чтобы не раскрывать персональные данные в открытом виде.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * maskPhone("+79991234567")  // "***4567"
     * maskPhone("123")           // "не указан"
     * maskPhone(null)            // "не указан"
     * </pre>
     *
     * @param p исходный номер телефона; может быть {@code null}
     *          или короче 4 символов
     * @return строка вида {@code "***XXXX"} с последними 4 символами номера,
     *         либо {@code "не указан"}, если номер {@code null} или слишком короткий
     */
    private static String maskPhone(String p) {
        if (p == null || p.length() < 4) return "не указан";
        return "***" + p.substring(p.length() - 4);
    }

    /**
     * Выполняет поиск квартир по заданным параметрам фильтрации.
     * <p>
     * Инструмент для AI-агента. Используется для получения списка доступных квартир
     * на основе предпочтений пользователя (площадь, цена, комнаты, этаж).
     * </p>
     *
     * <p><b>Логика работы:</b></p>
     * <ol>
     *     <li>Получает текущую сессию из контекста (ThreadLocal).</li>
     *     <li>Создаёт объект {@link Filters} на основе переданных параметров.</li>
     *     <li>Сохраняет фильтры в сессии через {@link SessionManager#updateFilters}.</li>
     *     <li>Проверяет, не превышен ли лимит циклов (счётчик counter >= MAX_CYCLES,
     *         значение задаётся параметром app.max.cycles) — если да,
     *         вызывает {@link #transferToManager} и возвращает пустой список.</li>
     *     <li>Выполняет поиск через {@link ApartmentSearchService#findApartments}.</li>
     *     <li>Если результат пуст и расширение ещё не применялось (флаг {@code expandedOnce}),
     *         выполняет расширенный поиск через {@link ApartmentSearchService#expandSearch}
     *         и устанавливает флаг {@code expandedOnce = true}.</li>
     *     <li>Сохраняет ранжированный список (список ID) в сессии через
     *         {@link SessionManager#setRankedList}.</li>
     *     <li>Возвращает список найденных квартир.</li>
     * </ol>
     *
     * <p><b>Параметры:</b></p>
     * Все параметры опциональны ({@code null} означает отсутствие фильтра).
     *
     * @param areaMin минимальная площадь (кв.м)
     * @param areaMax максимальная площадь (кв.м)
     * @param floor   желаемый этаж (конкретный номер)
     * @param priceMin минимальная цена (руб)
     * @param priceMax максимальная цена (руб)
     * @param roomsMin минимальное количество комнат
     * @param roomsMax максимальное количество комнат
     * @return список квартир, соответствующих фильтрам (может быть пустым, но не {@code null})
     */
    @Tool(description = """
    Поиск квартир по заданным параметрам. Используй этот инструмент, когда клиент предоставил хотя бы один критерий (площадь, цена, комнаты, этаж).
    Если параметры не указаны – сначала задай уточняющие вопросы через askClarification.
    Все значения могут быть null (означает отсутствие фильтра).
    Инструмент автоматически сохранит фильтры в сессии и увеличит счётчик циклов только при изменении бюджета или площади.
    Если по основным фильтрам ничего не найдено, будет автоматически применено расширение (один раз).
    Возвращает список квартир (может быть пустым). Если пусто – предложи клиенту изменить параметры или вызови transferToManager.
    Важно: если параметр не указан, передавай null. Для этажа не используй 0 – это будет воспринято как конкретный этаж. Если этаж неизвестен, передай null
    """)
    public List<Apartment> searchApartments(
            @ToolParam(description = "Минимальная площадь в кв.м (BigDecimal)") BigDecimal areaMin,
            @ToolParam(description = "Максимальная площадь в кв.м (BigDecimal)") BigDecimal areaMax,
            @ToolParam(description = "Желаемый этаж (Integer, конкретное число)") Integer floor,
            @ToolParam(description = "Минимальная цена в рублях (BigDecimal)") BigDecimal priceMin,
            @ToolParam(description = "Максимальная цена в рублях (BigDecimal)") BigDecimal priceMax,
            @ToolParam(description = "Минимальное количество комнат (Integer)") Integer roomsMin,
            @ToolParam(description = "Максимальное количество комнат (Integer)") Integer roomsMax,
            @ToolParam(description = "ЖК, подстрока названия, например 'Ласточкино'") String complex
    ) {
        log.info("Tool searchApartments called: areaMin={}, areaMax={}, floor={}, priceMin={}, priceMax={}, roomsMin={}, roomsMax={}, complex={}",
                areaMin, areaMax, floor, priceMin, priceMax, roomsMin, roomsMax, complex);

        Session session = sessionManager.getCurrentSession();
        if (session == null) {
            log.warn("Текущая сессия не найдена в ThreadLocal, поиск невозможен");
            return List.of();
        }
        MDC.put("sessionId", session.getSessionKey());

        long startTime = System.currentTimeMillis();
        try {
            if (floor != null && floor <= 0) {
                floor = null;
            }
            if (roomsMin != null && roomsMin <= 0) {
                roomsMin = null;
            }
            if (roomsMax != null && roomsMax <= 0) {
                roomsMax = null;
            }

            Filters filters = new Filters(areaMin, areaMax, floor, priceMin, priceMax, roomsMin, roomsMax, complex);
            log.debug("Фильтры для поиска: {}", filters);

            sessionManager.updateFilters(session, filters);

            if (session.getCounter() >= MAX_CYCLES) {
                log.warn("Превышен лимит циклов ({} >= {}), переводим на менеджера", session.getCounter(), MAX_CYCLES);
                transferToManager("limit_cycles", null);
                return List.of();
            }

            List<Apartment> apartments = searchService.findApartments(filters);
            log.debug("Найдено {} квартир по основным фильтрам", apartments.size());

            if (apartments.isEmpty() && !session.getExpandedOnce()) {
                log.info("Поиск не дал результатов, применяем расширенный поиск (один раз)");
                apartments = searchService.expandSearch(filters);
                log.debug("Расширенный поиск нашёл {} квартир", apartments.size());
                session.setExpandedOnce(true);
                sessionManager.save(session);
            }

            List<Long> ids = apartments.stream().map(Apartment::getId).collect(Collectors.toList());
            sessionManager.setRankedList(session, ids);
            log.debug("Ранжированный список сохранён в сессии, количество ID: {}", ids.size());

            long duration = System.currentTimeMillis() - startTime;
            log.info("searchApartments завершён: найдено {} квартир, duration={}ms", apartments.size(), duration);

            return apartments;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Ошибка в searchApartments: duration={}ms, error={}", duration, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Возвращает количество квартир, подходящих под заданные фильтры,
     * но уже забронированных или проданных.
     * <p>
     * Инструмент для AI-агента. Используется в сценарии, когда
     * {@code searchApartments} не вернул свободных вариантов: если
     * {@code bookedCount > 0}, агент может сообщить клиенту, что по его
     * критериям есть забронированные квартиры, и предложить передать диалог
     * менеджеру для уточнения (через {@link #transferToManager} с
     * {@code reason="booked"}).
     * </p>
     *
     * <p><b>Особенности:</b></p>
     * <ul>
     *     <li>Параметры совпадают с {@code searchApartments}; {@code null}
     *         означает «без фильтра».</li>
     *     <li>Некорректные значения {@code floor}, {@code roomsMin}, {@code roomsMax}
     *         ({@code <= 0}) игнорируются (приравниваются к {@code null}).</li>
     *     <li>Если текущая сессия отсутствует — возвращается {@code 0}.</li>
     *     <li>При любой ошибке подсчёта возвращается {@code 0} (исключение не
     *         пробрасывается, чтобы не ломать диалог агента).</li>
     * </ul>
     *
     * <p><b>Пример вызова агентом:</b></p>
     * <pre>
     * searchApartments(...) → пусто
     * countBookedMatches(areaMin=50, priceMax=8000000, roomsMin=2) → 3
     * // Агент отвечает: «По вашим критериям есть 3 забронированные квартиры,
     * // могу передать менеджеру для уточнения»
     * </pre>
     *
     * @param areaMin  минимальная площадь; {@code null} — без ограничения
     * @param areaMax  максимальная площадь; {@code null} — без ограничения
     * @param floor    этаж; {@code null} или значение {@code <= 0} — без ограничения
     * @param priceMin минимальная цена; {@code null} — без ограничения
     * @param priceMax максимальная цена; {@code null} — без ограничения
     * @param roomsMin минимальное число комнат; {@code null} или {@code <= 0} — без ограничения
     * @param roomsMax максимальное число комнат; {@code null} или {@code <= 0} — без ограничения
     * @param complex  подстрока названия ЖК; {@code null} — без ограничения
     * @return количество забронированных или проданных квартир, удовлетворяющих
     *         фильтрам; {@code 0}, если ничего не найдено, сессия отсутствует
     *         или произошла ошибка
     * @see #transferToManager(String, String)
     */
    @Tool(description = """
    Сколько квартир подходит под те же фильтры, но уже забронировано/продано (не свободно).
    Вызывай, когда searchApartments вернул пусто: если bookedCount > 0 — скажи клиенту
    «по вашим критериям есть N забронированных, могу передать менеджеру для уточнения» и при согласии вызови transferToManager с reason="booked".
    Все параметры как у searchApartments, null = без фильтра.
    """)
    public int countBookedMatches(
            @ToolParam(description = "Минимальная площадь") BigDecimal areaMin,
            @ToolParam(description = "Максимальная площадь") BigDecimal areaMax,
            @ToolParam(description = "Этаж") Integer floor,
            @ToolParam(description = "Минимальная цена") BigDecimal priceMin,
            @ToolParam(description = "Максимальная цена") BigDecimal priceMax,
            @ToolParam(description = "Комнат мин") Integer roomsMin,
            @ToolParam(description = "Комнат макс") Integer roomsMax,
            @ToolParam(description = "ЖК, подстрока названия") String complex
    ) {
        log.info("Tool countBookedMatches called: areaMin={}, areaMax={}, floor={}, priceMin={}, priceMax={}, roomsMin={}, roomsMax={}, complex={}",
                areaMin, areaMax, floor, priceMin, priceMax, roomsMin, roomsMax, complex);

        Session session = sessionManager.getCurrentSession();
        if (session == null) {
            log.warn("Текущая сессия не найдена в ThreadLocal, подсчёт забронированных невозможен");
            return 0;
        }
        MDC.put("sessionId", session.getSessionKey());

        long startTime = System.currentTimeMillis();
        try {
            if (floor != null && floor <= 0) {
                floor = null;
            }
            if (roomsMin != null && roomsMin <= 0) {
                roomsMin = null;
            }
            if (roomsMax != null && roomsMax <= 0) {
                roomsMax = null;
            }

            Filters filters = new Filters(areaMin, areaMax, floor, priceMin, priceMax, roomsMin, roomsMax, complex);
            log.debug("Фильтры для подсчёта забронированных: {}", filters);

            int count = searchService.countBookedMatches(filters);

            long duration = System.currentTimeMillis() - startTime;
            log.info("countBookedMatches завершён: найдено {} забронированных/проданных, duration={}ms", count, duration);

            return count;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Ошибка в countBookedMatches: duration={}ms, error={}", duration, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Возвращает первые {@code FLAT_LIMIT} квартир из ранжированного списка сессии.
     * <p>
     * Количество определяется параметром конфигурации {@code app.flat.limit}.
     * </p>
     * <p>
     * Инструмент для AI-агента. Используется для извлечения топ-результатов после поиска
     * и ранжирования.
     * </p>
     *
     * @return список из первых 5 квартир (или меньше, если в списке меньше объектов),
     *         либо пустой список, если сессия отсутствует или список ранжированных ID пуст
     */
    @Tool(description = """
    Получить первые несколько квартир из ранжированного списка (количество задаётся конфигурацией app.flat.limit).
    Используй этот инструмент после вызова rateApartments, чтобы показать клиенту топ-вариантов.
    Если ранжированный список пуст или не задан – вернёт пустой список.
    Вызывай этот инструмент только тогда, когда хочешь показать начальный набор вариантов.
    """)
    public List<Apartment> getTopApartments() {
        log.debug("Tool getTopApartments called");
        Session session = sessionManager.getCurrentSession();
        if (session == null) {
            log.warn("Текущая сессия не найдена в ThreadLocal, getTopApartments возвращает пустой список");
            return List.of();
        }
        MDC.put("sessionId", session.getSessionKey());

        if (session.getRankedList() == null || session.getRankedList().isEmpty()) {
            log.debug("Ранжированный список сессии пуст или null");
            return List.of();
        }

        List<Long> ids = session.getRankedList().stream().limit(FLAT_LIMIT).toList();
        List<Apartment> result = ids.stream()
                .map(searchService::getById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        log.debug("getTopApartments вернул {} квартир (из запрошенных {})", result.size(), ids.size());

        return result;
    }

    /**
     * Оценивает список квартир по неметрическим параметрам с помощью AI-сервиса.
     * <p>
     * Инструмент для AI-агента. Передаёт список ID квартир в {@link ScoringService}
     * для вычисления скора на основе таких критериев, как вид из окна, наличие парковки,
     * этаж и другие качественные факторы.
     * </p>
     *
     * @param apartmentIds список идентификаторов квартир для оценки (не {@code null})
     * @return список объектов {@link ScoredApartment} с вычисленными оценками
     */
    @Tool(description = """
    Оценивает список квартир по неметрическим параметрам (вид из окна, наличие парковки, престижность этажа, близость к метро и т.п.).
    Вызывай этот инструмент после получения списка квартир от searchApartments.
    Передай список ID квартир (можно взять из результата searchApartments или из ранжированного списка сессии).
    Возвращает список объектов ScoredApartment с оценкой 0-100. Используй эти оценки для сортировки и отображения лучших вариантов.
    Если список ID пуст или null – результат будет пустым.
    """)
    public List<ScoredApartment> rateApartments(
            @ToolParam(description = "Список ID квартир для оценки (List<Long>)") List<Long> apartmentIds
    ) {
        log.info("Tool rateApartments called с {} ID квартир", apartmentIds != null ? apartmentIds.size() : 0);
        if (apartmentIds == null || apartmentIds.isEmpty()) {
            log.debug("Список ID пуст, возвращаем пустой список");
            return List.of();
        }

        Session session = sessionManager.getCurrentSession();
        if (session != null) {
            MDC.put("sessionId", session.getSessionKey());
        } else {
            log.warn("Текущая сессия не найдена, но продолжаем оценку без контекста сессии");
        }

        long startTime = System.currentTimeMillis();
        try {
            List<ScoredApartment> result = scoringService.rateApartments(apartmentIds);
            long duration = System.currentTimeMillis() - startTime;
            log.info("rateApartments завершён: оценено {} квартир, duration={}ms", result.size(), duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Ошибка в rateApartments: duration={}ms, error={}", duration, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Возвращает следующую квартиру из ранжированного списка сессии.
     * <p>
     * Инструмент для AI-агента. Используется для пошагового перебора предложений
     * клиентом. Увеличивает индекс текущей позиции в сессии.
     * </p>
     *
     * @return следующая квартира из списка, или {@code null}, если список пуст или индекс вне границ
     */
    @Tool(description = """
    Показать следующую квартиру из полного ранжированного списка (без изменения фильтров и без увеличения счётчика циклов).
    Используй этот инструмент, когда клиент говорит «покажи следующий», «ещё», «не нравится этот» и т.п.
    Каждый вызов сдвигает внутренний индекс на один шаг вперёд.
    Если список исчерпан – вернёт null. В этом случае предложи клиенту изменить параметры или связаться с менеджером.
    """)
    public Apartment getNextApartment() {
        log.debug("Tool getNextApartment called");
        Session session = sessionManager.getCurrentSession();
        if (session == null) {
            log.warn("Текущая сессия не найдена в ThreadLocal");
            return null;
        }
        MDC.put("sessionId", session.getSessionKey());

        List<Long> ranked = session.getRankedList();
        if (ranked == null || ranked.isEmpty()) {
            log.debug("Ранжированный список пуст");
            return null;
        }

        int index = session.getCurrentIndex() != null ? session.getCurrentIndex() : 0;
        if (index >= ranked.size()) {
            log.debug("Достигнут конец списка (index={}, size={})", index, ranked.size());
            return null;
        }

        Long id = ranked.get(index);
        session.setCurrentIndex(index + 1);
        sessionManager.save(session);
        log.debug("Возвращена квартира с ID {} (индекс был {})", id, index);
        return searchService.getById(id);
    }

    /**
     * Переводит диалог на менеджера с указанием причины.
     * <p>
     * Инструмент для AI-агента. Используется в случаях, когда агент не может
     * обработать запрос (ошибка, нехватка данных, запрос пользователя).
     * Выполняет следующие действия:
     * <ol>
     *     <li>Если {@code phone} не передан, подставляет значение из {@code session.getClientPhone()}.</li>
     *     <li>Сохраняет причину перевода в {@code ThreadLocal} (для последующего извлечения
     *         через {@link #drainLastTransferReason()}).</li>
     *     <li>Создаёт задачу для менеджера через {@link NotificationService#notifyManager}.</li>
     *     <li>Устанавливает статус сессии в {@link SessionState#TO_MANAGER} и сохраняет её.</li>
     * </ol>
     * </p>
     *
     * <p><b>Особенности поведения:</b></p>
     * <ul>
     *     <li>Метод <b>не пробрасывает исключения</b> — при любой ошибке (включая
     *         отсутствие текущей сессии) возвращается строка {@code "ok"}, чтобы
     *         AI-агент корректно завершил генерацию ответа.</li>
     *     <li>Если текущая сессия не установлена в {@code ThreadLocal}, метод
     *         немедленно возвращает {@code "ok"} без каких-либо действий —
     *         это означает, что перевод менеджеру фактически <b>не произошёл</b>.
     *         При отладке следует проверять наличие сессии в контексте потока.</li>
     *     <li>При ошибке вызова {@link NotificationService} причина удаляется
     *         из {@code ThreadLocal}, но статус сессии <b>остаётся прежним</b>,
     *         и метод всё равно возвращает {@code "ok"}.</li>
     * </ul>
     *
     * @param reason причина перевода (например, {@code "timeout"}, {@code "no_data"},
     *               {@code "limit_cycles"}, {@code "user_request"})
     * @param phone  номер телефона клиента (если известен, может быть {@code null} —
     *               тогда будет использован {@code session.getClientPhone()})
     * @return строка {@code "ok"} — единый ответ для AI-агента в любом случае
     *         (успех, отсутствие сессии, ошибка сохранения)
     * @see #drainLastTransferReason()
     * @see NotificationService#notifyManager(Session, String, String)
     */
    @Tool(description = """
    Перевести диалог на менеджера с указанием причины.
    Используй этот инструмент в следующих случаях:
    - клиент явно просит менеджера;
    - после двух уточняющих вопросов по параметрам данные всё ещё неполные;
    - после расширенного поиска квартир не найдено;
    - достигнут лимит циклов (5 изменений бюджета или площади);
    - клиент задаёт вопросы не по теме (ипотека, юристы, стройматериалы);
    - клиент слишком долго не отвечает (автоматически, но можно вызвать при необходимости).
    Параметр phone – номер телефона клиента. Если номер уже был сохранён через saveClientPhone, ты можешь передать его сюда (или оставить null, тогда он будет взят из сессии автоматически).
    Рекомендуется перед вызовом убедиться, что номер сохранён (если клиент согласился его дать). Если номер неизвестен, а клиент не хочет его давать, передай null и укажи причину "no_phone".
    """)
    public String transferToManager(
            @ToolParam(description = "Причина перевода (одна из перечисленных выше)") String reason,
            @ToolParam(description = "Телефон клиента (опционально, но желательно)") String phone
    ) {
        log.info("Tool transferToManager called: reason={}, phone={}", reason, maskPhone(phone));
        Session session = sessionManager.getCurrentSession();
        if (session == null) {
            log.warn("Текущая сессия не найдена для перевода на менеджера");
            return "ok";
        }
        MDC.put("sessionId", session.getSessionKey());

        if (phone == null || phone.isBlank()) {
            phone = session.getClientPhone();
            log.info("Номер телефона не был передан, данные взяты из сессии. Сессия: {}, загруженный номер: {}", session.getSessionKey(), maskPhone(phone));
        }
        try {
            lastTransferReason.set(reason);
            notificationService.notifyManager(session, reason, phone);
            session.setStatus(SessionState.TO_MANAGER);
            sessionManager.save(session);
            log.info("Диалог переведён на менеджера. Сессия: {}, причина: {}", session.getSessionKey(), reason);
            return "ok";
        } catch (Exception e) {
            lastTransferReason.remove();
            log.error("Ошибка при переводе на менеджера для сессии {}: {}", session.getSessionKey(), e.getMessage(), e);
            return "ok";
        }
    }

    /**
     * Генерирует коммерческое предложение для выбранной квартиры.
     * <p>
     * Инструмент для AI-агента. Используется, когда пользователь выбрал конкретную квартиру
     * и запрашивает финальное предложение. Проверяет статус квартиры (должна быть свободна)
     * и генерирует текст оффера через {@link OfferGeneratorService}.
     * Статус сессии устанавливается в {@link SessionState#OFFER_READY}.
     * </p>
     *
     * @param apartmentId ID выбранной квартиры
     * @return текст коммерческого предложения или сообщение об ошибке, если квартира недоступна
     */
    @Tool(description = """
    Сформировать коммерческое предложение для выбранной квартиры.
    Вызывай этот инструмент, когда клиент явно выбрал квартиру (по номеру или описанию) и хочет оформить покупку.
    Инструмент проверит статус квартиры в реальном времени – если она уже забронирована, вернёт сообщение об ошибке.
    В случае успеха вернёт текст предложения, а статус сессии будет изменён на OFFER_READY.
    После вызова этого инструмента диалог можно завершить или предложить записаться на просмотр.
    ВАЖНО: верни пользователю ТОЧНО ТОТ ЖЕ ТЕКСТ, который вернул этот инструмент, без изменений, без добавлений и без перефразирования. Не пиши "коммерческое предложение сформировано" – просто верни полученный текст.
    """)
    public String generateOffer(
            @ToolParam(description = "ID выбранной квартиры (Long)") Long apartmentId
    ) {
        log.info("Tool generateOffer called: apartmentId={}", apartmentId);
        Session session = sessionManager.getCurrentSession();
        if (session == null) {
            log.warn("Текущая сессия не найдена для генерации КП");
            return "Извините, произошла ошибка. Обратитесь к менеджеру.";
        }
        MDC.put("sessionId", session.getSessionKey());

        if (apartmentId == null) {
            log.warn("apartmentId равен null");
            return "Не указан ID квартиры.";
        }

        long startTime = System.currentTimeMillis();
        try {
            Apartment apartment = searchService.getById(apartmentId);
            if (apartment == null) {
                log.warn("Квартира с ID {} не найдена", apartmentId);
                return "Квартира с таким ID не найдена.";
            }

            if (!"свободна".equals(apartment.getStatus())) {
                log.warn("Квартира ID {} не свободна, статус: {}", apartmentId, apartment.getStatus());
                return "К сожалению, эта квартира уже забронирована. Выберите другой вариант.";
            }

            String offerText = offerService.generateOffer(apartment, session);
            session.setStatus(SessionState.OFFER_READY);
            sessionManager.save(session);
            long duration = System.currentTimeMillis() - startTime;
            log.info("Коммерческое предложение сгенерировано для квартиры ID {}, длина текста {}, duration={}ms",
                    apartmentId, offerText.length(), duration);
            return offerText;
        }catch (ApartmentNotFoundException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("Квартира с ID {} не найдена в базе: duration={}ms, error={}",
                    apartmentId, duration, e.getMessage(), e);
            return "Квартиры с номером " + apartmentId + " нет в списке. Пожалуйста, выберите вариант из предложенного списка и укажите его номер (ID).";
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Ошибка при генерации КП для квартиры ID {}: duration={}ms, error={}",
                    apartmentId, duration, e.getMessage(), e);
            return "Произошла техническая ошибка. Пожалуйста, попробуйте позже или свяжитесь с менеджером.";
        }
    }

    /**
     * Задаёт уточняющий вопрос клиенту для получения недостающих параметров.
     * <p>
     * Инструмент для AI-агента. Используется, когда не хватает данных для поиска
     * (например, не указан бюджет или количество комнат). Ограничение — максимум 2 вопроса
     * на сессию, после чего агент передаёт диалог менеджеру.
     * </p>
     *
     * @param question текст вопроса для клиента
     * @return переданный вопрос или сообщение о переходе к менеджеру, если лимит превышен
     */
    @Tool(description = """
    Задать уточняющий вопрос клиенту, чтобы получить недостающие параметры.
    Максимум 2 вызова за сессию. Если после двух вопросов данные неполные – автоматически будет вызван transferToManager.
    Используй этот инструмент только когда клиент не указал один из ключевых параметров (площадь, цена, комнаты, этаж).
    Не задавай вопросы, на которые можно ответить «да/нет» – вопросы должны быть открытыми и конкретными.
    Важно: аргумент question должен содержать только текст вопроса, без лишних символов, кодов или истории диалога. Вопрос должен быть кратким и конкретным
    """)
    public String askClarification(
            @ToolParam(description = "Текст вопроса для клиента") String question
    ) {
        log.info("Tool askClarification called: question='{}'", question);
        Session session = sessionManager.getCurrentSession();
        if (session == null) {
            log.warn("Текущая сессия не найдена");
            return "Ошибка сессии";
        }
        MDC.put("sessionId", session.getSessionKey());


        int count = session.getClarificationCount() != null ? session.getClarificationCount() : 0;
        if (count >= 2) {
            log.warn("Превышен лимит уточняющих вопросов ({}), переводим на менеджера", count);
            transferToManager("too_many_questions", null);
            return "Вы уже задали максимальное число вопросов. Я переключу вас на менеджера.";
        }
        session.incrementClarificationCount();
        sessionManager.save(session);
        log.debug("Уточняющий вопрос задан (теперь count={})", session.getClarificationCount());

        return question;
    }

    /**
     * Полностью сбрасывает состояние сессии, обнуляя все фильтры, счётчики и историю ранжирования.
     * <p>
     * Инструмент для AI-агента. Используется, когда клиент хочет начать диалог заново
     * без создания новой сессии. Сбрасывает все параметры подбора, счётчики и флаги,
     * устанавливает статус в {@link SessionState#NEW}.
     * </p>
     *
     * @return сообщение о сбросе диалога с предложением начать заново
     */
    @Tool(description = """
    Полностью сбросить состояние сессии: обнулить все фильтры, счётчики, ранжированный список и установить статус NEW.
    Используй этот инструмент, когда клиент говорит «начать заново», «сбросить», «давай с чистого листа».
    После сброса начни диалог заново – попроси клиента ввести параметры поиска.
    """)
    public String resetSession() {
        log.info("Tool resetSession called");
        Session session = sessionManager.getCurrentSession();
        if (session == null) {
            log.warn("Текущая сессия не найдена");
            return "Ошибка сессии";
        }
        MDC.put("sessionId", session.getSessionKey());
        try {
            session.setAreaMin(null);
            session.setAreaMax(null);
            session.setPriceMin(null);
            session.setPriceMax(null);
            session.setRoomsMin(null);
            session.setRoomsMax(null);
            session.setFloor(null);
            session.setCounter(0);
            session.resetClarificationCount();
            session.resetExpandedOnce();
            session.setRankedList(new ArrayList<>());
            session.setCurrentIndex(0);
            session.setStatus(SessionState.NEW);
            sessionManager.save(session);
            log.info("Сессия {} сброшена к состоянию NEW", session.getSessionKey());
            return "Диалог сброшен. Давайте начнём заново. Напишите, какие параметры вас интересуют.";
        } catch (Exception e) {
            log.error("Ошибка при сбросе сессии {}: {}", session.getSessionKey(), e.getMessage(), e);
            return "Не удалось сбросить диалог. Попробуйте позже.";
        }
    }

    /**
     * Сохраняет номер телефона клиента в текущей сессии для последующего использования.
     * <p>
     * Инструмент для AI-агента. Используется, когда клиент явно или косвенно сообщает свой
     * контактный номер (например, в тексте диалога). Сохранённый номер автоматически
     * подставляется в задачу менеджеру при вызове {@link #transferToManager}, что позволяет
     * избежать повторных запросов контакта.
     * </p>
     *
     * <p><b>Логика работы:</b></p>
     * <ol>
     *     <li>Получает текущую сессию из контекста (ThreadLocal) через {@link SessionManager#getCurrentSession()}.</li>
     *     <li>Если сессия отсутствует, возвращает сообщение об ошибке (AI-агент должен воспринимать это как сигнал к завершению диалога).</li>
     *     <li>Сохраняет переданную строку номера в поле {@code clientPhone} сессии.</li>
     *     <li>Сохраняет обновлённую сессию через {@link SessionManager#save(Session)}.</li>
     *     <li>Возвращает подтверждающее сообщение для отображения клиенту.</li>
     * </ol>
     *
     * <p><b>Важные рекомендации для AI-агента:</b></p>
     * <ul>
     *     <li>Вызывайте этот инструмент как можно раньше, если клиент сам предоставил номер,
     *         даже если вы ещё не планируете передавать диалог менеджеру. Это избавит от
     *         повторных запросов в будущем.</li>
     *     <li>Перед вызовом приведите номер к единому формату: оставьте только цифры и символ «+»,
     *         если он был указан (например, для международного формата). Пробелы, скобки и дефисы
     *         можно удалить – это не влияет на сохранение и последующую передачу менеджеру.</li>
     *     <li>Не вызывайте этот инструмент повторно, если номер уже сохранён (проверить можно
     *         через {@code session.getClientPhone()}), и не запрашивайте номер заново у клиента
     *         после успешного сохранения.</li>
     *     <li>После выполнения инструмента полученное сообщение «Номер сохранён. Спасибо!»
     *         рекомендуется показать клиенту, чтобы он знал, что контакт принят.</li>
     * </ul>
     *
     * <p><b>Примеры фраз клиента, после которых следует вызывать инструмент:</b></p>
     * <ul>
     *     <li>«мой номер 89123456789»</li>
     *     <li>«звоните на +7(912)345-67-89»</li>
     *     <li>«телефон для связи: 8-912-345-67-89»</li>
     * </ul>
     *
     * @param phone номер телефона клиента в виде строки (может содержать пробелы, скобки,
     *              дефисы, знак «+»); не должен быть {@code null} или пустым – в противном случае
     *              сохранение бессмысленно, но метод всё равно выполнит операцию (рекомендуется
     *              проверять перед вызовом)
     * @return строка подтверждения «Номер сохранён. Спасибо!» в случае успеха, или
     *         «Ошибка сессии», если текущая сессия не найдена
     * @see SessionManager#save(Session)
     * @see #transferToManager(String, String)
     */
    @Tool(description = """
    Сохранить номер телефона клиента в текущей сессии.
    Используй этот инструмент, когда клиент явно или косвенно сообщил свой номер телефона.
    Примеры фраз клиента: «мой номер 89123456789», «звоните на +7(912)345-67-89», «телефон для связи: 8-912-345-67-89».
    Извлеки номер из сообщения, приведи к единому формату (достаточно оставить только цифры и знак «+», если он был), и передай в этот инструмент.
    Сохранённый номер автоматически подставится в задачу менеджеру при вызове transferToManager.
    Если номер уже сохранён (вы можете проверить это через session.getClientPhone(), но обычно не нужно), повторно не вызывай этот инструмент и не запрашивай номер заново.
    После успешного сохранения инструмент вернёт подтверждение «Номер сохранён. Спасибо!» – его можно показать клиенту, чтобы он знал, что контакт принят.
    Важно: вызывай этот инструмент как можно раньше, если клиент сам дал номер, даже если вы ещё не планируете передавать диалог менеджеру. Это избавит от повторных запросов в будущем.
    """)
    public String saveClientPhone(
            @ToolParam(description = "Номер телефона клиента (строка, можно с пробелами и скобками)") String phone
    ) {
        Session session = sessionManager.getCurrentSession();
        if (session == null) return "Ошибка сессии";
        String normalized = normalizeRuPhone(phone);
        if (normalized == null) {
            return "Не смог распознать номер. Попросите клиента прислать номер в формате +7XXXXXXXXXX.";
        }
        session.setClientPhone(normalized);
        sessionManager.save(session);
        return "Номер сохранён. Спасибо!";
    }

    /**
     * Забирает и очищает причину последней передачи диалога менеджеру для текущего потока.
     * <p>
     * Возвращает значение {@link #lastTransferReason}, после чего удаляет его
     * через {@link ThreadLocal#remove()}. Совмещение «чтения» и «очистки» в одном методе
     * гарантирует, что причина не «протечёт» в следующий запрос, обработанный тем же
     * потоком из пула.
     * </p>
     * <p>
     * Вызывается один раз по завершении обработки запроса — при формировании
     * {@link ChatResponse}
     * </p>
     *
     * @return причина передачи менеджеру (например, {@code "user_request"},
     *         {@code "low_confidence"}), либо {@code null}, если в текущем
     *         потоке передачи менеджеру не было
     * @see #lastTransferReason
     * @see ChatResponse
     */
    public String drainLastTransferReason() {
        String r = lastTransferReason.get();
        lastTransferReason.remove();
        return r;
    }

    /**
     * Приводит номер телефона к единому российскому формату {@code +7XXXXXXXXXX}.
     * <p>
     * Используется для нормализации телефонов клиентов, введённых в свободной форме
     * (с пробелами, скобками, дефисами, ведущей {@code 8} и т.п.), перед сохранением
     * в задаче менеджера и сравнением с уже существующими записями.
     * </p>
     *
     * <p><b>Правила нормализации:</b></p>
     * <ul>
     *     <li>Из строки удаляются все символы, кроме цифр.</li>
     *     <li>11 цифр, начинающихся с {@code 8} или {@code 7} — приводятся к виду
     *         {@code 7XXXXXXXXXX}.</li>
     *     <li>10 цифр — дополняются ведущей {@code 7}.</li>
     *     <li>Любое другое количество цифр — номер считается некорректным.</li>
     * </ul>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * normalizeRuPhone("8 (999) 123-45-67")  // "+79991234567"
     * normalizeRuPhone("9991234567")         // "+79991234567"
     * normalizeRuPhone("+7 999 123 45 67")   // "+79991234567"
     * normalizeRuPhone("12345")              // null
     * normalizeRuPhone(null)                 // null
     * </pre>
     *
     * @param raw исходная строка с номером телефона; может быть {@code null}
     *            или содержать произвольные разделители
     * @return нормализованный номер в формате {@code +7XXXXXXXXXX}
     *         или {@code null}, если номер пуст, некорректен или не является
     *         российским
     */
    private static String normalizeRuPhone(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.length() == 11 && (digits.startsWith("8") || digits.startsWith("7"))) {
            digits = "7" + digits.substring(1);
        } else if (digits.length() == 10) {
            digits = "7" + digits;
        } else {
            return null;
        }
        if (!digits.matches("7\\d{10}")) return null;
        return "+" + digits;
    }

}