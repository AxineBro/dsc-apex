package com.hackathon.agent.domain.exception;

/**
 * Исключение, выбрасываемое при попытке бронирования квартиры, которая уже была
 * забронирована другим клиентом в процессе конкурентного доступа.
 * <p>
 * Является частным случаем {@link DomainException} и используется в бизнес-логике
 * для сигнализации о состоянии гонки (race condition) при бронировании.
 * </p>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Инкапсуляция ошибки конкурентного бронирования на уровне домена.</li>
 *     <li>Предотвращение двойного бронирования одной квартиры разными пользователями.</li>
 *     <li>Обеспечение обратной связи клиенту с понятным сообщением о том, что
 *         квартира была забронирована другим пользователем.</li>
 * </ul>
 *
 * <p><b>Типичный сценарий использования:</b></p>
 * <pre>
 * // В сервисе бронирования
 * public Booking bookApartment(Long apartmentId, User user) {
 *     Apartment apartment = apartmentRepository.findByIdWithLock(apartmentId);
 *     if (apartment.isBooked()) {
 *         throw new BookingRaceException(apartmentId);
 *     }
 *     // Бронирование ...
 * }
 * </pre>
 *
 * <p><b>Обработка на уровне контроллера:</b></p>
 * Рекомендуется перехватывать данное исключение в глобальном обработчике
 * (например, {@code @ControllerAdvice}) и возвращать клиенту HTTP-статус
 * {@code 409 Conflict} с телом ответа, содержащим сообщение об ошибке.
 * Это информирует клиента о необходимости повторить попытку или выбрать другую квартиру.
 *
 * @author Axine
 * @since 1.0.0
 * @see DomainException
 * @see org.springframework.web.bind.annotation.ControllerAdvice
 */
public class BookingRaceException extends DomainException {

    /**
     * Создаёт исключение для случая, когда квартира была забронирована другим клиентом.
     * <p>
     * Формирует сообщение вида:
     * {@code "Квартира с ID <apartmentId> была забронирована другим клиентом"},
     * где {@code apartmentId} — идентификатор квартиры, которую пытались забронировать.
     * </p>
     *
     * @param apartmentId идентификатор квартиры, которая стала причиной конфликта
     */
    public BookingRaceException(Long apartmentId) {
        super("Квартира с ID " + apartmentId + " была забронирована другим клиентом");
    }
}