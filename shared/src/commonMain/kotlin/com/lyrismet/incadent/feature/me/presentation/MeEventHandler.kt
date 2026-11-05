package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyStatRanges
import com.lyrismet.incadent.feature.codex.domain.model.SheetRanges
import com.lyrismet.incadent.feature.codex.presentation.party.PlayerCharacterPicker
import com.lyrismet.incadent.feature.me.domain.HitPointMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TEXT_SAVE_DELAY_MILLIS = 400L

/** what a press needs to know about the screen it came from */
internal class MeEventScope(
    val member: PartyMember?,
    val party: List<PartyMember>,
    val character: MeCharacter?,
    val readOnly: Boolean,
    val records: CodexRecords,
    val liveSessionId: Long?,
)

/** reacts to every [MeEvent] - UI-only ones change the remembered state, the rest become writes */
@Suppress("LongParameterList")
internal class MeEventHandler(
    private val currentScope: () -> MeEventScope,
    private val ui: MeUiState,
    private val actions: MeActions,
    private val counters: MeCounterActions,
    private val picker: PlayerCharacterPicker,
    private val things: MeThingsHandler,
    private val scope: CoroutineScope,
) {
    fun handle(event: MeEvent) {
        val data = currentScope()
        // an archived campaign is read only, the screen drops what would change it
        if (data.readOnly && event.writes()) return
        val member = data.member
        val character = data.character
        when (event) {
            is MeEvent.Picked -> picker.pick(scope, data.party, event.id)
            is MeEvent.TabSelected -> ui.tab = event.tab
            MeEvent.PopoverClosed -> ui.popover = null
            MeEvent.MenuToggled -> ui.menuOpen = !ui.menuOpen
            else -> if (member != null && character != null) handleCharacter(event, member, character, data)
        }
    }

    private fun handleCharacter(
        event: MeEvent,
        member: PartyMember,
        character: MeCharacter,
        data: MeEventScope,
    ) {
        when (event) {
            is MeEvent.Hp -> hitPoints(event.event, member, character)
            is MeEvent.DeathSaves -> deathSaves(event.event, member)
            is MeEvent.TileTapped -> tile(event.tile, character)
            MeEvent.AdjustmentRequested -> adjustment(character)
            is MeEvent.Sheet -> sheet(event.event, member, character)
            is MeEvent.Overlay -> overlay(event.event, member)
            is MeEvent.Counters -> MeCounterHandler(ui, actions, counters).handle(event.event, member, character)
            is MeEvent.Menu -> menu(event.action, member, character)
            is MeEvent.Rest,
            is MeEvent.Wallet,
            is MeEvent.Items,
            is MeEvent.ItemStepped,
            is MeEvent.ItemDeleted,
            -> things.handle(event, member, data)
            else -> Unit
        }
    }

    private fun hitPoints(
        event: HitPointsEvent,
        member: PartyMember,
        character: MeCharacter,
    ) {
        val pad = ui.hitPad
        when (event) {
            HitPointsEvent.Opened -> ui.hitPad = HitPad()
            is HitPointsEvent.ModeChanged -> ui.hitPad = HitPad(mode = event.mode)
            is HitPointsEvent.Digit -> ui.hitPad = pad?.copy(expression = pad.expression.digit(event.digit))
            HitPointsEvent.Backspace -> ui.hitPad = pad?.copy(expression = pad.expression.backspace())
            HitPointsEvent.Cleared -> ui.hitPad = pad?.copy(expression = pad.expression.clear())
            HitPointsEvent.Closed -> ui.hitPad = null
            HitPointsEvent.Applied -> {
                if (pad != null && pad.expression.raw.isNotEmpty()) {
                    actions.hit(member.id, pad.mode, pad.expression.value())
                }
                ui.hitPad = null
            }
        }
        if (event == HitPointsEvent.Opened) ui.popover = null
        check(character.id == member.id)
    }

    private fun deathSaves(
        event: DeathSavesEvent,
        member: PartyMember,
    ) {
        val id = member.id
        when (event) {
            is DeathSavesEvent.SuccessDot -> actions.successDot(id, event.index)
            is DeathSavesEvent.FailureDot -> actions.failureDot(id, event.index)
            DeathSavesEvent.SuccessAdded ->
                actions.vitals(
                    id,
                ) { it.copy(deathSavesSuccess = (it.deathSavesSuccess + 1).coerceIn(SheetRanges.deathSaves)) }
            DeathSavesEvent.FailureAdded ->
                actions.vitals(
                    id,
                ) { it.copy(deathSavesFailure = (it.deathSavesFailure + 1).coerceIn(SheetRanges.deathSaves)) }
            DeathSavesEvent.FailureRemoved ->
                actions.vitals(
                    id,
                ) { it.copy(deathSavesFailure = (it.deathSavesFailure - 1).coerceIn(SheetRanges.deathSaves)) }
            DeathSavesEvent.MarkedFallen -> actions.markFallen(member)
        }
    }

    private fun tile(
        tile: MeTile,
        character: MeCharacter,
    ) {
        when (tile) {
            MeTile.ARMOR_CLASS ->
                ui.edit =
                    MeOverlay.ArmorClass(
                        character.numbers.armorClass ?: DEFAULT_ARMOR_CLASS,
                        character.vitals.armorClassFrom,
                    )
            MeTile.SPEED -> ui.edit = MeOverlay.Speed(character.numbers.speed ?: DEFAULT_SPEED)
            else -> ui.popover = if (ui.popover == tile) null else tile
        }
    }

    private fun adjustment(character: MeCharacter) {
        val tile = ui.popover ?: return
        val value =
            if (tile ==
                MeTile.INITIATIVE
            ) {
                character.breakdown.initiativeAdjustment
            } else {
                character.breakdown.passiveAdjustment
            }
        ui.popover = null
        ui.edit = MeOverlay.Adjustment(tile, value)
    }

    private fun sheet(
        event: SheetEvent,
        member: PartyMember,
        character: MeCharacter,
    ) {
        val id = member.id
        val level = member.level ?: 1
        val source = character.sheet.source
        when (event) {
            is SheetEvent.AbilityTapped -> ui.edit = MeOverlay.AbilityEdit(event.ability, level, source)
            is SheetEvent.SaveToggled -> actions.toggleSave(id, event.ability)
            is SheetEvent.SkillMarkTapped -> actions.nextMark(id, event.skill)
            is SheetEvent.SkillTapped -> ui.edit = MeOverlay.SkillEdit(event.skill, level, source)
            SheetEvent.InspirationToggled -> actions.sheet(id) { it.copy(inspiration = !it.inspiration) }
            SheetEvent.SearchToggled -> {
                ui.searchOpen = !ui.searchOpen
                if (!ui.searchOpen) ui.query = ""
            }
            is SheetEvent.QueryChanged -> ui.query = event.query
            is SheetEvent.TextChanged -> typeText(id, event.text, event.value)
            SheetEvent.FillRequested -> ui.edit = MeOverlay.Fill(fillDraft(member, character))
            SheetEvent.AboutTapped -> ui.edit = MeOverlay.EditCharacter(characterDraft(member, source))
        }
    }

    // saved a moment after the last key, the draft is shown meanwhile
    private fun typeText(
        id: Long,
        text: MeText,
        value: String,
    ) {
        ui.textDrafts[text] = value
        ui.textJobs[text]?.cancel()
        ui.textJobs[text] =
            scope.launch {
                delay(TEXT_SAVE_DELAY_MILLIS)
                actions.sheetNow(id) { it.withText(text, value) }
                ui.textDrafts.remove(text)
            }
    }

    private fun overlay(
        event: OverlayEvent,
        member: PartyMember,
    ) {
        val open = ui.edit
        when {
            event == OverlayEvent.Closed -> ui.edit = null
            open == null -> Unit
            event == OverlayEvent.Applied -> {
                // a resource without a name keeps its sheet open
                if (open is MeOverlay.Resource && open.name.isBlank()) return
                commit(member, open)
                ui.edit = null
            }
            else -> ui.edit = open.reduce(event)
        }
    }

    private fun commit(
        member: PartyMember,
        open: MeOverlay,
    ) {
        when (open) {
            is MeOverlay.Resource -> counters.save(member.id, open)
            is MeOverlay.Slots -> counters.saveSlots(member.id, open.maxima)
            else -> actions.commit(member, open)
        }
    }

    private fun menu(
        action: MenuAction,
        member: PartyMember,
        character: MeCharacter,
    ) {
        ui.menuOpen = false
        when (action) {
            MenuAction.EDIT -> ui.edit = MeOverlay.EditCharacter(characterDraft(member, character.sheet.source))
            MenuAction.FILL -> ui.edit = MeOverlay.Fill(fillDraft(member, character))
            MenuAction.TOGGLE_SPELLS -> counters.setSpellsHidden(member, !character.spellsHidden)
            MenuAction.NOT_MINE -> actions.releaseMark(member)
        }
    }
}

