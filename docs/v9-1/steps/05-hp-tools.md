# Шаг 5: Хиты: до максимума, точные, шкала

- **Кому:** агент по данным и Compose (бой). Вид и жесты на устройстве проверяет владелец.
- **Зависит от:** нет.
- **Миграции:** нет (номера сверить с папкой миграций).

Шаг состоит из частей, делать по порядку, после каждой сборка, `ktlintFormat`, `detekt`. Один PR на весь шаг. Если часть оказалась слишком большой, остановиться и записать это в отчёт.

## Часть 1. "До максимума" и клавиатура калькулятора
**Цель:** Кнопка "До максимума" в режиме лечения боевого калькулятора и в режиме "+" калькулятора листа "Я", новая раскладка клавиатуры.

**Читать (только это):** `DEV.md`: эпик F (F.1). Вид: `Design Discussion`, раунд 15. `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/core/designsystem/component/NumberPadSheet.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/presentation/round/CalcBuilder.kt, CalcHandler.kt, CalcSheetUi.kt, PanelHpBlock.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/me/domain/HitPoints.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/domain/damage/HpChange.kt`

**Миграция и тесты:** миграций нет. Тесты: до максимума для одной цели, нескольких, неизвестный максимум, временные хиты не трогаются, одна отмена на несколько целей.

**Не трогать:** режим "Урон" калькулятора (множители, типы урона, "От кого", "Ещё удар").

**Приёмка:**
- Кнопка слева от применения, подпись "до 28 · +6" / "целей: 2".
- Неактивна при полных хитах и неизвестном максимуме.
- Тост с отменой, временные хиты не тронуты.
- Клавиатура "7 8 9 ⌫ / 4 5 6 + / 1 2 3 / C 0 00".

**Строки и словарь:** `calc_heal_to_max_*`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.

## Часть 2. Точные хиты и шкала хитов
**Цель:** Ползунок "Примерно сколько осталось", строка "Точно · Вписать", сверка лечения, расширение `forecastHits`.

**Читать (только это):** `DEV.md`: эпик F (F.2). Вид: `Design Discussion`, раунд 16. `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/domain/forecast/HitForecast.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/data/HitObservationRepositoryImpl.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/presentation/round/RoundForecast.kt, PanelHpBlock.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/domain/results/HitObservationDraft.kt`

**Миграция и тесты:** миграций нет (проверить, хватает ли `combat_event`). Тесты `forecastHits`: случаи чародея и паладина, неизвестная сумма лечения, лечили врага.

**Не трогать:** слова состояния хитов (остаются как способ выбора), режим "Урон".

**Приёмка:**
- Ползунок с подписями Упал-Цел, оценка "≈ N" с источником.
- "Вписать" открывает калькулятор в режиме точного числа, итог "= N".
- Диалог "Лечение не записано: N" или "Просто N".
- Прогноз врага верен после лечения.

**Строки и словарь:** `hp_scale_*`, `hp_exact_*`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.
