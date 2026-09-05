package com.hackathon.agent.infrastructure.offer;

import com.hackathon.agent.domain.exception.DomainException;
import com.hackathon.agent.domain.model.Apartment;
import com.hackathon.agent.domain.model.Session;
import com.hackathon.agent.domain.service.OfferGeneratorService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Реализация сервиса генерации коммерческого предложения {@link OfferGeneratorService},
 * использующая текстовый шаблон, загружаемый из внешнего файла.
 * <p>
 * При инициализации компонента (через {@link PostConstruct}) загружает шаблон из classpath
 * по пути, указанному в свойстве {@code app.resource.path}. Затем при вызове
 * {@link #generateOffer} выполняет замену плейсхолдеров (вида {@code {placeholder}})
 * на реальные данные из объекта {@link Apartment}.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Генерация персонализированного коммерческого предложения для клиента
 *         на основе выбранной квартиры.</li>
 *     <li>Отделение содержимого оффера от кода — изменение шаблона без перекомпиляции.</li>
 *     <li>Обеспечение гибкости: можно использовать разные шаблоны для разных сегментов
 *         клиентов или типов недвижимости (путем замены файла).</li>
 * </ul>
 *
 * <p><b>Формат шаблона:</b></p>
 * Шаблон — это обычный текстовый файл (UTF-8), содержащий плейсхолдеры в фигурных скобках.
 * Поддерживаются следующие плейсхолдеры (названия соответствуют полям {@link Apartment}):
 * <ul>
 *     <li>{@code {complexName}} — название ЖК.</li>
 *     <li>{@code {address}} — адрес.</li>
 *     <li>{@code {floor}} — этаж.</li>
 *     <li>{@code {totalFloors}} — общее количество этажей.</li>
 *     <li>{@code {area}} — площадь.</li>
 *     <li>{@code {rooms}} — количество комнат.</li>
 *     <li>{@code {price}} — цена.</li>
 *     <li>{@code {viewType}} — вид из окон.</li>
 *     <li>{@code {parking}} — наличие парковки (текст "есть" или "нет").</li>
 * </ul>
 *
 * <p><b>Конфигурация:</b></p>
 * Свойство {@code app.resource.path} должно указывать путь к файлу шаблона в classpath.
 * <br>Пример в {@code application.yml}:
 * <pre>
 * app:
 *   resource:
 *     path: templates/offer_template.txt
 * </pre>
 *
 * <p><b>Обработка ошибок:</b></p>
 * <ul>
 *     <li>Если файл шаблона не найден или не может быть прочитан, логируется ошибка,
 *         и используется стандартный шаблон-заглушка, чтобы система оставалась работоспособной.</li>
 *     <li>Если какое-либо поле квартиры равно {@code null}, оно заменяется на пустую строку
 *         (кроме {@code parking}, где {@code null} обрабатывается как "нет").</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * OfferGeneratorService offerService = ...;
 * Apartment apartment = searchService.getById(1001L);
 * String offer = offerService.generateOffer(apartment, session);
 * // Отправка клиенту
 * </pre>
 *
 * @author Axine
 * @since 1.0.0
 * @see OfferGeneratorService
 * @see Apartment
 * @see Session
 */
@Service
@Slf4j
public class TemplateOfferGeneratorService implements OfferGeneratorService {

    /**
     * Содержимое загруженного шаблона коммерческого предложения.
     * <p>
     * Устанавливается в методе {@link #loadTemplate()} при инициализации.
     * Содержит плейсхолдеры, которые будут заменены данными квартиры.
     * </p>
     */
    private String template;

    /**
     * Путь к файлу шаблона в classpath.
     * <p>
     * Читается из конфигурационного свойства {@code app.resource.path}.
     * </p>
     * <p>
     * <b>Пример значения:</b> {@code "templates/offer_template.txt"}
     * </p>
     */
    @Value("${app.resource.path}")
    private String pathResource;

    /**
     * Загружает шаблон коммерческого предложения из файла при инициализации компонента.
     * <p>
     * Аннотирована {@link PostConstruct}, вызывается автоматически после внедрения зависимостей.
     * </p>
     *
     * <p><b>Алгоритм:</b></p>
     * <ol>
     *     <li>Создаёт ресурс {@link ClassPathResource} по пути {@link #pathResource}.</li>
     *     <li>Читает содержимое файла в строку с кодировкой UTF-8.</li>
     *     <li>Сохраняет строку в поле {@link #template}.</li>
     *     <li>Логирует успешную загрузку.</li>
     *     <li>В случае ошибки (IOException) логирует ошибку и устанавливает
     *         стандартный шаблон-заглушку, чтобы система могла продолжить работу.</li>
     * </ol>
     *
     * <p><b>Шаблон-заглушка:</b></p>
     * {@code "Квартира: {address}, этаж {floor}, площадь {area}, цена {price}"}
     */
    @PostConstruct
    public void loadTemplate() {
        try {
            var resource = new ClassPathResource(pathResource);
            template = resource.getContentAsString(StandardCharsets.UTF_8);
            log.info("Шаблон КП загружен, длина: {} символов", template.length());
        } catch (IOException e) {
            log.error("Не удалось загрузить шаблон КП из {}: {}", pathResource, e.getMessage(), e);
            template = "Квартира: {address}, этаж {floor}, площадь {area}, цена {price}";
            log.warn("Используется шаблон-заглушка");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Реализация выполняет замену всех плейсхолдеров в загруженном шаблоне на
     * соответствующие значения из объекта {@link Apartment}. Если какое-либо поле
     * квартиры равно {@code null}, оно заменяется на пустую строку (кроме {@code parking},
     * где {@code null} трактуется как "нет").
     * </p>
     *
     * <p><b>Параметры:</b></p>
     * <ul>
     *     <li>{@code apartment} — выбранная квартира (не {@code null}).</li>
     *     <li>{@code session} — сессия клиента (не используется в текущей реализации,
     *         но оставлено для возможности расширения).</li>
     * </ul>
     *
     * <p><b>Возвращаемое значение:</b></p>
     * Строка с заполненным коммерческим предложением. Если шаблон не был загружен
     * (ошибка при старте), используется заглушка.
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * // Вход: квартира с адресом "ул. Ленина, 15", этаж 8, площадь 58.5, цена 12.5 млн.
     * // Шаблон: "Квартира: {address}, этаж {floor}, площадь {area} м², цена {price} руб."
     * // Результат: "Квартира: ул. Ленина, 15, этаж 8, площадь 58.5 м², цена 12500000 руб."
     * </pre>
     *
     * @param apartment выбранная квартира (не {@code null})
     * @param session   сессия клиента (не используется, но может быть задействована в будущем)
     * @return сгенерированное коммерческое предложение (всегда не {@code null})
     * @throws IllegalArgumentException если {@code apartment} равен {@code null}
     */
    @Override
    public String generateOffer(Apartment apartment, Session session) {
        if (apartment == null) {
            log.error("Попытка генерации оффера с null квартирой");
            throw new DomainException("Apartment cannot be null");
        }
        if (session == null) {
            log.error("Попытка генерации оффера с null сессией");
            throw new DomainException("Session cannot be null");
        }

        String sessionKey = session.getSessionKey();
        MDC.put("sessionId", sessionKey);
        long startTime = System.currentTimeMillis();

        log.info("Генерация коммерческого предложения для квартиры id={}, сессия={}",
                apartment.getId(), sessionKey);

        try{
            String offer = template
                    .replace("{complexName}", nullSafe(apartment.getComplexName()))
                    .replace("{address}", nullSafe(apartment.getAddress()))
                    .replace("{floor}", nullSafe(apartment.getFloor()))
                    .replace("{totalFloors}", nullSafe(apartment.getTotalFloors()))
                    .replace("{area}", nullSafe(apartment.getArea()))
                    .replace("{rooms}", nullSafe(apartment.getRooms()))
                    .replace("{price}", nullSafe(apartment.getPrice()))
                    .replace("{viewType}", nullSafe(apartment.getViewType()))
                    .replace("{parking}", apartment.getParking() != null && apartment.getParking() ? "есть" : "нет");

            long duration = System.currentTimeMillis() - startTime;
            log.info("Оффер сгенерирован для квартиры id={}, длина текста={} символов, duration={}ms",
                    apartment.getId(), offer.length(), duration);

            if (log.isDebugEnabled()) {
                log.debug("Содержимое оффера: {}", offer);
            }

            return offer;
        }catch (Exception e) {
            log.error("Ошибка при генерации оффера для квартиры id={}, сессия={}: {}",
                    apartment.getId(), sessionKey, e.getMessage(), e);
            throw new DomainException("Не удалось сгенерировать коммерческое предложение", e);
        }
    }

    private String nullSafe(Object value) {
        return value != null ? value.toString() : "";
    }
}