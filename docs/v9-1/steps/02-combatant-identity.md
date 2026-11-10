# Шаг 2: Участник боя: имя, роль, связь, слияние NPC

- **Кому:** агент по данным и KMP (бой, кодекс). Вид и жесты на устройстве проверяет владелец.
- **Зависит от:** нет.
- **Миграции:** `13.sqm`, `14.sqm` (номера сверить с папкой миграций).

Шаг состоит из частей, делать по порядку, после каждой сборка, `ktlintFormat`, `detekt`. Один PR на весь шаг. Если часть оказалась слишком большой, остановиться и записать это в отчёт.

## Часть 1. Участник боя: имя, роль, связь с кодексом
**Цель:** Строка роли "вид · класс · подкласс", метка связи с кодексом, имя из кодекса вместо копии, переименование в бою, поле "Роль" и кнопки "В кодекс" и "Связать с NPC".

**Читать (только это):** `DEV.md`: эпик C (C.1-C.4). Вид: `Design Discussion`, раунды 1-2. `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/domain/model/Combat.kt (`Combatant`)`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/domain/round/NameChoice.kt, NpcPromotion.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/domain/grouping/CombatNames.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/presentation/round/RoundRowUi.kt, RoundRows.kt, NamingBuilder.kt, NamingHandler.kt, NamingUi.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/data/CombatRepositoryImpl.kt, CombatMappers.kt`
- `shared/src/commonMain/sqldelight/com/lyrismet/incadent/db/Combatant.sq`

**Миграция и тесты:** `13.sqm`: `combatant.role TEXT NOT NULL DEFAULT ''` (сверить номер). Тест миграции на старой версии. Тесты: сжатие строки роли (порядок обрезки), имя из кодекса и ручное имя, перенос роли в NPC при "В кодекс".

**Не трогать:** слияние NPC (шаг 2, часть 2), метки (шаг 4, часть 1), очередь.

**Приёмка:**
- Строка участника из двух строк, роль не режется несоразмерно ширине.
- Ромб у связанных, пунктирное "только в бою" у остальных.
- Переименование связанного видно в кодексе и заметках, тост с отменой.
- "В кодекс" переносит роль, "Связать с NPC" показывает список.

**Строки и словарь:** `combatant_*`, `combat_naming_*`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.

## Часть 2. Слияние двух NPC с одним именем
**Цель:** Кнопка "Объединить", когда введённое имя уже есть в кодексе: слияние записей NPC со всеми связями и Undo.

**Читать (только это):** `DEV.md`: эпик C (C.5). Вид: `Design Discussion`, раунд 2 (одно имя). `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/codex/data/` (репозиторий NPC)
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/campaign/data/file/ImportRollback.kt` (образец журнала переехавших строк), `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/mention/domain/` (`remapMentionTokens`)
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/campaign/data/OriginAliasRepositoryImpl.kt`
- `shared/src/commonMain/sqldelight/com/lyrismet/incadent/db/Npc.sq`, `NpcLocation.sq`, `Quest.sq`, `Combatant.sq`

**Миграция и тесты:** `14.sqm`: журнал слияния (какие строки переехали) для Undo. Тесты на каждую связь (участники боя, места, квесты, упоминания, алиасы) и на Undo.

**Не трогать:** остальные поля записи; логику переименования в бою (шаг 2, часть 1).

**Приёмка:**
- Имя уже есть: под полем карточка той записи и кнопка "Объединить".
- После объединения всё переехало, у выжившей пустые поля заполнены, заполненные не затёрты.
- Undo возвращает проигравшую под старым id и все связи.

**Строки и словарь:** `npc_merge_*`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.
