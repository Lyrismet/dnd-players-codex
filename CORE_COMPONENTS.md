# CORE_COMPONENTS.md

Catalog of everything already built in `shared/src/commonMain/kotlin/com/lyrismet/dndcodex/core/designsystem/`.

**Check this file before writing any new visual block** (a card, a pill, a segmented switch, an icon slot, a divider...).
If something here already covers the shape you need, use it with different parameters instead of hand-rolling
`clip`/`background`/`border` again. If two or more features end up needing a visual shape that isn't here yet,
that's the signal to add it here, not to copy-paste it a third time (see CLAUDE.md's "Recurring review feedback").

## Tokens

| Name | File | What it is |
|---|---|---|
| `AppPalette` | `AppPalette.kt` | raw color constants (`Gold`, `Surface`, `BorderSubtle`, `TextPrimary`, ...). Never hardcode a hex color in feature code - reference a palette value or a `StatusColor`/`GoldCursorBrush` below instead. |
| `StatusColor` | `StatusColor.kt` | `(foreground, background, border)` triple for one status. `NpcStatus.toStatusColor()`, `QuestStatus.toStatusColor()`, `LocationMentionColor` build these from a domain status. |
| `GoldCursorBrush` | `Brushes.kt` | the gold `SolidColor` cursor brush every hand-rolled `BasicTextField` uses - don't inline `SolidColor(AppPalette.Gold)` again. |
| `Theme.kt` / `Type.kt` | - | Compose `MaterialTheme` wiring and the type scale. Reference `MaterialTheme.typography.*`, never a raw `TextStyle`. |

## Surfaces

| Component | File | Use for | Key params |
|---|---|---|---|
| `Modifier.appCard()` | `component/AppCard.kt` | the clip+tint+optional-border+optional-click "surface card" chrome - codex cards, session rows, fact grids, composer input, popovers. A **Modifier extension**, not a composable: apply it to whatever `Row`/`Column` you already have, then add your own padding after it. | `shape`, `background`, `border` (nullable - `null` means no border), `borderWidth`, `onClick` (nullable) |
| `IconBadge` | `component/IconBadge.kt` | fixed-size tinted slot with centered content - avatar initials, entity emblems, circular header/dialog action buttons (edit, close, "@"). | `size`, `shape` (`CircleShape` by default), `background`, `border` (nullable), `onClick` (nullable), `content` |
| `AppDialog` | `component/AppDialog.kt` | the generic centered-dialog shell (rounded, bordered, padded `Column`). `ConfirmationDialog` is built on this. | `borderColor`, `content` |
| `AppBottomSheet` | `component/AppBottomSheet.kt` | the modal bottom sheet shell (26dp top corners, gold edge, drag handle) used by every entity sheet and the entry form. Also hosts `SheetCloseButton` (built on `IconBadge`). | `onDismissRequest`, `content` |

## Buttons, badges, chips

| Component | File | Use for | Key params |
|---|---|---|---|
| `HeaderActionButton` | `component/HeaderActionButton.kt` | small tinted rounded-rect button in a screen header ("+ Сессия", "Завершить"). | `text`, `onClick`, `foreground`/`background`/`border` |
| `FormPrimaryButton` | `component/FormFields.kt` | full-width gold 52dp CTA at the bottom of a form/sheet. | `text`, `onClick`, `enabled` |
| `TagChip` | `component/TagChip.kt` | small rounded label pill - generic tags, badges. | `text`, `foreground`/`background`/`border` |
| `StatusBadge` | `component/StatusBadge.kt` | `TagChip` pre-wired to a `StatusColor` - NPC/quest status badges. | `text`, `color: StatusColor` |
| `MentionChip` | `component/MentionChip.kt` | the colored "●NPC name"/"▲Location"/"◆Quest" pill used inline in session notes and entity sheets. Also owns `MentionGlyph` (the canonical `●`/`▲`/`◆` symbols - reference `MentionGlyph.X.symbol`, never retype the glyph). | `item: MentionChipItem`, `onClick`, `fontSize`, `glyphSize`, `glyphGap`, `lineHeightFactor` |
| `FormChipPicker` | `component/FormFields.kt` | the pill-chip picker for a form field (status, relationship, "given by", "where") - every chip-type field uses this, never a segmented row. A selected option takes the status color it carries in `FormChipOption.selectedColor`, gold otherwise. | `label`, `options: List<FormChipOption<T>>`, `onClick` |

## Pickers / segmented controls

| Component | File | Use for | Key params |
|---|---|---|---|
| `SegmentedControl<T>` | `component/SegmentedControl.kt` | the rounded-rect tab-switcher chrome - codex filter tabs, the entry-type switch, the entity status picker. One bordered container of equal-width clickable items; background/border per item are callbacks so a status-colored picker and a flat gold-accent picker both fit. | `items`, `onSelected`, `itemBackground`, `itemBorder` (nullable), `containerBackground`/`containerBorder`, `itemHeight`, `itemSpacing`, `itemContent` |

Don't hand-roll a new `Row { items.forEach { Box(...).clickable{} } }` tab switcher - it's the third time that exact
shape got copy-pasted that triggered this doc's `SegmentedControl` extraction in the first place.

## Dividers / dots / misc primitives

