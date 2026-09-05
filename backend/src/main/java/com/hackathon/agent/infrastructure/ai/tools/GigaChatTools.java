package com.hackathon.agent.infrastructure.ai.tools;

import com.hackathon.agent.application.orchestrator.AgentOrchestrator;
import com.hackathon.agent.application.orchestrator.SessionManager;
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
    @Tool(description = "Поиск квартир по заданным параметрам. Возвращает список доступных квартир.")
    public List<Apartment> searchApartments(
            @ToolParam(description = "Минимальная площадь (кв.м)") BigDecimal areaMin,
            @ToolParam(description = "Максимальная площадь (кв.м)") BigDecimal areaMax,
            @ToolParam(description = "Желаемый этаж (если указан)") Integer floor,
            @ToolParam(description = "Минимальная цена (руб)") BigDecimal priceMin,
            @ToolParam(description = "Максимальная цена (руб)") BigDecimal priceMax,
            @ToolParam(description = "Минимальное количество комнат") Integer roomsMin,
            @ToolParam(description = "Максимальное количество комнат") Integer roomsMax
    ) {
        log.info("Tool searchApartments called: areaMin={}, areaMax={}, floor={}, priceMin={}, priceMax={}, roomsMin={}, roomsMax={}",
                areaMin, areaMax, floor, priceMin, priceMax, roomsMin, roomsMax);

        Session session = sessionManager.getCurrentSession();
        if (session == null) {
            log.warn("Текущая сессия не найдена в ThreadLocal, поиск невозможен");
            return List.of();
        }
        MDC.put("sessionId", session.getSessionKey());

        long startTime = System.currentTimeMillis();
        try {
            Filters filters = new Filters(areaMin, areaMax, floor, priceMin, priceMax, roomsMin, roomsMax);
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
    @Tool(description = "Получить первые квартиры из ранжированного списка (количество задаётся конфигурацией app.flat.limit)")
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
    @Tool(description = "Оценить список квартир по неметрическим параметрам (вид из окна, парковка, престижность этажа). Возвращает список с оценками 0-100.")
    public List<ScoredApartment> rateApartments(
            @ToolParam(description = "Список ID квартир для оценки") List<Long> apartmentIds
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
    @Tool(description = "Показать следующую квартиру из полного списка")
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
     * Создаёт задачу для менеджера через {@link NotificationService},
     * изменяет статус сессии на {@link SessionState#TO_MANAGER}
     * и сохраняет сессию.
     * </p>
     *
     * @param reason причина перевода (например, 'timeout', 'no_data', 'limit_cycles')
     * @param phone  номер телефона клиента (если известен, может быть {@code null})
     * @return строковый статус "ok" (для AI-агента)
     */
    @Tool(description = "Перевести клиента на менеджера с указанием причины.")
    public String transferToManager(
            @ToolParam(description = "Причина перевода (например, 'timeout', 'no_data', 'limit_cycles')") String reason,
            @ToolParam(description = "Телефон клиента (если известен)") String phone
    ) {
        log.info("Tool transferToManager called: reason={}, phone={}", reason, phone);
        Session session = sessionManager.getCurrentSession();
        if (session == null) {
            log.warn("Текущая сессия не найдена для перевода на менеджера");
            return "ok";
        }
        MDC.put("sessionId", session.getSessionKey());

        try {
            notificationService.notifyManager(session, reason, phone);
            session.setStatus(SessionState.TO_MANAGER);
            sessionManager.save(session);
            log.info("Диалог переведён на менеджера. Сессия: {}, причина: {}", session.getSessionKey(), reason);
            return "ok";
        } catch (Exception e) {
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
    @Tool(description = "Сформировать коммерческое предложение для выбранной квартиры.")
    public String generateOffer(
            @ToolParam(description = "ID выбранной квартиры") Long apartmentId
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
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Ошибка при генерации КП для квартиры ID {}: duration={}ms, error={}",
                    apartmentId, duration, e.getMessage(), e);
            return "Произошла ошибка при формировании предложения. Обратитесь к менеджеру.";
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
    @Tool(description = "Задать уточняющий вопрос клиенту, чтобы получить недостающие параметры. Используйте только когда не хватает данных. Максимум 2 вызова.")
    public String askClarification(@ToolParam(description = "Вопрос") String question) {
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
    @Tool(description = "Полностью сбросить все фильтры и начать диалог заново.")
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
}