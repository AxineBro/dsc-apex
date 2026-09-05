package com.hackathon.agent.infrastructure.ai.scoring;

import com.hackathon.agent.application.orchestrator.AgentOrchestrator;
import com.hackathon.agent.domain.model.Apartment;
import com.hackathon.agent.domain.model.ScoredApartment;
import com.hackathon.agent.domain.service.ApartmentSearchService;
import com.hackathon.agent.domain.service.ScoringService;
import com.hackathon.agent.infrastructure.ai.GigaChatClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Реализация сервиса оценки квартир {@link ScoringService}, использующая AI-модель GigaChat
 * для вычисления скора по неметрическим параметрам.
 * <p>
 * Выполняет пакетную обработку квартир: список идентификаторов разбивается на батчи
 * (размером {@code app.batch.size}), и для каждого батча отправляется запрос в GigaChat
 * для получения числовых оценок. Оценки вычисляются на основе таких критериев, как
 * вид из окна, наличие парковки, этаж и другие качественные характеристики.
 * </p>
 *
 * <p><b>Алгоритм работы:</b></p>
 * <ol>
 *     <li>Получение объектов {@link Apartment} по переданным ID через {@link ApartmentSearchService}.</li>
 *     <li>Разбивка списка квартир на батчи (размер задаётся свойством {@code app.batch.size}).</li>
 *     <li>Для каждого батча формируется промпт с описанием квартир и отправляется в GigaChat.</li>
 *     <li>Парсинг ответа GigaChat: ожидается JSON-массив чисел, например {@code [85, 70, 92]}.</li>
 *     <li>Если парсинг JSON не удался, применяется fallback — извлечение всех чисел из ответа
 *         с помощью регулярного выражения.</li>
 *     <li>Каждое полученное значение ограничивается диапазоном 0–100 (clamp) и сохраняется
 *         в объект {@link ScoredApartment}.</li>
 *     <li>Если для какой-то квартиры оценка не получена (меньше оценок, чем квартир),
 *         присваивается дефолтный скор = 50.</li>
 * </ol>
 *
 * <p><b>Обработка ошибок:</b></p>
 * <ul>
 *     <li>Если список идентификаторов пуст или {@code null}, возвращается пустой список (не {@code null}).</li>
 *     <li>Если квартира не найдена в ERP-системе ({@link ApartmentSearchService#getById} возвращает {@code null}),
 *         она исключается из оценки без выбрасывания исключения.</li>
 *     <li>При ошибке парсинга ответа GigaChat (невалидный JSON) используется fallback-парсинг,
 *         что гарантирует возврат оценки (пусть даже не всегда точной).</li>
 *     <li>Все ошибки логируются на уровне WARN, но не пробрасываются выше, чтобы не прерывать
 *         основной процесс ранжирования.</li>
 * </ul>
 *
 * <p><b>Конфигурация:</b></p>
 * <ul>
 *     <li>{@code app.batch.size} — количество квартир в одном пакетном запросе к GigaChat.
 *         Рекомендуемое значение: 10–20, чтобы не превышать лимиты токенов модели.</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <pre>
 * List&lt;Long&gt; ids = Arrays.asList(1001L, 1002L, 1003L, 1004L);
 * List&lt;ScoredApartment&gt; scored = scoringService.rateApartments(ids);
 * scored.sort(Comparator.comparing(ScoredApartment::getScore).reversed());
 * </pre>
 *
 * @author Axine
 * @since 1.0.0
 * @see ScoringService
 * @see GigaChatClient
 * @see ApartmentSearchService
 * @see ScoredApartment
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GigaChatScoringService implements ScoringService {

    private final GigaChatClient gigaChatClient;
    private final ApartmentSearchService searchService;
    private final ObjectMapper objectMapper;

    /**
     * Размер батча для отправки запросов в GigaChat.
     * <p>
     * Определяет, сколько квартир оценивается в одном вызове AI-модели.
     * Значение задаётся в конфигурации через {@code @Value("${app.batch.size}")}.
     * </p>
     */
    @Value("${app.batch.size}")
    private int BATCH_SIZE;

    /**
     * Пороговое значение времени выполнения (в миллисекундах) для скорингового вызова GigaChat.
     * <p>
     * Аналогично полю в {@link AgentOrchestrator}, но применяется к пакетной оценке квартир
     * (метод {@link GigaChatScoringService#rateBatch(List)}).
     * Если обработка одного батча занимает больше указанного времени, в лог записывается
     * предупреждение (WARN) для анализа производительности.
     * </p>
     * <p>
     * Значение задаётся через свойство {@code app.logging.slow-scoring-threshold-ms}.
     * Рекомендуемое значение: 3000 (3 секунды).
     * </p>
     *
     * @see GigaChatScoringService#rateBatch(List)
     */
    @Value("${app.logging.slow-scoring-threshold-ms}")
    private long slowThresholdMs;

    /**
     * {@inheritDoc}
     * <p>
     * Реализация выполняет пакетную обработку с вызовом GigaChat для каждого батча.
     * Возвращает список {@link ScoredApartment}, содержащий квартиры и их скорее.
     * </p>
     *
     * @param apartmentIds список идентификаторов квартир для оценки
     * @return список оценённых квартир (не {@code null}, может быть пустым)
     */
    @Override
    public List<ScoredApartment> rateApartments(List<Long> apartmentIds) {
        if (apartmentIds == null || apartmentIds.isEmpty()) {
            log.debug("Empty apartment IDs list, returning empty result");
            return List.of();
        }

        long startTime = System.currentTimeMillis();
        log.info("Scoring {} apartments", apartmentIds.size());

        List<Apartment> apartments = new ArrayList<>();
        for (Long id : apartmentIds) {
            try {
                Apartment a = searchService.getById(id);
                if (a != null) {
                    apartments.add(a);
                } else {
                    log.warn("Apartment with id {} not found, skipping scoring", id);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch apartment with id {}: {}", id, e.getMessage());
            }
        }

        if (apartments.isEmpty()) {
            log.info("No valid apartments found for scoring, returning empty result");
            return List.of();
        }

        log.debug("Retrieved {} apartments for scoring (out of {})", apartments.size(), apartmentIds.size());

        List<ScoredApartment> result = new ArrayList<>();

        int totalBatches = (apartments.size() + BATCH_SIZE - 1) / BATCH_SIZE;
        log.debug("Processing {} batches of size {}", totalBatches, BATCH_SIZE);

        for (int i = 0; i < apartments.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, apartments.size());
            List<Apartment> batch = apartments.subList(i, end);
            result.addAll(rateBatch(batch));
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Scoring completed: {} apartments scored in {} ms ({} batches)",
                result.size(), duration, totalBatches);
        return result;
    }

    /**
     * Оценивает один батч квартир с помощью GigaChat.
     * <p>
     * Формирует промпт с описанием всех квартир батча, отправляет запрос к AI-модели,
     * парсит ответ и преобразует в список {@link ScoredApartment}.
     * </p>
     *
     * <p><b>Детали парсинга:</b></p>
     * <ol>
     *     <li>Сначала пытается распарсить JSON-массив.</li>
     *     <li>При неудаче извлекает все числа из ответа с помощью регулярного выражения {@code \\d+}.</li>
     *     <li>Ограничивает каждое значение диапазоном 0–100 с помощью {@link Math#clamp}.</li>
     *     <li>Если оценок меньше, чем квартир, для оставшихся устанавливается дефолтный скор 50.</li>
     * </ol>
     *
     * @param apartments список квартир для оценки (не {@code null}, не пустой)
     * @return список {@link ScoredApartment} в том же порядке, что и входной список
     */
    private List<ScoredApartment> rateBatch(List<Apartment> apartments) {
        if (apartments.isEmpty()) return List.of();

        long batchStart = System.currentTimeMillis();
        log.debug("Scoring batch of {} apartments", apartments.size());

        StringBuilder promptBuilder = getPromptBuilder(apartments);
        String prompt = promptBuilder.toString();

        if (log.isDebugEnabled()) {
            String truncated = prompt.length() > 500 ? prompt.substring(0, 500) + "..." : prompt;
            log.debug("Sending prompt to GigaChat (length={}): {}", prompt.length(), truncated);
        }

        String response;
        try{
            response = gigaChatClient.callForScoring(prompt);
            long callDuration = System.currentTimeMillis() - batchStart;
            log.debug("Received response from GigaChat in {} ms", callDuration);
            if (log.isTraceEnabled()) {
                log.trace("Response: {}", response);
            }
        } catch (Exception e) {
            log.error("GigaChat scoring call failed for batch: {}", e.getMessage(), e);
            return apartments.stream()
                    .map(a -> new ScoredApartment(a, 50))
                    .toList();
        }

        List<Integer> scores = parseScores(response, apartments.size());

        List<ScoredApartment> scored = new ArrayList<>();
        for (int i = 0; i < apartments.size(); i++) {
            int score = (i < scores.size()) ? scores.get(i) : 50;
            score = Math.clamp(score, 0, 100);
            scored.add(new ScoredApartment(apartments.get(i), score));
        }

        long batchDuration = System.currentTimeMillis() - batchStart;
        if (batchDuration > slowThresholdMs) {
            log.warn("Scoring batch took {} ms (threshold {} ms)", batchDuration, slowThresholdMs);
        } else {
            log.debug("Batch scored in {} ms", batchDuration);
        }

        if (log.isDebugEnabled()) {
            double avg = scored.stream().mapToInt(ScoredApartment::getScore).average().orElse(0);
            int min = scored.stream().mapToInt(ScoredApartment::getScore).min().orElse(0);
            int max = scored.stream().mapToInt(ScoredApartment::getScore).max().orElse(0);
            log.debug("Batch scores: min={}, max={}, avg={:.1f}", min, max, avg);
        }

        return scored;
    }

    private List<Integer> parseScores(String response, int expectedCount) {
        List<Integer> scores = new ArrayList<>();
        try {
            String jsonPart = response.replaceAll(".*\\[", "[").replaceAll("\\].*", "]");
            scores = objectMapper.readValue(jsonPart, new TypeReference<List<Integer>>() {});
            log.debug("Parsed {} scores from JSON", scores.size());
        } catch (Exception e) {
            log.warn("Failed to parse JSON from GigaChat response, trying regex fallback", e);
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(response);
            while (matcher.find()) {
                scores.add(Integer.parseInt(matcher.group()));
            }
            log.debug("Extracted {} scores via regex", scores.size());
        }

        while (scores.size() < expectedCount) {
            scores.add(50);
            log.debug("Added default score 50 for missing apartment");
        }
        if (scores.size() > expectedCount) {
            scores = scores.subList(0, expectedCount);
            log.debug("Trimmed scores to first {} values", expectedCount);
        }
        return scores;
    }

    /**
     * Формирует промпт для GigaChat на основе списка квартир.
     * <p>
     * Включает инструкцию оценить квартиры по шкале 0–100 с учётом критериев:
     * вид из окна, наличие парковки, этаж (чем выше — тем лучше).
     * Ожидается ответ в формате JSON-массива чисел.
     * </p>
     *
     * @param apartments список квартир (не {@code null})
     * @return готовый {@link StringBuilder} с промптом
     */
    private static @NonNull StringBuilder getPromptBuilder(List<Apartment> apartments) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Оцени каждую из следующих квартир по шкале от 0 до 100.\n");
        promptBuilder.append("Критерии: вид из окна, наличие парковки, этаж (чем выше — тем лучше).\n");
        promptBuilder.append("Верни ответ в формате JSON-массива чисел, например: [85, 70, 92]\n");
        for (Apartment a : apartments) {
            promptBuilder.append(String.format("ID %d: этаж %d, вид %s, парковка %s\n",
                    a.getId(), a.getFloor(), a.getViewType(), a.getParking() != null && a.getParking() ? "есть" : "нет"));
        }
        return promptBuilder;
    }
}