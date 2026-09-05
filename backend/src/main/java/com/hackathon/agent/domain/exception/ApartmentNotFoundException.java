package com.hackathon.agent.domain.exception;

/**
 * Исключение, выбрасываемое при попытке обращения к несуществующей квартире.
 * <p>
 * Является частным случаем {@link DomainException} и используется в бизнес-логике
 * для сигнализации о том, что запрошенная квартира с указанным идентификатором
 * отсутствует в системе.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Инкапсуляция ошибки "квартира не найдена" на уровне домена.</li>
 *     <li>Упрощение обработки ошибок в сервисном слое и контроллерах за счёт
 *         использования иерархии исключений.</li>
 *     <li>Предоставление понятного и локализованного сообщения для клиента
 *         (через глобальный обработчик исключений).</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * // В сервисе поиска квартир
 * Optional&lt;Apartment&gt; apartmentOpt = apartmentRepository.findById(id);
 * if (apartmentOpt.isEmpty()) {
 *     throw new ApartmentNotFoundException(id);
 * }
 * return apartmentOpt.get();
 * </pre>
 *
 * <p><b>Обработка:</b></p>
 * Рекомендуется перехватывать данное исключение в глобальном обработчике
 * (например, {@code @ControllerAdvice}) и возвращать клиенту HTTP-статус
 * {@code 404 Not Found} с соответствующим телом ответа.
 *
 * @author Axine
 * @since 1.0.0
 * @see DomainException
 * @see org.springframework.web.bind.annotation.ControllerAdvice
 */
public class ApartmentNotFoundException extends DomainException {

    /**
     * Создаёт исключение для несуществующей квартиры по её идентификатору.
     * <p>
     * Формирует сообщение вида:
     * {@code "Квартира с ID <id> не найдена"}, где {@code id} — переданный параметр.
     * Это сообщение будет передано клиенту после обработки исключения.
     * </p>
     *
     * @param id идентификатор квартиры, которая не была найдена
     */
    public ApartmentNotFoundException(Long id) {
        super("Квартира с ID " + id + " не найдена");
    }
}