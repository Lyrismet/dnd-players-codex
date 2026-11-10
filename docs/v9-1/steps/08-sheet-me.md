# Шаг 8: Лист "Я": надето, щиты, черты, портрет

- **Кому:** агент-разработчик KMP (лист "Я", кодекс). Вид и жесты на устройстве проверяет владелец.
- **Зависит от:** шаг 4.
- **Миграции:** `19.sqm`, `20.sqm`, `21.sqm` (номера сверить с папкой миграций).

Шаг состоит из частей, делать по порядку, после каждой сборка, `ktlintFormat`, `detekt`. Один PR на весь шаг. Если часть оказалась слишком большой, остановиться и записать это в отчёт.

## Часть 1. "Надето" и щиты в листе "Я" и записи отряда
**Цель:** Флаг "Надето" у предметов с группой в списке и блок щитов "Особенности" в листе "Я" и записи члена отряда.

**Читать (только это):** `DEV.md`: эпик I (I.1, I.2). Вид: `Design Discussion 3`, раунд 20 (20a, 20b). `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/me/presentation/MeItemsTab.kt, MeItemsState.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/codex/presentation/item/`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/codex/domain/model/Item.kt, PartyMember.kt`
- `shared/src/commonMain/sqldelight/com/lyrismet/incadent/db/Item.sq, PartyMember.sq`

**Миграция и тесты:** `19.sqm`: `item.worn INTEGER NOT NULL DEFAULT 0`; хранение щитов члена отряда по образцу `npc.vulnerabilities`. Тест миграции; экспорт и импорт несут флаг.

**Не трогать:** блок "Особенности" записи NPC (шаг 4, часть 2): только переиспользовать.

**Приёмка:**
- Переключатель "Надето" в листе предмета, группа "Надето" над "В рюкзаке".
- Щиты в листе "Я" и записи отряда одна запись, нажатие по кругу.

**Строки и словарь:** `item_worn_*`, `me_features_*`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.

## Часть 2. Черты и портрет в листе "Я"
**Цель:** Список черт на вкладке "Лист" и портрет в шапке листа тем же компонентом, что в кодексе.

**Читать (только это):** `DEV.md`: эпик I (I.3, I.4). Вид: `Design Discussion 3`, раунд 20 (20c, 20d). `docs/CORE_COMPONENTS.md` (искать готовое до нового блока).

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/feature/me/presentation/MeSheetTab.kt, MeHeader.kt, MeTextBlocks.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/core/designsystem/component/AppPhotoSlot.kt, AppPhotoViewer.kt, AppPhotoCropper.kt`
- `shared/src/commonMain/sqldelight/com/lyrismet/incadent/db/CharacterSheet.sq`

**Миграция и тесты:** `20.sqm`: `character_feat` (вместе со щитами члена отряда, если не вошли в шаг 8, часть 1). Тест миграции и репозитория.

**Не трогать:** блок "Особенности", ресурсы, компонент портрета (только использовать).

**Приёмка:**
- Список черт: название, откуда, что даёт; правка и удаление с тостом.
- Портрет в листе открывает выбор, кадрирование, просмотр с "Заменить / Кадрировать / Убрать".

**Строки и словарь:** `me_feats_*`, `me_portrait_*`. Словарь `CLAUDE.md`, раздел Localization.

**Готово, когда:** `docs/RULES.md` "Готовность задачи", шаг отмечен `[x]` в `README.md`, пункты `docs/FEATURES.md` отмечены, `./gradlew build -x :shared:linkReleaseFrameworkIosArm64 -x :shared:linkReleaseFrameworkIosSimulatorArm64`, `ktlintFormat`, `detekt` зелёные. Что проверить на устройстве, перечислить в отчёте.

## Часть 3. Повторное кадрирование от исходного кадра

**Цель:** хранить рядом с обрезанным кадром сжатый исходный кадр до обрезки и кадрировать заново от него, а не от уже обрезанного (например, 250×230, затем 190×190, снова от 250×230).

**Читать (только это):** `DEV.md`: эпик I (I.5). `docs/CORE_COMPONENTS.md`.

**Код (где работать):** пути от корня репозитория.
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/core/portrait/CropGeometry.kt`, `PortraitCodec.kt`, `ImageCompressor.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/core/designsystem/component/AppPhotoCropper.kt`, `AppPhotoSlot.kt`
- `shared/src/commonMain/kotlin/com/lyrismet/incadent/domain/model/Photo.kt` и таблицы с `photo`: `party_member`, `npc`, `item` и другие

**Миграция и тесты:** `21.sqm`: `photo_source BLOB` (NULL у старых записей). Тест миграции на старой версии. Тесты: кадрирование от исходного, запись без исходного (как сейчас), список запросов не тянет исходный кадр.

**Не трогать:** показ (сжатые кадры 50/90 КБ и миниатюры), экспорт (исходный кадр в файл не идёт).

**Приёмка:**
- После кадрирования повторное открытие показывает исходный кадр, а не обрезанный.
- Старое фото без исходного кадрируется как раньше.
- Вес базы вырос не больше, чем на размер одного сжатого кадра на запись.

**Строки и словарь:** новых строк нет.
