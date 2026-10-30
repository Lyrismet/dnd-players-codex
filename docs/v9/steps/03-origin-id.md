# Шаг 3. Идентичность данных (`origin_id`)
- **Кому:** агент-разработчик KMP (SQLDelight, миграции, формат файла). Ревью: QA-агент, обязательно проверка миграции. Устройство: владелец (установка поверх базы v8 с данными).
- **Цель:** у кампании, записей кодекса, сессий, заметок и боёв есть неизменный `origin_id` (UUID), он выживает в экспорте и импорте; есть таблица алиасов. Данные v8 живут. Экранов нет.
- **Задачи:** 4.2 `DESIGN_V9.md:223-232`. Новые таблицы следующих шагов создаются с `origin_id` сразу. Только `origin_id` и алиасы: классификация, слияние и сравнение это шаги 7.
- **Читать (только это):**

| Файл : строки | Зачем |
|---|---|
| `DESIGN_V9.md:27-40` | "Готовность задачи" |
| `DESIGN_V9.md:223-232` | задача 4.2 |
| `DESIGN_V9.md:599-617` | приложение Б (миграции), строка 4.2 |
| `DESIGN_V9.md:618-636`, `677-678` | приложение В (тесты миграций), сценарий 13 |
| `CLAUDE.md` | раздел "Schema changes need a migration", "Tests" |
| Прототип | не нужен: в нём нет `origin_id` |

- **Код (где работать)**, пути от `shared/src/commonMain/`:
  - таблицы: `sqldelight/com/lyrismet/incadent/db/{Campaign,Npc,Quest,Location,Species,PartyMember,SessionNote,SessionEntry,Combat}.sq`. Ключи записей `INTEGER AUTOINCREMENT`, у `campaign.id` это `TEXT`. Добавить колонку `origin_id TEXT NOT NULL` + уникальный индекс, запросы вставки (в том числе варианты с явным `id` для импорта, например `Npc.sq:44,49`) принимают его.
  - миграция: `sqldelight/migrations/2.sqm` (сейчас только `1.sqm`; схема версии 2, после миграции версия 3). Backfill `lower(hex(randomblob(16)))`, у каждой строки свой. Снимок схемы лежит в `sqldelight/databases/` (есть `1.db`, `2.db`): после изменения создать `3.db` задачей SQLDelight (имя задачи проверить `./gradlew tasks --all | grep -i schema`), `verifyMigrations` включён (`shared/build.gradle.kts:117`).
  - репозитории, которые создают записи (генерируют `origin_id` при создании): `feature/codex/data/{Npc,Quest,Location,Species,Party}RepositoryImpl.kt`, `feature/session/data/{SessionNote,SessionEntry}RepositoryImpl.kt`, `feature/combat/data/CombatRepositoryImpl.kt`, `feature/campaign/data/{CampaignRepositoryImpl,CampaignInsert}.kt`.
  - файл обмена: `feature/campaign/data/file/CampaignFile.kt` (`FILE_VERSION = 2`, строка 26: поднять), `CampaignExporter.kt` (410), `CampaignImporter.kt` (494), `FileIds.kt` (50, номера внутри файла остаются); файлы v8 без `origin_id` читаются как раньше (по имени и типу).
  - домен: алиасы как интерфейс в `feature/campaign/domain/repository`, реализация в `feature/campaign/data`.
- **Миграция и тесты:** `2.sqm`. Тест на старой версии в `shared/src/androidHostTest/.../persistence/SchemaMigrationTest.kt` (образец: открывает `databases/1.db`, мигрирует, проверяет строки) по тому же образцу для `2.db`: строки целы, у каждой свой уникальный `origin_id`. Тест экспорта и импорта: `origin_id` едет в файл и возвращается. Файл без `origin_id` импортируется. `TestDatabase.kt` в `androidHostTest/.../persistence/`.
- **Не трогать:** экран сравнения, классификацию, слияние (шаг 7), предметы и лист (новые таблицы позже), UI.
- **Приёмка:** (1) установка поверх базы v8 с данными: все кампании, записи, сессии, бои на месте (сценарий 13); (2) новая запись получает `origin_id`; (3) экспорт и импорт сохраняют `origin_id`; (4) файл v8 без `origin_id` по-прежнему импортируется; (5) тест миграции зелёный.
- **Строки и словарь:** новых пользовательских строк нет.
- **Готово, когда:** "Готовность задачи" (`DESIGN_V9.md:27-39`), миграция и тест, шаг 3 отмечен `[x]` в приложении А, пункты `FEATURES.md` раздела 18 про `origin_id`.