| Component | File | Use for | Key params |
|---|---|---|---|
| `AppDivider` | `component/Divider.kt` | the plain 1dp rule between a header/footer and the content beside it. Full-width by default. | `color`, `thickness`, `modifier` (pass your own `weight`/`padding` modifier to override the full-width default) |
| `HeaderDivider` | `component/HeaderDivider.kt` | the ornamental short-gold-line + diamond + long-border-line rule under a screen title (built out of `AppDivider` + `Spacer`s). | - |
| `Dot` | `component/Dot.kt` | a plain solid circle at a given size/color - filter-chip status dots, the core of `GlowingDot` below, and an outlined circle (pass `color = Color.Transparent` with `borderColor`) like the codex search icon. | `size`, `color`, `borderColor` (optional), `borderWidth` (1dp), `content` (optional, for nesting) |
| `GlowingDot` | `component/GlowingDot.kt` | a `Dot` with a concentric flat-color glow ring - the "live session" indicator. | `dotSize`, `dotColor`, `glowColor`, `ringWidth` |
| `NumberLabel` | `component/NumberLabel.kt` | centered fixed-width roman-numeral index in front of a list row (session number, note number) - the font shrinks as the label gets longer, per `core/format`'s `NumberSizeLadder`. | `text`, `width` (44dp), `color`, `sizeLadder`, `lineHeightFactor` |
| `SectionOverline` | `component/SectionOverline.kt` | tracked all-caps single-line eyebrow label ("АРХИВ", "БАЗА ЗНАНИЙ"). The design uses 0.18em for page eyebrows, 0.16em for list sections and 0.14em for sheet and form labels, so pass the tracking. | `text`, `color`, `letterSpacing`, `fontSize`, `trailingContent` |
| `ScreenHeader` | `component/ScreenHeader.kt` | overline + serif title + optional `HeaderDivider` - top of every top-level screen. | `overline`, `title`, `overlineTrailingContent` |
| `EmptyStatePlaceholder` | `component/EmptyStatePlaceholder.kt` | centered secondary-text message for an empty list/search/loading state. | `text` |
| `OrnamentedEmptyState` | `component/OrnamentedEmptyState.kt` | the "blank page" state of a screen meant to be filled: gold diamond, serif title and a hint (empty session feed). | `title`, `hint` |
| `LabelValueGrid` | `component/LabelValueGrid.kt` | two-column grid (`auto 1fr`): children alternate label, value, the label column is as wide as the longest label and rows are top-aligned (entity sheet facts). | `rowSpacing`, `columnSpacing`, `content` |
| `SwipeToDeleteRow` | `component/SwipeToDeleteRow.kt` | swipe-left-to-reveal-delete row chrome. Never deletes anything itself - caller drives the real delete, which is usually immediate (see `UndoToast`) rather than a confirm dialog. | `onDeleteRequested`, `deleteContentDescription`, `content` |
| `UndoToast` | `component/UndoToast.kt` | the "X deleted · Отменить" bar that floats above the bottom tab bar for 5s after an optimistic delete (sessions, entries, codex entities) - driven by `core/undo/UndoController`, not local screen state. | `action: UndoAction?`, `onUndo` |
| `ConfirmationDialog` | `component/ConfirmationDialog.kt` | "are you sure?" dialog for a destructive action that has no undo (session/entry/codex deletes use `UndoToast` instead), built on `AppDialog` + `IconBadge`. | `title`, `text`, `onConfirm`, `onDismiss` |
| `Modifier.dismissKeyboardOnTap()` | `component/DismissKeyboardOnTap.kt` | clears focus/hides keyboard on an unconsumed tap - applied once at the app root. | - |

## Forms

| Component | File | Use for |
|---|---|---|
| `FormTextField` | `component/FormFields.kt` | single-line labeled text input matching the create/edit form style. |
| `FormTextArea` | `component/FormFields.kt` | multi-line variant of `FormTextField`, 3-line minimum height. |
| `FormChipPicker` | `component/FormFields.kt` | see "Buttons, badges, chips" above. |
| `FormPrimaryButton` | `component/FormFields.kt` | see "Buttons, badges, chips" above. |

Every field in `CodexEntryFormUi.kt` is built from these four - if a new form needs a field type that doesn't fit,
add it here rather than reaching for a raw Material `TextField` in feature code.

## Navigation / icons

| Component | File | Use for |
|---|---|---|
| `BottomTabBar` / `BottomTabBarItem` | `component/BottomTabBar.kt` / `BottomTabBarItem.kt` | the persistent 4-column bottom nav bar. |
| `AppIcons` | `component/icons/AppIcons.kt` | glyphs reused across 2+ features: `Edit` is the design's quill, `Delete` the stock Material bin. A single-use icon doesn't need an entry here - only add one once a second feature wants the same glyph. |
| `TabIconSlot` + `CodexTabIcon`/`CombatTabIcon`/`SessionsTabIcon`/`SettingsTabIcon` | `component/icons/` | the 4 hand-drawn bottom-nav glyphs, all sharing one fixed-height alignment slot. |

## Entity sheet (built on the above, not a primitive itself)

`component/EntitySummarySheetBody.kt`'s `EntitySummarySheetContent` is the one entity-detail sheet shared by
`codex`, `sessionlist` and `sessiondetail` - it's composed entirely from `IconBadge`, `AppCard`, `SegmentedControl`,
`NumberLabel`, `MentionChip` and `SectionOverline` above. If you need a new fact/relation/status block inside an
entity sheet, it almost certainly belongs as a case inside this file, not a new top-level component.

## Not a visual component - don't look here for UI shapes

`core/mention`, `core/format`, `core/entitysummary` hold **logic** (parsing, formatting, cross-entity mapping)
reused across 3+ features, not visual building blocks - see the root `CLAUDE.md` project overview for the
`core/<name>` vs `core/designsystem` split. `core/entitysummary`'s `EntitySheetInteractions` is the shared
tap/dismiss/status-update event-handling every presenter delegates to; it has no composable of its own.

## When nothing here fits

Adding a genuinely new visual primitive is fine - this file just exists so "genuinely new" is a real judgment call,
not a guess. Add the new component under `core/designsystem/component/`, then add a row to this file in the same PR.
