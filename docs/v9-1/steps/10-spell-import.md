# Шаг 10: Импорт заклинания по ссылке

- **Кому:** агент-разработчик Compose и Kotlin Multiplatform. Вид и жесты на устройстве проверяет владелец.
- **Зависит от:** шаг 6 (заклинания).
- **Миграции:** нет. Импортированное заклинание сохраняется обычной записью `character_spell`.

Шаг состоит из частей, делать по порядку, после каждой сборка, `ktlintFormat`, `detekt`. Один PR на весь шаг. Если часть оказалась слишком большой, остановиться и записать это в отчёт.

## Часть 1. Распознавание и разбор ссылки
**Цель:** Чистая логика без сети: из ссылки получить источник и запрос, из ответа dnd.su (и его хостов), Open5e и dnd5eapi.co получить заполненное заклинание, урон и усиление из таблиц источника (если они есть) и список кубов из описания.

**Читать (только это):** `DEV.md`: эпик K, разделы K.2 и K.4. `DESIGN.md`: задача 17, "Принято". `docs/CORE_COMPONENTS.md` не нужен.

**Код (где работать):** пути от `shared/src/commonMain/kotlin/com/lyrismet/incadent/`.
- `feature/codex/domain/spellimport/` (`SpellSource`, `ImportedSpell`), разбор ссылки, мапперы источников и поиск кубов лежат в `feature/codex/data/remote/spellimport/`
- `feature/codex/domain/model/CharacterSpell.kt` (модель, только читать)
- `feature/me/presentation/spells/RangeUnit.kt` и `School.kt` (как значения расставляются в плитки, читать, чтобы сопоставить поля)

**Миграция и тесты:** миграций нет. Тесты в `shared/src/commonTest/kotlin/com/lyrismet/incadent/feature/codex/data/remote/spellimport/`: ссылка (схема, `www.`, пробелы, чужой хост, страница и адрес API каждого сайта), каждый маппер на коротких своих образцах (не копиях чужих страниц), обязательные поля без которых отказ, дальность и время и компоненты и длительность, кубы `NdM` по порядку без повторов. Имена тестов в обратных кавычках без запятых и точек. Точный вид страниц Open5e и адресов API сверить на живых сайтах до написания разбора и записать в отчёт.

**Не трогать:** `CharacterSpell`, форму и шиты, репозитории. Снаряды и бесплатные применения не заполнять, урон и усиление только из таблиц источника (раздел K.4 `DEV.md`).

**Приёмка:**
- Ссылки с `http://`, без схемы, с `www.` и лишними пробелами приводятся к одному виду, чужой хост даёт `UnsupportedSite`.
- Разбор не пишет мусор: без названия, круга или описания результат это `Unreadable`.
- Урон и усиление заполнены из таблиц dnd5eapi.co и Open5e v2 и пусты у остальных источников, кубы из описания собраны в `dice`.

**Строки и словарь:** строк нет, кроме тех, что нужны части 3.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные, тесты на iOS-имена без запятых и точек.

## Часть 2. Загрузка по сети
**Цель:** Подключить Ktor и `SpellImporter` с четырьмя отказами, таймаутом, лимитом ответа и отменой, добавить событие аналитики `spell_imported`.

**Читать (только это):** `DEV.md`: эпик K, разделы K.3 и K.5 (про событие). `CLAUDE.md`: разделы "Dependency injection" и "Planned: Firebase sharing" (сеть живёт в `data/remote`).

