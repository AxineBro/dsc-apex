package com.hackathon.agent.api.controller;

import com.hackathon.agent.domain.model.Offer;
import com.hackathon.agent.domain.service.OfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * REST-контроллер для выдачи PDF-файла коммерческого предложения (КП).
 * <p>
 * Предоставляет два независимых способа получить PDF-документ
 * сформированного коммерческого предложения:
 * <ul>
 *     <li>по идентификатору оффера — {@code GET /api/v1/offers/{offerId}/pdf};</li>
 *     <li>по публичному ключу сессии ({@code X-Session-Id}) —
 *         {@code GET /api/v1/chat/sessions/{sessionKey}/offer/pdf}.</li>
 * </ul>
 * Оба эндпоинта возвращают PDF-файл в виде вложения
 * ({@code Content-Disposition: attachment}), готового к скачиванию клиентом.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Выдача PDF-файла КП фронтенду по запросу пользователя.</li>
 *     <li>Обеспечение двух сценариев доступа: когда известен ID оффера и
 *         когда известен только публичный ключ сессии.</li>
 *     <li>Логирование запросов через MDC-контекст (correlationId, sessionId).</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * // Скачивание по ID оффера
 * GET /api/v1/offers/550e8400-e29b-41d4-a716-446655440000/pdf
 *
 * // Скачивание последнего КП по ключу сессии
 * GET /api/v1/chat/sessions/550e8400-e29b-41d4-a716-446655440000/offer/pdf
 * </pre>
 *
 * <p><b>Зависимости:</b></p>
 * <ul>
 *     <li>{@link OfferService} — сервис управления коммерческими предложениями,
 *         предоставляющий методы поиска офферов по ID и ключу сессии.</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see OfferService
 * @see Offer
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class OfferController {

    private final OfferService offerService;

    /**
     * Отдаёт PDF-файл коммерческого предложения по его уникальному идентификатору.
     * <p>
     * Эндпоинт {@code GET /api/v1/offers/{offerId}/pdf} используется, когда
     * клиенту известен ID конкретного оффера (например, он был получен ранее
     * через API чата или сохранён на стороне фронтенда).
     * </p>
     *
     * <p><b>Устанавливаемые заголовки ответа:</b></p>
     * <ul>
     *     <li>{@code Content-Type: application/pdf}</li>
     *     <li>{@code Content-Disposition: attachment; filename="offer-<offerId>.pdf"}</li>
     *     <li>{@code Content-Length: <размер PDF в байтах>}</li>
     * </ul>
     *
     * <p><b>Обработка ошибок:</b></p>
     * <ul>
     *     <li>{@code 404 Not Found} — если оффер с указанным ID не найден
     *         (исключение {@link com.hackathon.agent.domain.exception.DomainException}
     *         из {@link OfferService#getById(UUID)}, преобразованное глобальным
     *         обработчиком).</li>
     *     <li>{@code 409 Conflict} — если оффер найден, но PDF ещё не сгенерирован
     *         (см. {@link #buildPdfResponse(Offer, String)}).</li>
     * </ul>
     *
     * <p><b>Логирование:</b></p>
     * Для каждого запроса генерируется уникальный {@code correlationId},
     * помещаемый в MDC. По завершении обработки (в блоке {@code finally})
     * MDC-контекст очищается.
     * </p>
     *
     * <p><b>Пример запроса:</b></p>
     * <pre>
     * GET /api/v1/offers/550e8400-e29b-41d4-a716-446655440000/pdf
     * </pre>
     *
     * @param offerId уникальный идентификатор оффера (UUID), передаваемый
     *                в пути запроса; не должен быть {@code null}
     * @return {@link ResponseEntity} с PDF-файлом в виде {@link Resource}
     *         и статусом {@code 200 OK}
     * @see OfferService#getById(UUID)
     * @see #buildPdfResponse(Offer, String)
     */
    @GetMapping("/api/v1/offers/{offerId}/pdf")
    public ResponseEntity<Resource> downloadByOfferId(@PathVariable UUID offerId) {
        MDC.put("correlationId", UUID.randomUUID().toString());
        try {
            Offer offer = offerService.getById(offerId);
            return buildPdfResponse(offer, "offer-" + offerId + ".pdf");
        } finally {
            MDC.clear();
        }
    }

    /**
     * Отдаёт PDF-файл последнего коммерческого предложения для указанной сессии.
     * <p>
     * Эндпоинт {@code GET /api/v1/chat/sessions/{sessionKey}/offer/pdf}
     * используется фронтендом, когда известен только публичный ключ сессии
     * ({@code X-Session-Id}), а внутренний ID оффера неизвестен. Возвращается
     * последнее по времени создания КП, связанное с данной сессией.
     * </p>
     *
     * <p><b>Устанавливаемые заголовки ответа:</b></p>
     * Аналогичны {@link #downloadByOfferId(UUID)}, но имя файла формируется
     * по внутреннему ID найденного оффера:
     * {@code offer-<offerId>.pdf}.
     * </p>
     *
     * <p><b>Обработка ошибок:</b></p>
     * <ul>
     *     <li>{@code 409 Conflict} — если для указанной сессии ещё не
     *         сформировано ни одного КП
     *         ({@link OfferService#findLatestBySessionKey(String)} вернул
     *         пустой {@link java.util.Optional}).</li>
     *     <li>{@code 409 Conflict} — если найденный оффер не содержит
     *         PDF-данных (см. {@link #buildPdfResponse(Offer, String)}).</li>
     *     <li>{@code 404 Not Found} — если сессия с указанным ключом
     *         не найдена (обрабатывается на уровне
     *         {@link OfferService#findLatestBySessionKey(String)}, который
     *         возвращает пустой {@link java.util.Optional}).</li>
     * </ul>
     *
     * <p><b>Логирование:</b></p>
     * В MDC помещаются {@code correlationId} и {@code sessionId} (равный
     * {@code sessionKey}) для сквозного логирования всех операций в рамках
     * обработки запроса. MDC-контекст очищается в блоке {@code finally}.
     * </p>
     *
     * <p><b>Пример запроса:</b></p>
     * <pre>
     * GET /api/v1/chat/sessions/550e8400-e29b-41d4-a716-446655440000/offer/pdf
     * </pre>
     *
     * @param sessionKey публичный ключ сессии ({@code X-Session-Id}),
     *                   передаваемый в пути запроса; не должен быть
     *                   {@code null} или пустым
     * @return {@link ResponseEntity} с PDF-файлом в виде {@link Resource}
     *         и статусом {@code 200 OK}
     * @throws ResponseStatusException со статусом {@code 409 Conflict},
     *         если для сессии ещё не сформировано КП
     * @see OfferService#findLatestBySessionKey(String)
     * @see #buildPdfResponse(Offer, String)
     */
    @GetMapping("/api/v1/chat/sessions/{sessionKey}/offer/pdf")
    public ResponseEntity<Resource> downloadBySessionKey(@PathVariable String sessionKey) {
        MDC.put("correlationId", UUID.randomUUID().toString());
        MDC.put("sessionId", sessionKey);
        try {
            Offer offer = offerService.findLatestBySessionKey(sessionKey)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Коммерческое предложение для сессии ещё не сформировано"));
            return buildPdfResponse(offer, "offer-" + offer.getId() + ".pdf");
        } finally {
            MDC.clear();
        }
    }

    /**
     * Формирует HTTP-ответ с PDF-файлом коммерческого предложения.
     * <p>
     * Вспомогательный метод, используемый обоими эндпоинтами контроллера.
     * Выполняет проверку наличия PDF-данных в оффере и, если они присутствуют,
     * собирает ответ с корректными заголовками:
     * <ul>
     *     <li>{@code Content-Type: application/pdf}</li>
     *     <li>{@code Content-Disposition: attachment; filename="<filename>"}</li>
     *     <li>{@code Content-Length: <размер PDF>}</li>
     * </ul>
     * Тело ответа формируется как {@link ByteArrayResource} на основе
     * байтового массива {@link Offer#getPdfData()}.
     * </p>
     *
     * <p><b>Проверка наличия PDF:</b></p>
     * Если {@code offer.getPdfData()} равен {@code null} или имеет нулевую
     * длину, выбрасывается {@link ResponseStatusException} со статусом
     * {@code 409 Conflict} и сообщением «PDF ещё не сгенерирован».
     * Это может произойти, если оффер создан, но генерация PDF по какой-то
     * причине не завершилась успешно.
     * </p>
     *
     * <p><b>Пример использования:</b></p>
     * <pre>
     * Offer offer = offerService.getById(offerId);
     * return buildPdfResponse(offer, "offer-" + offerId + ".pdf");
     * </pre>
     *
     * @param offer    объект коммерческого предложения (не {@code null});
     *                 должен содержать PDF-данные
     * @param filename имя файла, указываемое в заголовке
     *                 {@code Content-Disposition} (например,
     *                 {@code "offer-550e8400-....pdf"}); рекомендуется
     *                 использовать расширение {@code .pdf}
     * @return {@link ResponseEntity} с PDF-файлом в качестве тела ответа
     *         и статусом {@code 200 OK}
     * @throws ResponseStatusException со статусом {@code 409 Conflict},
     *         если PDF-данные отсутствуют в оффере
     * @see Offer#getPdfData()
     * @see ByteArrayResource
     */
    private ResponseEntity<Resource> buildPdfResponse(Offer offer, String filename) {
        if (offer.getPdfData() == null || offer.getPdfData().length == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PDF ещё не сгенерирован");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentLength(offer.getPdfData().length)
                .body(new ByteArrayResource(offer.getPdfData()));
    }
}