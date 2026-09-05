package com.hackathon.agent;

import com.hackathon.agent.scheduler.TimeoutScheduler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Главный класс приложения Agent (чат-бот для подбора квартир).
 * <p>
 * Является точкой входа в Spring Boot приложение. Инициализирует контекст,
 * активирует необходимые инфраструктурные возможности через аннотации
 * и запускает встроенный веб-сервер.
 * </p>
 *
 * <p><b>Аннотации:</b></p>
 * <ul>
 *     <li>{@link SpringBootApplication} — маркирует класс как основной
 *         Spring Boot приложения, включает автоматическую конфигурацию,
 *         сканирование компонентов и конфигурацию.</li>
 *     <li>{@link EnableAsync} — активирует асинхронное выполнение методов
 *         (используется для фоновых задач, например, отправки уведомлений,
 *         вызовов внешних API без блокировки основного потока).</li>
 *     <li>{@link EnableScheduling} — активирует планировщик задач,
 *         позволяющий выполнять методы по расписанию (используется в
 *         {@link TimeoutScheduler} для обработки тайм-аутов сессий).</li>
 *     <li>{@link EnableResilientMethods} — включает механизмы устойчивости
 *         (retry, circuit breaker, time limiter) для защиты вызовов
 *         внешних сервисов, например GigaChat API.</li>
 * </ul>
 *
 * <p><b>Назначение:</b></p>
 * <ul>
 *     <li>Инициализация Spring-контекста и всех бинов приложения.</li>
 *     <li>Активация асинхронной обработки для улучшения производительности
 *         и отзывчивости.</li>
 *     <li>Включение планировщика для фоновых задач (управление сессиями,
 *         очистка и напоминания).</li>
 *     <li>Включение механизмов устойчивости для надёжной работы с внешними
 *         зависимостями (GigaChat, ERP-система).</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * Приложение запускается стандартно через выполнение метода {@code main},
 * который делегирует управление {@link SpringApplication#run}.
 * </p>
 *
 * <p><b>Конфигурация:</b></p>
 * Все необходимые настройки (порт, параметры БД, API-ключи GigaChat, пути к шаблонам)
 * загружаются из внешних файлов (application.yml, application.properties)
 * и переменных окружения.
 * </p>
 *
 * <p><b>Пример запуска:</b></p>
 * <pre>
 * java -jar agent-1.0.0.jar
 * </pre>
 *
 * @author Axine
 * @since 1.0.0
 * @see SpringBootApplication
 * @see EnableAsync
 * @see EnableScheduling
 * @see TimeoutScheduler
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableResilientMethods
public class AgentApplication {

	/**
	 * Точка входа в приложение.
	 * <p>
	 * Запускает Spring Boot приложение, инициализирует контекст,
	 * поднимает веб-сервер и начинает обработку входящих запросов.
	 * </p>
	 *
	 * @param args аргументы командной строки (могут использоваться для
	 *             переопределения конфигурации)
	 */
	static void main(String[] args) {
		SpringApplication.run(AgentApplication.class, args);
	}

}
