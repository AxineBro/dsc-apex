package com.hackathon.agent.infrastructure.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.agent.api.dto.response.ChatAttachment;
import com.hackathon.agent.domain.model.Apartment;
import com.hackathon.agent.domain.model.Session;
import com.hackathon.agent.domain.service.ApartmentSearchService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Каталог жилых комплексов (ЖК), доступных для прикрепления к ответам бота.
 * <p>
 * Загружает справочник ЖК из JSON-файла на этапе инициализации Bean и предоставляет
 * метод {@link #attachmentsFor(Session)} для формирования списка вложений
 * (изображений ЖК) на основе ранжированного списка квартир текущей сессии.
 * Используется оркестратором {@code AgentOrchestrator} при сборке
 * {@code ProcessResult}, который затем возвращается клиенту через
 * {@code ChatController}.
 * </p>
 *
 * <p><b>Основные обязанности:</b></p>
 * <ul>
 *     <li>Загрузка справочника ЖК из classpath-ресурса, путь к которому задаётся
 *         свойством {@code app.data.complexes}.</li>
 *     <li>Нормализация названий ЖК для устойчивого поиска (регистр, кавычки,
 *         лишние пробелы).</li>
 *     <li>Формирование списка вложений {@link ChatAttachment} для сессии
 *         (не более 3 уникальных ЖК из первых 10 квартир ранжированного списка).</li>
 *     <li>Устойчивость к ошибкам: при сбое загрузки или обработки каталог
 *         считается пустым, приложение продолжает работать.</li>
 * </ul>
 *
 * <p><b>Формат файла справочника (JSON-массив):</b></p>
 * <pre>
 * [
 *   { "name": "Северный парк", "imageUrl": "{@code https://.../north.jpg}", "district": "САО" },
 *   { "name": "Западный луч",  "imageUrl": "{@code https://.../west.jpg}",  "district": "ЗАО" }
 * ]
 * </pre>
 *
 * <p><b>Зависимости:</b></p>
 * <ul>
 *     <li>{@link ApartmentSearchService} — поиск квартиры по ID для определения ЖК.</li>
 *     <li>{@link ObjectMapper} — де сериализация справочника из JSON.</li>
 * </ul>
 *
 * @author Axine
 * @since 1.0.0
 * @see ChatAttachment
 * @see ApartmentSearchService
 * @see Session
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ComplexCatalog {

    /**
     * DTO-представление жилого комплекса.
     * <p>
     * Используется как элемент справочника, загружаемого из JSON, и как источник
     * данных для формирования вложений {@link ChatAttachment}.
     * </p>
     *
     * @param name     название ЖК (используется как ключ поиска после нормализации).
     * @param imageUrl URL изображения-презентации ЖК для прикрепления к ответу бота.
     * @param district название района (например, {@code "САО"}, {@code "ЗАО"}).
     *                 Может быть {@code null}, если не указано в справочнике.
     */
    public record Complex(String name, String imageUrl, String district) {}

    private final ApartmentSearchService searchService;
    private final ObjectMapper objectMapper;

    /**
     * Путь к JSON-файлу со справочником ЖК внутри classpath.
     * <p>
     * Значение задаётся свойством {@code app.data.complexes} в конфигурации
     * (application.yml/properties). Пример: {@code data/complexes.json}.
     * </p>
     */
    @Value("${app.data.complexes}")
    private String complexesPath;

    /**
     * Индекс справочника ЖК по нормализованному названию.
     * <p>
     * Ключ — результат {@link #norm(String)} от названия ЖК, значение — объект
     * {@link Complex}. Инициализируется в {@link #load()} и далее используется
     * только для чтения.
     * </p>
     * <p>
     * Если загрузка справочника не удалась, остаётся пустой картой, и метод
     * {@link #attachmentsFor(Session)} возвращает пустой список.
     * </p>
     */
    private Map<String, Complex> byName = Map.of();

    /**
     * Загружает справочник ЖК из classpath-ресурса.
     * <p>
     * Вызывается один раз после создания Bean (аннотация {@link PostConstruct}).
     * Читает JSON-массив объектов {@link Complex}, строит индекс по нормализованному
     * названию и сохраняет его в {@link #byName}.
     * </p>
     *
     * <p><b>Поведение при ошибке:</b></p>
     * Любое исключение (отсутствие файла, некорректный JSON, ошибки ввода-вывода)
     * логируется на уровне ERROR, после чего {@link #byName} устанавливается
     * в пустую карту. Приложение продолжает работу без вложений ЖК.
     * </p>
     */
    @PostConstruct
    public void load() {
        try (var in = new ClassPathResource(complexesPath).getInputStream()) {
            List<Complex> list = objectMapper.readValue(in, new TypeReference<>() {});
            Map<String, Complex> m = new HashMap<>();
            for (var c : list) m.put(norm(c.name()), c);
            byName = Map.copyOf(m);
            log.info("Loaded {} complexes from {}", byName.size(), complexesPath);
        } catch (Exception e) {
            log.error("Failed to load complexes from {}: {}", complexesPath, e.getMessage(), e);
            byName = Map.of();
        }
    }

    /**
     * Формирует список вложений {@link ChatAttachment} с изображениями ЖК
     * для указанной сессии.
     * <p>
     * Вложения строятся на основе ранжированного списка квартир
     * ({@link Session#getRankedList()}):
     * <ol>
     *     <li>Берутся первые 10 ID из ранжированного списка.</li>
     *     <li>Для каждого ID через {@link ApartmentSearchService#getById(Long)}
     *         получается квартира; если она не найдена или у неё не указан ЖК — пропускается.</li>
     *     <li>По нормализованному названию ЖК ищется запись в справочнике
     *         ({@link #byName}); если не найдена — пропускается.</li>
     *     <li>Уникальные ЖК добавляются в порядке первого появления,
     *         максимум 3 штуки.</li>
     * </ol>
     * </p>
     *
     * <p><b>Формируемые вложения:</b></p>
     * Для каждого найденного ЖК создаётся {@link ChatAttachment} с типом
     * {@code "complex_image"}, URL изображения и названием ЖК в качестве подписи.
     * </p>
     *
     * <p><b>Поведение при ошибке:</b></p>
     * Если сессия {@code null}, ранжированный список пуст или при обработке
     * возникает исключение, возвращается пустой список (без проброса исключения).
     * </p>
     *
     * @param session текущая сессия; может быть {@code null}
     * @return список вложений (не более 3); пустой список, если ничего не найдено
     *         или произошла ошибка; никогда не {@code null}
     * @see ChatAttachment
     * @see ApartmentSearchService#getById(Long)
     */
    public List<ChatAttachment> attachmentsFor(Session session) {
        try {
            if (session == null || session.getRankedList() == null || session.getRankedList().isEmpty())
                return List.of();
            LinkedHashMap<String, Complex> uniq = new LinkedHashMap<>();
            for (Long id : session.getRankedList().stream().limit(10).toList()) {
                try{
                    Apartment a = searchService.getById(id);

                    if (a == null || a.getComplexName() == null) continue;
                    Complex c = byName.get(norm(a.getComplexName()));
                    if (c != null) uniq.putIfAbsent(c.name(), c);
                    if (uniq.size() >= 3) break;
                } catch (Exception e){
                    log.warn("Failed to load apartment id {} from rankedList, skipping: {}", id, e.getMessage());
                }
            }
            return uniq.values().stream()
                    .map(c -> new ChatAttachment("complex_image", c.imageUrl(), c.name()))
                    .toList();
        } catch (Exception e) {
            log.warn("attachmentsFor failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Нормализует название ЖК для использования в качестве ключа поиска.
     * <p>
     * Правила нормализации:
     * <ul>
     *     <li>{@code null} приводится к пустой строке.</li>
     *     <li>Удаляются кавычки {@code «»} и {@code "}.</li>
     *     <li>Обрезаются пробелы по краям ({@link String#trim()}).</li>
     *     <li>Строка приводится к нижнему регистру с использованием
     *         {@link Locale#ROOT}.</li>
     * </ul>
     * Это позволяет сопоставлять названия вида {@code «Северный парк»},
     * {@code "Северный парк"} и {@code северный парк }.
     * </p>
     *
     * <p><b>Пример:</b></p>
     * <pre>
     * norm("«Северный парк»")  // "северный парк"
     * norm(null)               // ""
     * </pre>
     *
     * @param s исходная строка названия ЖК; может быть {@code null}
     * @return нормализованное название; пустая строка, если вход {@code null}
     */
    private static String norm(String s) {
        return s == null ? "" : s
                .replaceAll("[«»\"]", "")
                .replace('ё','е')
                .replace('Ё','Е')
                .trim().toLowerCase(Locale.ROOT);
    }
}