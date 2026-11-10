# Шаг 7: Сессии: даунтайм и режим правки списка

- **Кому:** агент по данным и Compose (сессии). Вид и жесты на устройстве проверяет владелец.
- **Зависит от:** нет.
- **Миграции:** `18.sqm` (номера сверить с папкой миграций).

Шаг состоит из частей, делать по порядку, после каждой сборка, `ktlintFormat`, `detekt`. Один PR на весь шаг. Если часть оказалась слишком большой, остановиться и записать это в отчёт.

## Часть 1. Даунтайм между сессиями
**Цель:** Даунтайм как элемент списка сессий: модель, карточка, лист правки, внесение золота и предметов, отмена внесения.

**Читать (только это):** `DEV.md`: эпик H (H.1). Вид: `Design Discussion 2`, раунды 17-18. `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/session/presentation/list/SessionListUi.kt, SessionListPresenter.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/session/domain/model/SessionNote.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/codex/domain/model/CoinPurse.kt, Item.kt`
- `shared/src/commonMain/sqldelight/com/lyrismet/incadent/db/SessionNote.sq`

**Миграция и тесты:** `18.sqm`: таблица `downtime`. Тест миграции. Тесты: внесение, снятие внесённого, удаление записи убирает внесённое, привязка к более ранней сессии.

**Не трогать:** режим правки списка (шаг 7, часть 2).

**Приёмка:**
- Карточка в списке, полный вид, лист правки.
- Тост "Внесено в лист" с отменой.
- Удаление записи убирает внесённое, тост возвращает всё.
- Архив: только чтение.

**Строки и словарь:** `downtime_*`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.

## Часть 2. Режим правки списка сессий
**Цель:** Режим "Изменить": перенос сессий за ≡, "+ Прошлая сессия", пунктиры "+ Между сессиями N и N+1", "?" про номера.

**Читать (только это):** `DEV.md`: эпик H (H.2). Вид: `Design Discussion 2`, раунд 19. `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/session/presentation/list/SessionListUi.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/session/domain/SessionNumbering.kt (`chronologicalIndex`)`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/session/domain/repository/SessionNoteRepository.kt`
- `shared/src/commonMain/sqldelight/com/lyrismet/incadent/db/SessionNote.sq`

**Миграция и тесты:** миграций нет (даунтайм уже привязан к сессии, шаг 7, часть 1). Тесты: дата между соседями, одинаковые даты, перенос не ломает номера, Undo.

**Не трогать:** детали сессии, записи внутри сессий.

**Приёмка:**
- "Изменить" и "Готово", плашка с "?".
- Перенос за ≡, номера на лету, тост "Порядок изменён" с отменой.
- "+ Прошлая сессия" под текущей.
- Пунктиры между любыми двумя, в архиве режима нет.

**Строки и словарь:** `session_list_edit_*`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.
