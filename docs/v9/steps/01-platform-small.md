# Шаг 1. Мелочи платформы
- **Кому:** агент-разработчик KMP (Compose, Circuit, Metro, expect/actual, Android manifest). Ревью: QA-агент. Устройство: владелец (файл из Telegram, версия на Android и iOS).
- **Цель:** версия приложения видна на сплэше и в "Настройки → О приложении" из одного источника на обеих платформах; файл из Telegram открывается внутри приложения, а не в окне Telegram; атрибуция SRD с рабочими ссылками. Миграций нет.
- **Задачи:**
  - 8.1 версия: `DESIGN_V9.md:510-520`. `AppVersion` (`core/appinfo`, expect/actual), строка "Версия" на сплэше мелко внизу и в "О приложении", без номера сборки.
  - 1.5 `singleTop` -> `singleTask`: `DESIGN_V9.md:107-113`.
  - 1.6 SRD: `DESIGN_V9.md:114-123`. Заголовок "Материалы SRD 5.1", ссылки нажимаемые, текст выделяемый, тот же блок в форме вида.
- **Читать (только это):**

| Файл : строки | Зачем |
|---|---|
| `DESIGN_V9.md:27-40` | "Готовность задачи" |
| `DESIGN_V9.md:510-520`, `107-113`, `114-123` | три задачи шага |
| `DESIGN_V9.md:634` (тест версии), `677` (сценарий 12) | тест и приёмка |
| `Players Codex v9.dc.html:1637-1643` | сплэш: `hasSplash`, строка версии на 1642 (`bottom:46px`) |
| `Players Codex v9.dc.html:728-798` | экран настроек (`isSettings`); блок "О ПРИЛОЖЕНИИ" и "МАТЕРИАЛЫ SRD 5.1" в строке 793, строка обновления скрыта (`hasUpdate`) |
| `v9 Design System.dc.html:89` | блок "10 · ТЕКСТ" правила текста |
| `CLAUDE.md` | разделы "Dependency injection", "Localization" |

- **Код (где работать):**
  - `androidApp/src/main/AndroidManifest.xml:14` (`launchMode="singleTop"` -> `singleTask`), `onNewIntent` уже есть в `androidApp/src/main/kotlin/com/lyrismet/incadent/MainActivity.kt:45`, проверить, что повторное открытие файла идёт через него.
  - Версия Android: `androidApp/build.gradle.kts:16-17,49-51` (`versionName = "0.8.1"`, `versionCodeOf`). iOS: `iosApp/Configuration/Config.xcconfig:6-7` (`CURRENT_PROJECT_VERSION=1`, `MARKETING_VERSION=1.0`). Сейчас они расходятся.
  - Новое: `shared/src/commonMain/kotlin/com/lyrismet/incadent/core/appinfo/AppVersion.kt` (expect) + `androidMain`/`iosMain` actual; провайдер через `DataBindings` (граф Metro, не локатор).
  - Сплэш: `shared/.../incadent/Splash.kt` (114 строк, `Splash` на 48, тексты 69-74), вызывается из `App.kt:103`.
  - О приложении: `feature/settings/presentation/SettingsUi.kt:262-270` (`AboutSection`, `InfoRow` на 352), ссылки через `LocalUriHandler`.
  - Форма вида: `feature/codex/presentation/form/CodexSpeciesFormFields.kt:79` (`codex_srd_attribution`).
- **Миграция и тесты:** миграции нет. Тест форматирования версии в `commonTest/.../core/appinfo/` (приложение В, строка "Версия (8)": разбор "9.0.0"; разбор и сравнение нужны шагу 14, здесь достаточно формата).
- **Не трогать:** `UpdateStatus`, `UpdatePolicy`, `LatestVersionSource` (шаг 14), схему БД, `AppGraph` кроме одного биндинга версии.
- **Приёмка:** (1) одна и та же версия на сплэше и в настройках на Android и iOS; (2) холодный старт из Telegram и повторное открытие файла при открытом приложении не создают окно внутри Telegram; "Назад" возвращает в Telegram; (3) обе ссылки SRD открываются в браузере, текст копируется; (4) заголовок "Материалы SRD 5.1"; (5) в форме вида тот же блок. Сценарий 12 приложения Д.
- **Вопрос владельцу до начала:** какое значение версии ставить на обеих платформах (в макете "9.0.0" только пример, ориентир владельца 0.9.5).
- **Строки и словарь:** префикс `settings_about_` (версия, заголовок), `splash_version_format`. Менять `settings_about_srd_title` (`values:878`, `values-en:850`) и дополнить `settings_about_srd_value` (`values:879`, `values-en:851`) предложением про шаблоны ресурсов, не теряя "переведены и адаптированы" и "не поддерживает". Ссылки: `https://dnd.wizards.com/resources/systems-reference-document`, `https://creativecommons.org/licenses/by/4.0/legalcode`.
- **Готово, когда:** выполнены пункты "Готовность задачи" (`DESIGN_V9.md:27-39`), в приложении А шаг 1 отмечен `[x]`, в `FEATURES.md` отмечены пункты версии, `singleTask` и SRD.
