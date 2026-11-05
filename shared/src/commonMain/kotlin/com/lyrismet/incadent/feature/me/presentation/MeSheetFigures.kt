package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.CharacterVitals
import com.lyrismet.incadent.feature.codex.domain.model.FifthEditionRules
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Skill
import com.lyrismet.incadent.feature.me.domain.AbilityStrip
import com.lyrismet.incadent.feature.me.domain.featureResources
import com.lyrismet.incadent.feature.me.domain.headerNumbers
import com.lyrismet.incadent.feature.me.domain.slotColumns

/** the numbers of the header and the breakdowns shown in the popovers, from the stored sheet alone */
internal class MeSheetFigures(
    sheet: CharacterSheet,
    vitals: CharacterVitals,
    level: Int,
) {
    val numbers = headerNumbers(sheet, vitals.armorClass, level)
    val breakdown = breakdown(sheet, level)
}

/** puts the stored records and the remembered screen state together into what the screen draws */
@Suppress("LongParameterList")
internal fun buildMeCharacter(
    member: PartyMember,
    vitals: CharacterVitals,
    sheet: CharacterSheet,
    figures: MeSheetFigures,
    strips: List<AbilityStrip>,
    resources: List<CharacterResource>,
    tabs: List<MeTab>,
    ui: MeUiState,
    readOnly: Boolean,
    items: MeItems,
    liveSessionTitle: String?,
): MeCharacter =
    MeCharacter(
        id = member.id,
        name = member.name,
        portrait = member.portrait,
        characterClass = member.characterClass,
        level = member.level,
        race = member.race,
        fallen = member.fallen,
        vitals = vitals,
        numbers = figures.numbers,
        tab = ui.tab.takeIf { it in tabs } ?: MeTab.SHEET,
        tabs = tabs,
        spellsHidden = sheet.spellsHidden,
        sheet = sheetState(sheet, strips, ui),
        spells = spellsState(sheet, member.level ?: 1, resources, ui.slotLevel),
        resources = featureResources(resources),
        breakdown = figures.breakdown,
        popover = ui.popover,
        overlay =
            ui.overlay(vitals)
                ?: ui.rest?.overlay(vitals, resources, liveSessionTitle)
                ?: ui.wallet?.let { MeOverlay.Wallet(it, it.change(sheet.purse)) },
        items = items,
        liveSessionTitle = liveSessionTitle,
        menuOpen = ui.menuOpen,
        readOnly = readOnly,
    )

internal fun meTabs(hasSpells: Boolean): List<MeTab> =
    buildList {
        add(MeTab.SHEET)
        if (hasSpells) add(MeTab.SPELLS)
        add(MeTab.RESOURCES)
        add(MeTab.ITEMS)
    }

private fun spellsState(
    sheet: CharacterSheet,
    level: Int,
    resources: List<CharacterResource>,
    openLevel: Int?,
): MeSpellsState {
    val columns = slotColumns(resources)
    return MeSpellsState(
        ability = sheet.spellAbility,
        saveDc = FifthEditionRules.spellSaveDc(sheet, level),
        attack = FifthEditionRules.spellAttack(sheet, level),
        columns = columns,
        // a level whose slots were taken away has nothing to open
        openLevel = openLevel?.takeIf { open -> columns.any { it.level == open } },
    )
}

private fun sheetState(
    sheet: CharacterSheet,
    strips: List<AbilityStrip>,
    ui: MeUiState,
): MeSheetState =
    MeSheetState(
        source = sheet,
        filled = sheet.abilities.isNotEmpty(),
        strips = strips,
        searchOpen = ui.searchOpen,
        query = ui.query,
        inspiration = sheet.inspiration,
        languages = ui.textDrafts[MeText.LANGUAGES] ?: sheet.languages,
        features = ui.textDrafts[MeText.FEATURES] ?: sheet.features,
        subclass = sheet.subclass,
        background = sheet.background,
        alignment = sheet.alignment,
        personality = ui.textDrafts[MeText.PERSONALITY] ?: sheet.personality,
    )

private fun breakdown(
    sheet: CharacterSheet,
    level: Int,
): MeBreakdown =
    MeBreakdown(
        level = level,
        dexterityBonus = FifthEditionRules.abilityBonus(sheet, Ability.DEXTERITY),
        wisdomBonus = FifthEditionRules.abilityBonus(sheet, Ability.WISDOM),
        perceptionProficiency = sheet.mark(Skill.PERCEPTION),
        initiativeAdjustment = sheet.initiativeAdjustment,
        passiveAdjustment = sheet.passivePerceptionAdjustment,
    )
