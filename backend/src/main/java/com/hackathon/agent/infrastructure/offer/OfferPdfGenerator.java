package com.hackathon.agent.infrastructure.offer;

import com.hackathon.agent.domain.exception.DomainException;
import com.hackathon.agent.domain.model.Apartment;
import com.hackathon.agent.domain.model.Session;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Генератор PDF-файла коммерческого предложения (КП).
 * <p>
 * Отвечает за рендеринг PDF-документа на основе HTML-шаблона и данных
 * выбранной квартиры и текущей диалоговой сессии. Использует библиотеку
 * <a href="https://github.com/danfickle/openhtmltopdf">openhtmltopdf</a>
 * для преобразования HTML/CSS в PDF.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Загрузка HTML-шаблона коммерческого предложения из classpath
 *         при инициализации компонента.</li>
 *     <li>Подстановка данных квартиры и сессии в плейсхолдеры шаблона.</li>
 *     <li>Рендеринг итогового HTML в PDF-документ с поддержкой кириллицы.</li>
 *     <li>Возврат PDF в виде байтового массива для последующего сохранения
 *         в БД или отдачи клиенту.</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * OfferPdfGenerator generator = ...;
 * byte[] pdf = generator.generatePdf(apartment, session);
 * // PDF готов к сохранению или передаче
 * </pre>
 *
 * <p><b>Шаблон HTML:</b></p>
 * Шаблон представляет собой обычный HTML-файл с плейсхолдерами вида
 * {@code {placeholder}}. Поддерживаются следующие плейсхолдеры:
 * <ul>
 *     <li>{@code {complexName}} — название ЖК.</li>
 *     <li>{@code {address}}, {@code {district}}, {@code {housePosition}},
 *         {@code {flatNumber}} — адресные данные.</li>
 *     <li>{@code {floor}}, {@code {totalFloors}} — этаж и этажность.</li>
 *     <li>{@code {area}}, {@code {livingArea}} — площадь общая и жилая.</li>
 *     <li>{@code {rooms}} — количество комнат.</li>
 *     <li>{@code {price}}, {@code {pricePerM2}} — цена и цена за м²
 *         (форматируется с разделителями и знаком рубля).</li>
 *     <li>{@code {viewType}} — вид из окон (локализованное значение).</li>
 *     <li>{@code {parking}} — наличие парковки ({@code "есть"} / {@code "нет"}).</li>
 *     <li>{@code {deadline}} — срок сдачи.</li>
 *     <li>{@code {planUrl}}, {@code {cardUrl}} — ссылки на планировку и карточку.</li>
 *     <li>{@code {clientPhone}}, {@code {sessionKey}} — данные сессии.</li>
 *     <li>{@code {generatedAt}} — дата формирования документа.</li>
 *     <li>{@code {fontFamily}} — имя семейства шрифта для CSS.</li>
 * </ul>
 *
 * <p><b>Шрифты:</b></p>
 * Для корректного отображения кириллицы необходимо положить TTF-файл
 * (например, {@code DejaVuSans.ttf}) в {@code src/main/resources/fonts/}.
 * Имя шрифта, указанное в CSS-шаблоне ({@code font-family}),
 * должно совпадать с именем, под которым шрифт регистрируется в этом классе
 * (см. {@link #fontFamily}).
 *
 * <p><b>Конфигурация:</b></p>
 * Параметры генератора задаются через свойства приложения
 * ({@code application.yml} / {@code application.properties}):
 * <ul>
 *     <li>{@code app.offer.pdf-template-path} — путь к HTML-шаблону
 *         (по умолчанию {@code templates/offer_template.html}).</li>
 *     <li>{@code app.offer.pdf-font-path} — путь к TTF-шрифту
 *         (по умолчанию {@code fonts/DejaVuSans.ttf}).</li>
 *     <li>{@code app.offer.pdf-font-family} — имя шрифта для CSS
 *         (по умолчанию {@code DejaVuSans}).</li>
 * </ul>
 *
 * <p><b>Обработка ошибок:</b></p>
 * <ul>
 *     <li>При ошибке загрузки шаблона при старте приложения выбрасывается
 *         {@link DomainException}, что останавливает инициализацию компонента.
 *         Это сделано намеренно, так как без шаблона генерация КП невозможна.</li>
 *     <li>При ошибке рендеринга PDF (некорректный HTML, проблемы со шрифтом
 *         и т.п.) выбрасывается {@link DomainException} с сохранением исходной
 *         причины.</li>
 * </ul>
 *
 * <p><b>Связь с другими компонентами:</b></p>
 * <ul>
 *     <li>Вызывается из
 *         {@link com.hackathon.agent.infrastructure.offer.OfferServiceImpl#createOffer}
 *         в процессе формирования коммерческого предложения.</li>
 *     <li>Использует доменные модели {@link Apartment} и {@link Session}
 *         в качестве источников данных для подстановки в шаблон.</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see com.hackathon.agent.infrastructure.offer.OfferServiceImpl
 * @see Apartment
 * @see Session
 * @see PdfRendererBuilder
 */
@Component
@Slf4j
public class OfferPdfGenerator {

    /**
     * Путь к HTML-шаблону КП в classpath.
     * <p>
     * Значение задаётся через свойство {@code app.offer.pdf-template-path}.
     * Если свойство не задано, используется значение по умолчанию
     * {@code templates/offer_template.html}.
     * </p>
     * <p>
     * Файл должен находиться в classpath (например, в
     * {@code src/main/resources/templates/}).
     * </p>
     */
    @Value("${app.offer.pdf-template-path:templates/offer_template.html}")
    private String templatePath;

    /**
     * Путь к TTF-шрифту с поддержкой кириллицы в classpath.
     * <p>
     * Значение задаётся через свойство {@code app.offer.pdf-font-path}.
     * Если свойство не задано, используется значение по умолчанию
     * {@code fonts/DejaVuSans.ttf}.
     * </p>
     * <p>
     * Файл должен находиться в classpath (например, в
     * {@code src/main/resources/fonts/}).
     * </p>
     */
    @Value("${app.offer.pdf-font-path:fonts/DejaVuSans.ttf}")
    private String fontPath;

    /**
     * Имя семейства шрифта, используемое в CSS-шаблоне.
     * <p>
     * Значение задаётся через свойство {@code app.offer.pdf-font-family}.
     * Если свойство не задано, используется значение по умолчанию
     * {@code DejaVuSans}.
     * </p>
     * <p>
     * Должно совпадать со значением {@code font-family}, указанным в CSS
     * HTML-шаблона. Именно под этим именем шрифт регистрируется в
     * {@link PdfRendererBuilder#useFont}.
     * </p>
     */
    @Value("${app.offer.pdf-font-family:DejaVuSans}")
    private String fontFamily;

    private String template;

    /**
     * Загружает HTML-шаблон КП из classpath при инициализации компонента.
     * <p>
     * Аннотирована {@link PostConstruct}, поэтому вызывается автоматически
     * после внедрения зависимостей и до первого использования генератора.
     * </p>
     *
     * <p><b>Алгоритм:</b></p>
     * <ol>
     *     <li>Создаёт ресурс {@link ClassPathResource} по пути
     *         {@link #templatePath}.</li>
     *     <li>Читает содержимое файла в строку с кодировкой UTF-8.</li>
     *     <li>Сохраняет строку в поле {@link #template}.</li>
     *     <li>Логирует успешную загрузку с указанием пути и длины шаблона.</li>
     *     <li>В случае ошибки ввода-вывода логирует ошибку и выбрасывает
     *         {@link DomainException}, останавливая запуск приложения.</li>
     * </ol>
     *
     * <p><b>Почему исключение, а не дефолтный шаблон:</b></p>
     * В отличие от некоторых других компонентов, отсутствие HTML-шаблона
     * делает генерацию КП бессмысленной. Поэтому вместо «тихой» деградации
     * с использованием заглушки здесь выбрасывается исключение, чтобы
     * проблема была обнаружена на этапе запуска.
     * </p>
     *
     * @throws DomainException если не удалось загрузить HTML-шаблон
     *         (файл не найден, ошибка чтения)
     */
    @PostConstruct
    public void loadTemplate() {
        try {
            var resource = new ClassPathResource(templatePath);
            template = resource.getContentAsString(StandardCharsets.UTF_8);
            log.info("HTML-шаблон КП загружен: {}, длина {} символов", templatePath, template.length());
        } catch (IOException e) {
            log.error("Не удалось загрузить HTML-шаблон КП из {}: {}", templatePath, e.getMessage(), e);
            throw new DomainException("Не удалось загрузить HTML-шаблон КП", e);
        }
    }

    /**
     * Генерирует PDF-файл КП для указанной квартиры и сессии.
     * <p>
     * Основной метод генератора. Выполняет последовательно:
     * <ol>
     *     <li>Подстановку данных квартиры и сессии в HTML-шаблон
     *         ({@link #buildHtml(Apartment, Session)}).</li>
     *     <li>Рендеринг полученного HTML в PDF с помощью
     *         {@link PdfRendererBuilder} в режиме {@code useFastMode}.</li>
     *     <li>Регистрацию TTF-шрифта для корректного отображения кириллицы
     *         (через {@link #openFontStream()}).</li>
     *     <li>Запись результата в {@link ByteArrayOutputStream} и получение
     *         байтового массива.</li>
     * </ol>
     * </p>
     *
     * <p><b>Производительность:</b></p>
     * Операция рендеринга PDF является ресурсоёмкой (может занимать
     * несколько сотен миллисекунд или секунд в зависимости от размера
     * шаблона и встраиваемых ресурсов). Рекомендуется вызывать вне
     * транзакции БД, чтобы не удерживать соединение с базой данных.
     * Длительность операции логируется на уровне INFO.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * byte[] pdf = generator.generatePdf(apartment, session);
     * log.info("PDF готов, размер: {} байт", pdf.length);
     * </pre>
     *
     * @param apartment квартира, данные которой подставляются в шаблон
     *                  (не {@code null})
     * @param session   диалоговая сессия (не {@code null}); используется
     *                  для подстановки контактных данных и ключа сессии
     * @return байтовый массив с содержимым PDF-файла; никогда не {@code null},
     *         имеет ненулевую длину
     * @throws DomainException если любой из аргументов равен {@code null},
     *         либо если рендеринг PDF завершился ошибкой
     * @see #buildHtml(Apartment, Session)
     * @see #openFontStream()
     */
    public byte[] generatePdf(Apartment apartment, Session session) {
        if (apartment == null) {
            throw new DomainException("Apartment cannot be null for PDF generation");
        }
        if (session == null) {
            throw new DomainException("Session cannot be null for PDF generation");
        }

        long start = System.currentTimeMillis();
        try {
            String html = buildHtml(apartment, session);

            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(html, null);

                builder.useFont(
                        () -> openFontStream(),
                        fontFamily
                );

                builder.toStream(os);
                builder.run();

                byte[] pdf = os.toByteArray();
                log.info("PDF сгенерирован для квартиры id={}, размер {} байт, duration={}ms",
                        apartment.getId(), pdf.length, System.currentTimeMillis() - start);
                return pdf;
            }
        } catch (Exception e) {
            log.error("Ошибка генерации PDF для квартиры id={}: {}", apartment.getId(), e.getMessage(), e);
            throw new DomainException("Не удалось сгенерировать PDF", e);
        }
    }

    /**
     * Открывает входной поток для TTF-шрифта из classpath.
     * <p>
     * Вспомогательный метод, используемый при регистрации шрифта в
     * {@link PdfRendererBuilder#useFont}. Возвращает новый поток при каждом
     * вызове, что соответствует требованиям библиотеки openhtmltopdf.
     * </p>
     *
     * @return {@link InputStream} с содержимым TTF-файла шрифта
     * @throws DomainException если файл шрифта не найден или не может быть
     *         прочитан (например, из-за ошибок доступа или отсутствия в classpath)
     */
    private InputStream openFontStream() {
        try {
            return new ClassPathResource(fontPath).getInputStream();
        } catch (IOException e) {
            throw new DomainException("Не удалось загрузить шрифт " + fontPath, e);
        }
    }

    /**
     * Формирует итоговый HTML-код коммерческого предложения, подставляя
     * данные квартиры и сессии в плейсхолдеры шаблона.
     * <p>
     * Выполняет последовательную замену всех известных плейсхолдеров
     * (см. описание класса) на реальные значения. Для {@code null}-значений
     * подставляется прочерк {@code "—"} (см. {@link #nullSafe(Object)}).
     * Цены форматируются с разделителями разрядов и знаком рубля
     * (см. {@link #formatPrice(java.math.BigDecimal)}). Вид из окон
     * переводится на русский язык (см. {@link #viewTypeRu(String)}).
     * </p>
     *
     * @param apartment квартира — источник данных для большинства
     *                  плейсхолдеров (не {@code null})
     * @param session   сессия — источник данных для {@code {clientPhone}},
     *                  {@code {sessionKey}} (не {@code null})
     * @return готовый HTML-код с подставленными значениями
     * @see #nullSafe(Object)
     * @see #formatPrice(java.math.BigDecimal)
     * @see #viewTypeRu(String)
     */
    private String buildHtml(Apartment apartment, Session session) {
        return template
                .replace("{complexName}", nullSafe(apartment.getComplexName()))
                .replace("{address}", nullSafe(apartment.getAddress()))
                .replace("{district}", nullSafe(apartment.getDistrict()))
                .replace("{housePosition}", nullSafe(apartment.getHousePosition()))
                .replace("{flatNumber}", nullSafe(apartment.getFlatNumber()))
                .replace("{floor}", nullSafe(apartment.getFloor()))
                .replace("{totalFloors}", nullSafe(apartment.getTotalFloors()))
                .replace("{area}", nullSafe(apartment.getArea()))
                .replace("{livingArea}", nullSafe(apartment.getLivingArea()))
                .replace("{rooms}", nullSafe(apartment.getRooms()))
                .replace("{price}", formatPrice(apartment.getPrice()))
                .replace("{pricePerM2}", formatPrice(apartment.getPricePerM2()))
                .replace("{viewType}", viewTypeRu(apartment.getViewType()))
                .replace("{parking}", Boolean.TRUE.equals(apartment.getParking()) ? "есть" : "нет")
                .replace("{deadline}", nullSafe(apartment.getDeadline()))
                .replace("{planUrl}", nullSafe(apartment.getPlanUrl()))
                .replace("{cardUrl}", nullSafe(apartment.getCardUrl()))
                .replace("{clientPhone}", nullSafe(session.getClientPhone()))
                .replace("{sessionKey}", nullSafe(session.getSessionKey()))
                .replace("{generatedAt}", java.time.LocalDate.now().toString())
                .replace("{fontFamily}", fontFamily);
    }

    /**
     * Безопасно преобразует значение в строку, подставляя прочерк для {@code null}.
     * <p>
     * Используется для заполнения плейсхолдеров шаблона: если значение поля
     * квартиры или сессии отсутствует, в HTML подставляется символ {@code "—"},
     * чтобы итоговый документ не содержал пустых мест или артефактов вида
     * {@code "null"}.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * nullSafe("Москва")  // "Москва"
     * nullSafe(null)      // "—"
     * nullSafe(42)        // "42"
     * </pre>
     *
     * @param v значение любого типа; может быть {@code null}
     * @return строковое представление значения или {@code "—"}, если значение
     *         равно {@code null}
     */
    private static String nullSafe(Object v) {
        return v != null ? v.toString() : "—";
    }

    /**
     * Форматирует цену в человекочитаемый вид с разделителями разрядов
     * и знаком рубля.
     * <p>
     * Использует локаль {@code ru-RU} для правильной расстановки разделителей
     * (пробел между тысячами) и добавляет символ рубля ({@code ₽}) в конце.
     * Для {@code null} возвращает прочерк {@code "—"}.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * formatPrice(new BigDecimal("12500000"))  // "12 500 000 ₽"
     * formatPrice(new BigDecimal("85000"))     // "85 000 ₽"
     * formatPrice(null)                        // "—"
     * </pre>
     *
     * @param price цена в рублях; может быть {@code null}
     * @return отформатированная строка с ценой и знаком рубля, либо
     *         {@code "—"} при {@code null}
     */
    private static String formatPrice(java.math.BigDecimal price) {
        if (price == null) return "—";
        java.text.DecimalFormat df = new java.text.DecimalFormat("#,###");
        df.setDecimalFormatSymbols(new java.text.DecimalFormatSymbols(java.util.Locale.forLanguageTag("ru")));
        return df.format(price) + " \u20BD";
    }

    /**
     * Переводит код типа вида из окон на русский язык для отображения в PDF.
     * <p>
     * Используется для преобразования технических значений поля
     * {@link Apartment#getViewType()} в человекочитаемый текст.
     * Поддерживаемые коды:
     * <ul>
     *     <li>{@code "park"} → {@code "парк"}</li>
     *     <li>{@code "city"} → {@code "город"}</li>
     *     <li>{@code "river"} или {@code "water"} → {@code "вода"}</li>
     *     <li>{@code "yard"} → {@code "двор"}</li>
     * </ul>
     * Для неизвестных значений возвращает исходную строку без изменений,
     * для {@code null} — прочерк {@code "—"}.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * viewTypeRu("park")   // "парк"
     * viewTypeRu("water")  // "вода"
     * viewTypeRu("forest") // "forest" (неизвестный код)
     * viewTypeRu(null)     // "—"
     * </pre>
     *
     * @param v строковый код типа вида из окон; может быть {@code null}
     * @return локализованное название вида или исходное значение, если код
     *         неизвестен; {@code "—"} при {@code null}
     */
    private static String viewTypeRu(String v) {
        if (v == null) return "—";
        return switch (v.toLowerCase()) {
            case "park" -> "парк";
            case "city" -> "город";
            case "river", "water" -> "вода";
            case "yard" -> "двор";
            default -> v;
        };
    }
}