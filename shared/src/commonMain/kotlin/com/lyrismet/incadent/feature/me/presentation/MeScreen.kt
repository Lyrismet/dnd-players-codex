package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.CharacterVitals
import com.lyrismet.incadent.feature.codex.domain.model.FifthEditionRules
import com.lyrismet.incadent.feature.codex.domain.model.Proficiency
import com.lyrismet.incadent.feature.codex.domain.model.ResourceTemplate
import com.lyrismet.incadent.feature.codex.domain.model.Skill
import com.lyrismet.incadent.feature.me.domain.AbilityStrip
import com.lyrismet.incadent.feature.me.domain.HeaderNumbers
import com.lyrismet.incadent.feature.me.domain.HitPointMode
import com.lyrismet.incadent.feature.me.domain.SlotColumn
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

internal const val DEFAULT_SCORE = 10

@Serializable
data object MeScreen : Screen

data class MeState(
    val content: MeContent,
    val eventSink: (MeEvent) -> Unit = {},
) : CircuitUiState

sealed interface MeContent {
    data object Loading : MeContent

    /** nobody of the party carries the "me" mark yet */
    data class Pick(
        val options: List<MePickOption>,
        val readOnly: Boolean,
    ) : MeContent

    data class Character(
        val character: MeCharacter,
    ) : MeContent
}

data class MePickOption(
    val id: Long,
    val name: String,
    val characterClass: String,
    val level: Int?,
)

/** the tabs under the header, the sheet is always there, spells only for a caster */
enum class MeTab { SHEET, SPELLS, RESOURCES, ITEMS }

/** the tiles under the hit points that explain themselves in a card on a press */
enum class MeTile { ARMOR_CLASS, INITIATIVE, SPEED, PROFICIENCY, PASSIVE_PERCEPTION }

data class MeCharacter(
    val id: Long,
    val name: String,
    val portrait: Photo?,
    val characterClass: String,
    val level: Int?,
    val race: String,
    val fallen: Boolean,
    val vitals: CharacterVitals,
    val numbers: HeaderNumbers,
    val tab: MeTab,
    val tabs: List<MeTab>,
    val spellsHidden: Boolean,
    val sheet: MeSheetState,
    val spells: MeSpellsState,
    val resources: List<CharacterResource>,
    val breakdown: MeBreakdown,
    val popover: MeTile?,
    val overlay: MeOverlay?,
    val menuOpen: Boolean,
    val readOnly: Boolean,
)

/** the spell tab: the chosen ability with what it gives and the slot columns */
data class MeSpellsState(
    val ability: Ability?,
    val saveDc: Int?,
    val attack: Int?,
    val columns: List<SlotColumn>,
    // the level whose cells are open under its tile
    val openLevel: Int?,
)

/** what the explanation cards of the tiles list */
data class MeBreakdown(
    val level: Int,
    val dexterityBonus: Int?,
    val wisdomBonus: Int?,
    val perceptionProficiency: Proficiency,
    val initiativeAdjustment: Int,
    val passiveAdjustment: Int,
)

data class MeSheetState(
    // what the edit sheets start from
    val source: CharacterSheet,
    val filled: Boolean,
    val strips: List<AbilityStrip>,
    val searchOpen: Boolean,
    val query: String,
    val inspiration: Boolean,
    val languages: String,
    val features: String,
    val subclass: String,
    val background: String,
    val alignment: String,
    val personality: String,
)

/** the free texts of the sheet that are typed in place */
enum class MeText { LANGUAGES, FEATURES, PERSONALITY }

sealed interface MeOverlay {
    /** the hit point keypad, [result] is what the keys typed, [before] and [after] the hit points now and then */
    data class HitPoints(
        val mode: HitPointMode,
        val result: String,
        val before: Int,
        val after: Int,
        val canApply: Boolean,
    ) : MeOverlay