**Код (где работать):** пути от корня репозитория.
- `gradle/libs.versions.toml` и `shared/build.gradle.kts` (Ktor: клиент в `commonMain`, OkHttp в `androidMain`, Darwin в `iosMain`; версию сверить с совместимостью с Kotlin 2.4.20)
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/codex/domain/repository/` (интерфейс `SpellImporter`, результат с отказами)
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/codex/data/remote/` (реализация на Ktor с `@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)`)
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/core/analytics/AnalyticsEvent.kt` (событие `SpellImported(source)`, параметр `source`: `dnd_su`, `next_dnd_su`, `open5e`, `dnd5eapi`)
- `shared/src/commonTest/kotlin/com/lyrismet/incadent/core/analytics/AnalyticsEventTest.kt`

**Миграция и тесты:** миграций нет. Тест события аналитики. Клиент проверяется на подставном движке Ktor (`MockEngine`) в `androidHostTest` или `commonTest`: 404 это `NotFound`, нет сети это `Offline`, слишком большой ответ и мусор это `Unreadable`, отмена корутины прерывает запрос. `ArchitectureTest`: Ktor не импортируется в `domain` и `presentation`.

**Не трогать:** Firebase, существующие репозитории, манифест (`INTERNET` уже есть).

**Приёмка:**
- Запрос только `https`, без переходов на другой хост, с таймаутом и лимитом 1 МБ, без личных данных в заголовках.
- Ссылка и ответ нигде не сохраняются и не логируются, кроме названия источника.
- Сборка Android проходит с Ktor, `./gradlew :shared:iosSimulatorArm64Test` компилируется (iOS на устройстве проверяет владелец).

**Строки и словарь:** строк нет.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", сборка, `ktlintFormat`, `detekt` и тесты зелёные.

## Часть 3. Кнопка, шит источника и форма со строкой-подсказкой
**Цель:** Кнопка импорта слева от ✕ в шите нового заклинания (лист "Я" и бой), шит источника с состояниями и ошибками, форма с `HoldHintCard` после импорта.

**Читать (только это):** `DEV.md`: эпик K, разделы K.1, K.5 и K.6. Вид: `v9.1 Design Discussion 6.dc.html`, раунд 33, состояния `z17_entry`, `z17_source`, `z17_form_hint` (в макете ✕ на строке-подсказке заменяется "Понятно", кубов в строке нет, их добавить по K.5). `docs/CORE_COMPONENTS.md`: `AppSheetHeader`, `HoldHintCard`, `AppBottomSheet`, поля формы, кнопки.

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/me/presentation/spells/SpellEditSheet.kt` (кнопка слева от ✕ только для `isNew` и целой формы, строка-подсказка над названием)
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/me/presentation/spells/SpellsModels.kt` (события открытия шита, ввода, отправки, закрытия, состояние шита и строки-подсказки в `SpellsUiState`, `reset()` очищает)
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/me/presentation/spells/SpellsHandler.kt` (импорт через `SpellImporter`, отмена при закрытии шита, замена `ui.draft`, событие аналитики)
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/me/presentation/spells/` (новый файл шита источника)
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/core/designsystem/component/AppSheetHeader.kt` (слот кнопки слева от ✕, новая запись в `docs/CORE_COMPONENTS.md`)
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/me/presentation/MePresenter.kt` и `MeCircuit.kt` (инъекция `SpellImporter`, `AnalyticsTracker`)
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/combat/presentation/round/RoundMagic.kt` и `CombatRoundCircuit.kt` (то же для панели боя, иначе в бою у кнопки нет обработчика)
- `shared/src/commonMain/composeResources/values/strings.xml` и `values-en/strings.xml`

**Миграция и тесты:** миграций нет. Тест жизненного цикла строки-подсказки на обработчике: появляется после импорта, исчезает после первой правки, по "Понятно", при закрытии и сохранении. Тест, что кнопки нет у существующего заклинания и в режиме "только чтение" (если это решается в чистой функции или обработчике).

**Не трогать:** саму форму заклинания и её поля, карточку, список, шит школы и остальные шиты вкладки "Магия", разрешения платформ, Firebase.

**Приёмка:**
- Кнопка стоит слева от ✕ в шите нового заклинания в "Я" и в бою, нажимаемая область 48 x 48, подпись для диктора "Импорт по ссылке". У существующего заклинания и в шитах блоков её нет.
- В шите источника: поле, "Вставить" (после ввода ✕ очистки), перечень сайтов, подпись про загрузку на устройстве, кнопка "Импортировать" (неактивна при пустом поле, с индикатором при загрузке, повторно не нажимается). Смахивание шита отменяет загрузку.
- Четыре ошибки строкой под полем, шит остаётся, ссылка остаётся. "Не удалось разобрать страницу" даёт "Создать вручную": закрывает шит источника.
- После успеха форма заполнена, сверху `HoldHintCard` "Импортировано с <сайт>. Урон и усиление проверьте и задайте сами", при кубах в описании часть "В описании есть кубы урона: 8d6". Урон и усиление заполнены только у dnd5eapi.co и Open5e v2.
- Ничего не сохраняется само, заклинание появляется после "Добавить".
- Что проверить на устройстве, перечислить в отчёте: все сайты (dnd.su, next.dnd.su, 5e14.dnd.su, Open5e, dnd5eapi.co) на Android и iOS, урон и усиление у Огненного шара, Волшебной стрелы, Огненной стрелы и Лечения ран, "Вставить" из буфера на iOS (системная плашка), нет сети.

**Строки и словарь:** префикс `me_spells_import_` в `values` и `values-en`. Тексты из `DESIGN.md` задача 17 и макета, "ссылка" (не "линк"), нажмите / удерживайте. Названия сайтов не склоняются, подпись "Страницу загружает ваше устройство, напрямую с сайта".

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены (раздел 16, строки про заклинания), `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.