private const val DEFAULT_ARMOR_CLASS = 10
private const val DEFAULT_SPEED = 30

private fun CharacterSheet.withText(
    text: MeText,
    value: String,
): CharacterSheet =
    when (text) {
        MeText.LANGUAGES -> copy(languages = value)
        MeText.FEATURES -> copy(features = value)
        MeText.PERSONALITY -> copy(personality = value)
        MeText.NOTES -> copy(notes = value)
    }

private fun fillDraft(
    member: PartyMember,
    character: MeCharacter,
): FillDraft {
    val sheet = character.sheet.source
    return FillDraft(
        characterClass = member.characterClass,
        subclass = sheet.subclass,
        level = (member.level ?: 1).coerceIn(PartyStatRanges.level),
        abilities = FillDraft().abilities + sheet.abilities,
        saves = sheet.saves,
        marks = sheet.skillMarks,
        hpMax = member.hpMax,
        armorClass = character.numbers.armorClass ?: DEFAULT_ARMOR_CLASS,
        speed = sheet.speed ?: DEFAULT_SPEED,
    )
}

private fun characterDraft(
    member: PartyMember,
    sheet: CharacterSheet,
): CharacterDraft =
    CharacterDraft(
        name = member.name,
        characterClass = member.characterClass,
        subclass = sheet.subclass,
        level = (member.level ?: 1).coerceIn(PartyStatRanges.level),
        race = member.race,
        background = sheet.background,
        alignment = sheet.alignment,
        playerName = member.playerName,
    )

/** true for the presses that change stored data, the others only move what the screen shows */
private fun MeEvent.writes(): Boolean =
    when (this) {
        is MeEvent.TabSelected, MeEvent.PopoverClosed, MeEvent.MenuToggled -> false
        is MeEvent.TileTapped -> tile == MeTile.ARMOR_CLASS || tile == MeTile.SPEED
        is MeEvent.Hp -> event == HitPointsEvent.Opened || event == HitPointsEvent.Applied
        is MeEvent.Sheet ->
            when (event) {
                SheetEvent.SearchToggled, is SheetEvent.QueryChanged -> false
                else -> true
            }
        is MeEvent.Overlay -> event != OverlayEvent.Closed
        is MeEvent.Counters -> event !is CounterEvent.SlotLevelToggled
        else -> true
    }

internal val HIT_POINT_MODES = listOf(HitPointMode.DAMAGE, HitPointMode.HEAL, HitPointMode.TEMP)
