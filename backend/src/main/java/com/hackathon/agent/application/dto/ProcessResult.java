package com.hackathon.agent.application.dto;

import com.hackathon.agent.api.controller.ChatController;
import com.hackathon.agent.api.dto.response.ChatAttachment;
import com.hackathon.agent.domain.model.SessionState;

import java.util.List;

/**
 * DTO (Data Transfer Object), представляющий результат обработки сообщения пользователя
 * в чат-сессии.
 * <p>
 * Возвращается фасадом {@code ChatFacade#processMessageRich} и используется в
 * {@link ChatController#sendMessage} для формирования ответа клиенту. Содержит
 * текст ответа бота, текущее состояние сессии, признак перевода диалога на
 * менеджера и, при необходимости, причину перевода и список вложений.
 * </p>
 *
 * <p><b>Пример JSON-ответа:</b></p>
 * <pre>
 * {
 *     "reply": "Привет! У меня всё отлично, спасибо.",
 *     "state": "ACTIVE",
 *     "transferredToManager": false,
 *     "managerReason": null,
 *     "attachments": []
 * }
 * </pre>
 *
 * <p><b>Особенности:</b></p>
 * <ul>
 *     <li>Поле {@code reply} может быть {@code null}, если бот не сформировал текстовый ответ
 *         (например, при переводе на менеджера без прощальной фразы).</li>
 *     <li>Поле {@code state} может быть {@code null}, если состояние сессии не определено
 *         (например, при ошибке обработки).</li>
 *     <li>Поле {@code managerReason} заполняется только при
 *         {@code transferredToManager == true}.</li>
 *     <li>Поле {@code attachments} никогда не возвращает {@code null} — при отсутствии
 *         вложений возвращается пустой список.</li>
 * </ul>
 *
 * @param reply                текстовый ответ бота. Может быть {@code null}.
 * @param state                текущее состояние сессии после обработки сообщения.
 *                             Может быть {@code null}.
 * @param transferredToManager признак того, что диалог переведён на менеджера.
 *                             {@code true} — диалог передан оператору,
 *                             {@code false} — бот продолжает обработку.
 * @param managerReason        причина перевода диалога на менеджера.
 *                             Заполняется только при {@code transferredToManager == true},
 *                             в остальных случаях может быть {@code null}.
 * @param attachments          список вложений, прикреплённых к ответу бота.
 *                             Не может быть {@code null}; при отсутствии вложений
 *                             содержит пустой список.
 *
 * @author Axine
 * @since 1.0.0
 * @see ChatController#sendMessage
 * @see SessionState
 * @see ChatAttachment
 */
public record ProcessResult(
        String reply,
        SessionState state,
        boolean transferredToManager,
        String managerReason,
        List<ChatAttachment> attachments
) {}
