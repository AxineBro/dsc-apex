[![English](https://img.shields.io/badge/English-Documentation-blue?style=flat-square)](../../README.md)
[![Лицензия](https://img.shields.io/badge/Лицензия-Apache2.0-green?style=flat-square)](../../LICENSE)

<div align="center">
  <h1>Dsc Apex</h1>
  <p>
    <img src="../img/logo.png" alt="Dsc Apex логотип" width="300">
  </p>
</div>

ИИ-помощник для автоматизации бизнес-процессов с использованием GigaChat LLM и внешних API

## Продакшен-развёртывание (для конечных пользователей)

Самый простой способ запустить **Dsc Apex** — использовать готовые Docker-образы, опубликованные в [GitHub Packages](https://github.com/AxineBro/dsc-apex/pkgs/container/dsc-apex).  
Никакой сборки, Java или Node.js не требуется — нужны только **Docker** и **Docker Compose**.

### Необходимые условия
- [Docker](https://docs.docker.com/get-docker/) (версия 20.10+)
- [Docker Compose](https://docs.docker.com/compose/install/) (версия 2.0+)

### Установка в одну команду

Выберите вашу операционную систему:

| ОС | Команда |
| :--- | :--- |
| **Linux / macOS** | `curl -sSL https://raw.githubusercontent.com/AxineBro/dsc-apex/main/install.sh \| bash` |
| **Windows (CMD)** | `curl -sSL https://raw.githubusercontent.com/AxineBro/dsc-apex/main/install.bat -o install.bat && install.bat` |
| **Windows (PowerShell)** | `Invoke-WebRequest -Uri "https://raw.githubusercontent.com/AxineBro/dsc-apex/main/install.ps1" -OutFile "install.ps1"; .\install.ps1` |

Скрипт загрузит `docker-compose.yml` и `.env.example` в текущую папку.

### Следующие шаги

1. **Отредактируйте файл `.env`** – укажите свой `GIGA_CHAT_API_KEY` (обязательно) и при необходимости измените другие переменные.
2. **Запустите приложение:**
   ```bash
   docker-compose up -d
   ```
3. Откройте http://localhost в браузере.

Все сервисы (бэкенд, фронтенд, nginx, PostgreSQL) запустятся автоматически.  
Виджет-чат будет доступен на главной странице.

### Скачать из Releases

Вы также можете загрузить скрипты (и compose-файл) вручную со [страницы Releases](https://github.com/AxineBro/dsc-apex/releases).  
Ищите файлы `install.sh`, `install.bat`, `install.ps1`, `docker-compose.yml` и `.env.example`.

Если вы хотите собрать образы локально из исходников вместо использования готовых, см. раздел **Разработка** ниже.

## Технологический стек

| Слой                 | Технологии                                                      |
| :------------------- | :--------------------------------------------------------------- |
| **Фронтенд**         | `React 19` · `Vite`                                              |
| **Бэкенд**          | `Java 25` · `Spring Boot 4.x`                                    |
| **База данных**         | `PostgreSQL 15`                                                  |
| **Веб сервер**       | `Nginx`                                                          |
| **Контейнеризация** | `Docker` · `Docker Compose`                                      |
| **Сборка и CI/CD**    | `Maven` · `GitHub Actions`                                       |
| **Тестирование**          | `JUnit 5` · `Testcontainers` (backend) / `Playwright` (frontend) |

## Команда

- **Алексей** — *Team Lead / Backend Developer*  
  System Architect · Business Analyst · UX/UI Designer  
  [GitHub](https://github.com/AxineBro)

- **Иван** — *Frontend Developer*  
  Business Analyst · UX/UI Designer  
  [GitHub](https://github.com/Onizuk0)

- **Сергей** — *DevOps / QA Engineer*  
  Business Analyst · UX/UI Designer  
  [GitHub](https://github.com/SergeyTerpugov)

## Документация и архитектура
- [Схема работы агента](architecture/behavior-flow.ru.md) — подробная блок-схема логики ИИ-помощника
- [API-документация](architecture/api_dock.ru.md) — полное описание эндпоинтов, управления сессиями и конфигурации.
- [Структура файлов](architecture/files_tree.ru.md) — подробное описание файловой структуры бэкенда.


## Обзор виджета фронтенда

Встраиваемый ИИ-консультант для сайта ДСК. Собран с помощью `Vite` в один `widget.js`, монтируется через `iframe` с изоляцией стилей.

| Режим | Поведение |
| :--- | :--- |
| `closed` | Круглая кнопка 60px, `iframe` 76x76 |
| `floating` | Карточка 400x680, шапка + история + поле ввода, быстрые чипсы |
| `full` | Модальное окно с отступом 24px + затемнение, `SideMenu` 280px + чат, приветственный экран с 4 карточками |
| `mobile <=640px` | `floating` открывается как `full`, без отступов, `100dvh`, боковое меню полноэкранное с fade-оверлеем |

Ключевые возможности:
- `GET /api/v1/chat/init` + `POST /api/v1/chat/message` с `X-Session-Id`, `sid` сохраняется в `localStorage` как `dsk_sid`
- Система подсказок: 20 промптов в 4 группах (`pick`, `mortgage`, `build`, `docs`), по 1 случайной из группы
- Индикатор размышления (9 вращающихся статусов, 4.5–5.5 с), кнопка отправки блокируется во время загрузки
- Редактирование последнего сообщения / регенерация, копирование, таблицы в markdown, пилюля `sys-info` для передачи менеджеру
- Синхронизация родитель ↔ iframe через `postMessage({ type: 'CHAT_MODE_CHANGE', mode })`, блокировка прокрутки body в режиме `full`
- Вложение `offer_pdf` с коммерческим предложением: при переходе сессии в `OFFER_READY` ответ содержит ссылку на скачивание PDF через `/api/v1/offers/{offerId}/pdf` или `/api/v1/chat/sessions/{sessionKey}/offer/pdf`

## Интеграция виджета (демо в 1 строку)

```html
<script src="/widget.js"></script>
```

Скрипт создаёт `iframe.dsk-chat-iframe` и монтирует `ChatWidget` внутри. Глобальные CSS не протекают.

## Структура репозитория

```text
dsc-apex/
  frontend/
    index.html                  # копия главной страницы ДСК + <script src="./widget.js">
    public/CopyAssetsDSK/       # копия ассетов (bitrix, upload, css, img)
    src/main.tsx                # точка входа виджета, монтирование iframe
    src/Components/             # ChatWidget, ChatLayout, ChatWindow, SideMenu, Suggestions
    src/api/chatApi.ts          # initSession + sendMessage
    dist/                       # результат сборки, НЕ в git
      widget.js
      index.html
      CopyAssetsDSK/
  backend/
    src/main/java/...           # ChatController, ChatFacade, AgentOrchestrator
    src/main/resources/         # prompts/, templates/, data/apartments.json
  nginx/nginx.conf              # / -> index.html, /api/ -> backend:8080
  docker-compose.yml
  .env                          # НЕ в git
```

## Разработка (быстрый старт)

### 1. Разработка фронтенда (UI + живой бэкенд)

```bash
cd frontend
npm ci
npm run dev
# откройте http://localhost:5173
```

`vite.config.ts` уже проксирует `/api` на `http://localhost:8080` для разработки. В продакшене используются относительные `/api/...` через nginx, пересборка не требуется.

### 2. Разработка бэкенда

```bash
docker compose up db -d
cd backend
export GIGA_CHAT_API_KEY='xxx'
export GIGA_CHAT_SCOPE='GIGACHAT_API_PERS'
export GIGA_CHAT_MODEL='GigaChat'
export DB_URL=jdbc:postgresql://localhost:5432/agentdb
export DB_USERNAME=postgres
export DB_PASSWORD=secret
./mvnw spring-boot:run
# проверка: curl http://localhost:8080/api/v1/chat/init
```

### 3. Полный стек через Docker (демо)

```bash
cd frontend && npm ci && npm run build
cd ..
docker compose up db backend nginx -d --build
# откройте http://localhost:80/
# проверка: curl http://localhost:80/api/v1/chat/init
```

`nginx` отдаёт `frontend/dist/index.html` и проксирует `/api/*` на `backend:8080`, поэтому `CORS` не требуется.

<details>
<summary><b>Переменные окружения</b></summary>

| Переменная | Значение по умолчанию | Описание |
| :--- | :--- | :--- |
| `GIGA_CHAT_API_KEY` | — | Ключ авторизации (base64) из SaluteAI, обязателен |
| `GIGA_CHAT_SCOPE` | `GIGACHAT_API_PERS` | должен соответствовать типу ключа |
| `GIGA_CHAT_MODEL` | `GigaChat` | используйте базовую модель для бесплатного тарифа, `GigaChat-Pro` платная |
| `DB_URL` | `jdbc:postgresql://localhost:5432/agentdb` | `db:5432` внутри compose |
| `DB_USERNAME` / `DB_PASS` | `postgres` / `secret` | обратите внимание: `DB_PASS` в compose против `DB_PASSWORD` для `mvn` |
| `APP_MAX_CYCLES` | `5` | количество итераций поиска |
| `APP_FLAT_LIMIT` | `5` | максимум квартир в выдаче |

Полный список смотрите в `docs/ru/architecture/api_dock.ru.md`.

</details>

<details>
<summary><b>Заметки по сборке и устранение проблем</b></summary>

- Сборка: `tsc -b && vite build` как библиотека `IIFE` в один `widget.js`. React встраивается внутрь, `assetsInlineLimit` встраивает логотипы.
- `public/*` копируется в `dist/*` автоматически. `index.html` копируется через скрипт `cp index.html dist/index.html`, потому что `lib`-режим игнорирует его.
- Никогда не коммитьте `dist/`, `node_modules/`, `.env`.
- Предпросмотр через `file://` никогда не работает для копии ДСК (`BX`, `$`, `CORS`). Используйте `npx serve dist -l 4173`, а не двойной клик.
- `process is not defined` в `widget.js` — исправлено через `define: { 'process.env.NODE_ENV': '"production"' }` в `vite.config.ts`.
- `402 Payment Required` от GigaChat — переключите `GIGA_CHAT_MODEL` на `GigaChat`, проверьте активацию модели в кабинете SaluteAI.
- `Row was already updated (optimistic lock)` — исправлено в `ChatFacade` повторным сохранением, см. ветки `fix/lombok-jdk25`, `fix/session-double-save`.
- `value too long for type character varying(20)` в `manager_tasks` — расширьте `client_phone` до `varchar(50)` и очистите номера-заглушки.
- `PDF содержит «кракозябры» вместо кириллицы` — проверьте, что `app.offer.pdf-font-path` указывает на TTF-шрифт с поддержкой кириллицы (например, DejaVuSans.ttf), а имя в `app.offer.pdf-font-family` совпадает с CSS-шаблоном.

</details>


## Обзор архитектуры (бэкенд)

Бэкенд построен по многослойной архитектуре, что обеспечивает разделение ответственности и упрощает поддержку:

| Слой             | Ответственность                                                                 |
| :--------------- | :------------------------------------------------------------------------------- |
| **Контроллеры**  | REST-эндпоинты, валидация запросов, обработка заголовка сессии, форматирование ответов. |
| **Прикладной**   | Фасад (`ChatFacade`), оркестратор (`AgentOrchestrator`), менеджер сессий (`SessionManager`). |
| **Доменный**     | Основная бизнес-логика: состояние сессии, фильтры, скоринг, поиск квартир, генерация предложений. |
| **Инфраструктурный** | Внешние интеграции (клиент GigaChat, JSON-источник данных, JPA-репозитории, сервис уведомлений, генерация PDF). |

### Выдача коммерческого предложения (PDF)

Бэкенд формирует PDF-файл коммерческого предложения (через `OfferPdfGenerator` на основе HTML-шаблона) и сохраняет его в базе данных в таблице `offers` со статусом `READY`. Для скачивания PDF предоставлены два эндпоинта:

| Эндпоинт                                            | Описание                                                      |
| :-------------------------------------------------- | :------------------------------------------------------------ |
| `GET /api/v1/offers/{offerId}/pdf`                  | Скачивание конкретного оффера по его UUID.                    |
| `GET /api/v1/chat/sessions/{sessionKey}/offer/pdf`  | Скачивание последнего КП для указанного ключа сессии.         |

При переходе сессии в состояние `OFFER_READY` в ответе `ChatResponse` появляется вложение `offer_pdf` с относительной ссылкой на эндпоинт скачивания. Подробности — в [API-документации](architecture/api_dock.ru.md).

### Ключевые паттерны проектирования

- **Фасад** – упрощает слой контроллеров, делегируя задачи оркестраторам.
- **Оркестратор** – координирует конвейер обработки AI (история, промпт, инструменты, ответ).
- **Репозиторий** – Spring Data JPA для доступа к данным.
- **Маппер** – преобразует доменные модели в JPA-сущности и обратно.
- **ThreadLocal** – обеспечивает контекст сессии в рамках запроса без явной передачи параметров.
- **Стратегия** – обычный и расширенный поиск.

### Устойчивость и отказоустойчивость

Бэкенд использует **Spring Resilience4j** (включён через `@EnableResilientMethods`) для:

- Автоматического **повтора** временных сбоев (сетевые тайм-ауты, недоступность GigaChat).
- **Автоматического выключателя** (circuit breaker) для внешних вызовов, предотвращающего каскадные отказы.
- **Ограничения по времени** выполнения долгих операций во избежание голодания потоков.

Все критические операции (создание сессии, обработка сообщений, скоринг) аннотированы `@Transactional`, что гарантирует атомарность и согласованность данных.

### Логирование и наблюдаемость

Каждому запросу присваивается **correlation ID** (UUID), который передаётся по всей цепочке вызовов с использованием **MDC** (Mapped Diagnostic Context). Это обеспечивает:

- Сквозную трассировку по логам.
- Простую отладку пользовательских сессий.
- Мониторинг производительности с настраиваемыми **порогами медленных запросов** (задаются через переменные окружения).

Все значимые события (создание сессии, вызовы AI, эскалации, ошибки) логируются на соответствующих уровнях (`INFO`, `WARN`, `DEBUG`). Стратегия логирования также служит **аудиторским следом** для целей безопасности и соответствия требованиям.

## Безопасность

Хотя текущая версия не реализует аутентификацию (планируется в будущих релизах), в системе заложены следующие защитные механизмы:

| Мера | Преимущество |
| :--- | :----------- |
| **Статусные токены сессий** (UUID) | Легко ротируются, не содержат конфиденциальных данных. |
| **Валидация входных данных** (Jakarta Validation) | Предотвращает инъекции и некорректные полезные нагрузки. |
| **Структурированное логирование с correlation ID** | Позволяет проводить криминалистический анализ и мониторинг подозрительной активности. |
| **Оптимистичная блокировка** (JPA `@Version`) | Предотвращает повреждение сессии при конкурентном доступе. |
| **Транзакционные границы** | Обеспечивает целостность данных даже в сценариях ошибок. |
| **Паттерны устойчивости** | Смягчают эффекты, похожие на DoS-атаки, при сбоях внешних сервисов. |

Мы рассматриваем **логирование как элемент безопасности** – все операции, влияющие на пользовательские данные или изменяющие состояние, записываются с достаточным контекстом (ID сессии, IP, User-Agent, длительность). Это позволяет операторам обнаруживать аномалии, расследовать инциденты и соответствовать аудиторским требованиям.

В будущих версиях планируется добавить:

- Аутентификацию через **OAuth2 / JWT**.
- **Ограничение скорости** запросов (на сессию или по IP).
- **Маскирование конфиденциальных данных** в логах.
- Принудительное использование **TLS/SSL**.

## Лицензия

Этот проект распространяется под лицензией [Apache License 2.0](../../LICENSE).