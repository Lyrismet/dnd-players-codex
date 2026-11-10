# Шаг 4: Метки, факты NPC, "Особенности" и щиты

- **Кому:** агент по данным и Compose (бой, кодекс). Вид и жесты на устройстве проверяет владелец.
- **Зависит от:** шаги 1, 2.
- **Миграции:** `15.sqm`, `16.sqm` (номера сверить с папкой миграций).

Шаг состоит из частей, делать по порядку, после каждой сборка, `ktlintFormat`, `detekt`. Один PR на весь шаг. Если часть оказалась слишком большой, остановиться и записать это в отчёт.

## Часть 1. Метки участника
**Цель:** Хранение и интерфейс меток: вкладка "Метки", лист "Новая метка", концентрация, счётчик раундов, свои состояния кампании.

**Читать (только это):** `DEV.md`: эпик E (E.1). Вид: `Design Discussion`, раунды 9-11. `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/domain/model/Combat.kt (`CombatCondition`)`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/presentation/round/RoundPanelTabsUi.kt, ConditionTexts.kt, RoundPanelBuilder.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/data/CombatRepositoryImpl.kt`
- `shared/src/commonMain/sqldelight/com/lyrismet/incadent/db/Combatant.sq`

**Миграция и тесты:** `15.sqm`: `combatant_mark`, `custom_condition`, бэкфилл `CONCENTRATION` в метки. Тест миграции. Тесты: счётчик на смене раунда и откате (шаг 1, часть 2), замена концентрации.

**Не трогать:** блок "Особенности" и факты (шаг 4, часть 2), заклинания.

**Приёмка:**
- "+ Метка" создаёт метку, концентрация одна и заменяется.
- Счётчик: янтарный на последнем раунде, красный на нуле, тост "Время вышло".
- Свои состояния общие на кампанию.
- Старые концентрации перенесены.

**Строки и словарь:** `combat_mark_*`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.

## Часть 2. Факты NPC, блок "Особенности", щиты в запись NPC
**Цель:** Поле "Сведения" у NPC, блок "Особенности" в записи и в бою, щиты всегда в запись NPC, убрать состояние "Не знаем".

**Читать (только это):** `DEV.md`: эпик E (E.2). Вид: `Design Discussion`, раунды 12-13. `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/codex/domain/model/Npc.kt, Species.kt (`DamageState`)`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/presentation/round/RoundVulnerabilities.kt, VulnerabilityLearner.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/codex/presentation/style/ (щиты `AppDamageShield`)`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/data/CombatFactsSourceImpl.kt`
- `shared/src/commonMain/sqldelight/com/lyrismet/incadent/db/Npc.sq`

**Миграция и тесты:** `16.sqm`: `npc.facts TEXT NOT NULL DEFAULT ''`. Тесты: запись щитов в NPC, не в вид; наследование видовых; `UNKNOWN` читается как обычный; Undo "Добавлено в NPC".

**Не трогать:** метки (шаг 4, часть 1), щиты в листе "Я" (шаг 8, часть 1).

**Приёмка:**
- В записи NPC блок "Особенности" со щитами и чипами "Сведения".
- Нажатие на щит по кругу, удержание открывает лист.
- В бою блок не выше трёх строк, "ещё N ›".
- Щиты врага без записи в бою, "В кодекс" переносит.

**Строки и словарь:** `entity_sheet_npc_*`, `combat_features_*`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.
