package com.hackathon.agent.domain.model;

import com.hackathon.agent.application.orchestrator.AgentOrchestrator;
import com.hackathon.agent.application.orchestrator.SessionManager;
import com.hackathon.agent.domain.exception.DomainException;
import com.hackathon.agent.domain.exception.UnknownSessionStateException;
import lombok.Getter;

/**
 * Перечисление, определяющее возможные состояния диалоговой сессии.
 * <p>
 * Управляет жизненным циклом сессии и определяет, какие действия доступны
 * в каждый момент времени. Состояние изменяется в процессе диалога на основе
 * действий пользователя, ответов AI-ассистента и внутренней логики.
 * </p>
 *
 * <p><b>Жизненный цикл сессии:</b></p>
 * <ol>
 *     <li>{@link #NEW} — сессия создана, но ещё не было сообщений.</li>
 *     <li>{@link #AWAITING_INPUT} — ожидает ввода от пользователя (базовое состояние).</li>
 *     <li>{@link #TIMEOUT_REMINDER} — пользователю отправлено напоминание (если долго не отвечает).</li>
 *     <li>{@link #SHOWING_LIST} — ассистент показывает список квартир.</li>
 *     <li>{@link #OFFER_READY} — сформировано коммерческое предложение.</li>
 *     <li>{@link #TO_MANAGER} — сессия передана менеджеру (эскалация).</li>
 *     <li>{@link #FINISHED} — диалог завершён.</li>
 * </ol>
 *
 * <p><b>Использование в коде:</b></p>
 * <pre>
 * Session session = sessionManager.getOrCreate(sessionKey);
 * if (session.getStatus() == SessionState.FINISHED) {
 *     // сессия завершена, блокируем обработку
 *     return "Диалог уже завершён.";
 * }
 *
 * // Установка состояния
 * session.setStatus(SessionState.AWAITING_INPUT);
 *
 * // Преобразование в строковый код
 * String code = session.getStatus().getCode();
 *
 * // Восстановление из кода
 * SessionState state = SessionState.fromCode("SHOWING_LIST");
 * </pre>
 *
 * <p><b>Связь с другими классами:</b></p>
 * <ul>
 *     <li>Используется в {@link Session} для хранения текущего статуса.</li>
 *     <li>Проверяется в {@link AgentOrchestrator#process} для определения,
 *         можно ли обрабатывать сообщение.</li>
 *     <li>Управляется через {@link SessionManager}.</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see Session
 * @see AgentOrchestrator
 * @see SessionManager
 */
@Getter
public enum SessionState {

    /**
     * Состояние "новая сессия".
     * <p>
     * Присваивается при создании сессии через {@link SessionManager#getOrCreate}.
     * В этом состоянии сессия ещё не содержит сообщений, и пользователь
     * должен отправить первое сообщение.
     * </p>
     */
    NEW("NEW"),

    /**
     * Состояние "ожидание ввода от пользователя".
     * <p>
     * Базовое состояние активного диалога. Сессия готова принять следующее сообщение
     * от пользователя. Ассистент либо задал уточняющий вопрос, либо ожидает
     * подтверждения/выбора.
     * </p>
     * <p>
     * Является наиболее частым состоянием в процессе диалога.
     * </p>
     */
    AWAITING_INPUT("AWAITING_INPUT"),

    /**
     * Состояние "отправлено напоминание о тайм-ауте".
     * <p>
     * Устанавливается, когда пользователь долго не отвечает (превышен порог бездействия).
     * Система отправляет напоминание и ожидает ответ в течение дополнительного времени
     * (например, 5 минут). Если ответа нет — сессия может быть переведена в {@code FINISHED}
     * или {@code EXPIRED}.
     * </p>
     */
    TIMEOUT_REMINDER("TIMEOUT_REMINDER"),

    /**
     * Состояние "показ списка квартир".
     * <p>
     * Ассистент выводит пользователю ранжированный список квартир (обычно по одной).
     * Пользователь может просматривать варианты, выбирать или запрашивать больше.
     * </p>
     */
    SHOWING_LIST("SHOWING_LIST"),

    /**
     * Состояние "коммерческое предложение готово".
     * <p>
     * Устанавливается, когда ассистент сформировал финальное предложение
     * на основе выбора пользователя. В этом состоянии сессия ожидает
     * подтверждения от пользователя или перехода к менеджеру.
     * </p>
     */
    OFFER_READY("OFFER_READY"),

    /**
     * Состояние "передано менеджеру".
     * <p>
     * Устанавливается, когда ассистент не смог обработать запрос (ошибка, недостаток данных,
     * сложный случай) или пользователь явно запросил консультацию менеджера.
     * В этом состоянии дальнейшая обработка AI прекращается, и задача передаётся
     * в {@link ManagerTask} для обработки специалистом.
     * </p>
     * <p>
     * Сессия считается завершённой (метод {@link Session#isFinished()} вернёт {@code true}).
     * </p>
     */
    TO_MANAGER("TO_MANAGER"),

    /**
     * Состояние "диалог завершён".
     * <p>
     * Финальное состояние, которое означает, что диалог окончен (например,
     * пользователь подтвердил выбор, отказ, или сессия была закрыта системой).
     * В этом состоянии дальнейшая обработка сообщений блокируется.
     * </p>
     */
    FINISHED("FINISHED");

    /**
     * Строковый код состояния, используемый для сериализации и передачи по API.
     * <p>
     * Значения: {@code "NEW"}, {@code "AWAITING_INPUT"}, {@code "TIMEOUT_REMINDER"},
     * {@code "SHOWING_LIST"}, {@code "OFFER_READY"}, {@code "TO_MANAGER"}, {@code "FINISHED"}.
     * </p>
     * -- GETTER --
     *  Возвращает строковый код состояния.
     *

     */
    private final String code;

    /**
     * Конструктор для создания состояния с кодом.
     *
     * @param code строковое представление состояния (не {@code null}, не пусто)
     */
    SessionState(String code) {
        this.code = code;
    }

    /**
     * Преобразует строковый код в соответствующее значение перечисления.
     * <p>
     * Используется для десериализации из JSON или при получении из внешних систем.
     * </p>
     *
     * @param code строковый код состояния (не {@code null})
     * @return соответствующее значение {@link SessionState}
     * @throws IllegalArgumentException если код не соответствует ни одному из состояний
     */
    public static SessionState fromCode(String code) {
        for (SessionState state : values()) {
            if (state.code.equals(code)) {
                return state;
            }
        }
        throw new UnknownSessionStateException("Unknown session state: " + code);
    }

    /**
     * Возвращает строковое представление состояния (его код).
     * <p>
     * Переопределён для удобства логирования и вывода.
     * </p>
     *
     * @return код состояния
     */
    @Override
    public String toString() {
        return code;
    }
}