    /** [sheet] is the working copy the edit changes, every number shown comes from it through the rules */
    data class AbilityEdit(
        val ability: Ability,
        val level: Int,
        val sheet: CharacterSheet,
    ) : MeOverlay {
        val score: Int get() = sheet.abilities[ability] ?: DEFAULT_SCORE
        val save: Boolean get() = ability in sheet.saves
        val bonus: Int get() = FifthEditionRules.abilityBonus(score)
        val saveBonus: Int get() = FifthEditionRules.saveBonus(sheet, ability, level) ?: bonus
    }

    data class SkillEdit(
        val skill: Skill,
        val level: Int,
        val sheet: CharacterSheet,
    ) : MeOverlay {
        val proficiency: Proficiency get() = sheet.mark(skill)
        val adjustment: Int get() = sheet.adjustment(skill)
        val bonus: Int? get() = FifthEditionRules.skillBonus(sheet, skill, level)
    }

    data class ArmorClass(
        val value: Int,
        val from: String,
    ) : MeOverlay

    data class Speed(
        val value: Int,
    ) : MeOverlay

    data class Adjustment(
        val tile: MeTile,
        val value: Int,
    ) : MeOverlay

    data class Fill(
        val draft: FillDraft,
    ) : MeOverlay

    data class EditCharacter(
        val draft: CharacterDraft,
    ) : MeOverlay

    /** the slots per level, 0 for none */
    data class Slots(
        val maxima: Map<Int, Int>,
    ) : MeOverlay

    data class SpellAbility(
        val selected: Ability?,
    ) : MeOverlay

    /** [id] is 0 for a resource not stored yet */
    data class Resource(
        val id: Long,
        val name: String,
        val max: Int,
        val current: Int,
        val position: Int,
        val originId: String,
    ) : MeOverlay

    data object Templates : MeOverlay
}

/** the four steps of the first fill, every number is what the paper sheet says */
data class FillDraft(
    val step: Int = 1,
    val characterClass: String = "",
    val subclass: String = "",
    val level: Int = 1,
    val abilities: Map<Ability, Int> = Ability.entries.associateWith { DEFAULT_SCORE },
    val saves: Set<Ability> = emptySet(),
    val marks: Map<Skill, Proficiency> = emptyMap(),
    val hpMax: Int? = null,
    val armorClass: Int = 10,
    val speed: Int = 30,
    // the steps the player left with "skip", they write nothing
    val skipped: Set<Int> = emptySet(),
) {
    companion object {
        const val STEP_CLASS = 1
        const val STEP_ABILITIES = 2
        const val STEP_SKILLS = 3
        const val STEP_NUMBERS = 4
        const val LAST_STEP = STEP_NUMBERS
    }
}

data class CharacterDraft(
    val name: String,
    val characterClass: String,
    val subclass: String,
    val level: Int,
    val race: String,
    val background: String,
    val alignment: String,
    val playerName: String,
)

sealed interface MeEvent : CircuitUiEvent {
    data class Picked(
        val id: Long,
    ) : MeEvent

    data class TabSelected(
        val tab: MeTab,
    ) : MeEvent

    data class Hp(
        val event: HitPointsEvent,
    ) : MeEvent

    data class DeathSaves(
        val event: DeathSavesEvent,
    ) : MeEvent

    data class TileTapped(
        val tile: MeTile,
    ) : MeEvent

    data object PopoverClosed : MeEvent

    data object AdjustmentRequested : MeEvent

    data class Sheet(
        val event: SheetEvent,
    ) : MeEvent

    data class Overlay(
        val event: OverlayEvent,
    ) : MeEvent

    data class Counters(
        val event: CounterEvent,
    ) : MeEvent

    data object MenuToggled : MeEvent

    data class Menu(
        val action: MenuAction,
    ) : MeEvent
}

sealed interface HitPointsEvent {
    data object Opened : HitPointsEvent

    data class ModeChanged(
        val mode: HitPointMode,
    ) : HitPointsEvent

    data class Digit(
        val digit: Int,
    ) : HitPointsEvent

    data object Backspace : HitPointsEvent

    data object Cleared : HitPointsEvent

    data object Applied : HitPointsEvent

    data object Closed : HitPointsEvent
}

sealed interface DeathSavesEvent {
    data class SuccessDot(
        val index: Int,
    ) : DeathSavesEvent

    data class FailureDot(
        val index: Int,
    ) : DeathSavesEvent

    data object SuccessAdded : DeathSavesEvent

    data object FailureAdded : DeathSavesEvent

    data object FailureRemoved : DeathSavesEvent

    data object MarkedFallen : DeathSavesEvent
}

sealed interface SheetEvent {
    data class AbilityTapped(
        val ability: Ability,
    ) : SheetEvent

    data class SaveToggled(
        val ability: Ability,
    ) : SheetEvent

    data class SkillMarkTapped(
        val skill: Skill,
    ) : SheetEvent

    data class SkillTapped(
        val skill: Skill,
    ) : SheetEvent

    data object InspirationToggled : SheetEvent

    data object SearchToggled : SheetEvent

    data class QueryChanged(
        val query: String,
    ) : SheetEvent

    data class TextChanged(
        val text: MeText,
        val value: String,
    ) : SheetEvent

    data object FillRequested : SheetEvent

    data object AboutTapped : SheetEvent
}

sealed interface CounterEvent {
    data class SlotLevelToggled(
        val level: Int,
    ) : CounterEvent

    /** a press on the cell at [index] of the counter [id], a slot level or a resource */
    data class CellPressed(
        val id: Long,
        val index: Int,
    ) : CounterEvent

    data class PipHeld(
        val id: Long,
    ) : CounterEvent

    data object ResourceAdded : CounterEvent

    data object TemplatesRequested : CounterEvent

    data class TemplateChosen(
        val template: ResourceTemplate,
        val name: String,
    ) : CounterEvent

    data object ResourceDeleted : CounterEvent

    data object SlotsRequested : CounterEvent

    data object SpellAbilityRequested : CounterEvent

    data object SpellsHidden : CounterEvent

    data class SpellAbilityChosen(
        val ability: Ability?,
    ) : CounterEvent
}

sealed interface OverlayEvent {
    data object Closed : OverlayEvent

    data object Applied : OverlayEvent

    data class NumberStepped(
        val delta: Int,
    ) : OverlayEvent

    data class TextChanged(
        val field: OverlayField,
        val value: String,
    ) : OverlayEvent

    data object SaveToggled : OverlayEvent

    data class ProficiencyChosen(
        val proficiency: Proficiency,
    ) : OverlayEvent

    data object AdjustmentCleared : OverlayEvent

    data class FillAbilityStepped(
        val ability: Ability,
        val delta: Int,
    ) : OverlayEvent

    data class FillSaveToggled(
        val ability: Ability,
    ) : OverlayEvent

    data class FillSkillTapped(
        val skill: Skill,
    ) : OverlayEvent

    data class FillHpMaxChanged(
        val value: Int?,
    ) : OverlayEvent

    data class FillArmorClassStepped(
        val delta: Int,
    ) : OverlayEvent

    data class FillSpeedStepped(
        val delta: Int,
    ) : OverlayEvent

    data object FillNext : OverlayEvent

    data object FillSkip : OverlayEvent

    data object FillBack : OverlayEvent

    data class SlotStepped(
        val level: Int,
        val delta: Int,
    ) : OverlayEvent
}

/** the typed fields of the sheets, the number sheets use [OverlayEvent.NumberStepped] */
enum class OverlayField {
    ARMOR_CLASS_FROM,
    CLASS,
    SUBCLASS,
    NAME,
    RACE,
    BACKGROUND,
    ALIGNMENT,
    PLAYER_NAME,
    RESOURCE_NAME,
}

enum class MenuAction { EDIT, FILL, TOGGLE_SPELLS, NOT_MINE }
