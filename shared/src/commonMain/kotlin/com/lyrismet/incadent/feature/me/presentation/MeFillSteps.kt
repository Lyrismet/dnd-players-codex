package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppStepper
import com.lyrismet.incadent.core.designsystem.component.FormNumberField
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.core.designsystem.component.PillFill
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.SelectablePill
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.numeral
import com.lyrismet.incadent.core.format.signed
import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.PartyStatRanges
import com.lyrismet.incadent.feature.me.domain.abilityStrips
import com.lyrismet.incadent.feature.me.domain.headerNumbers
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_ac_title
import dndplayerscodex.shared.generated.resources.me_fill_abilities_hint
import dndplayerscodex.shared.generated.resources.me_fill_class_label
import dndplayerscodex.shared.generated.resources.me_fill_class_placeholder
import dndplayerscodex.shared.generated.resources.me_fill_hp_max_label
import dndplayerscodex.shared.generated.resources.me_fill_level_label
import dndplayerscodex.shared.generated.resources.me_fill_proficiency_hint
import dndplayerscodex.shared.generated.resources.me_fill_proficiency_label
import dndplayerscodex.shared.generated.resources.me_fill_saves_label
import dndplayerscodex.shared.generated.resources.me_fill_skills_label
import dndplayerscodex.shared.generated.resources.me_fill_step4_hint
import dndplayerscodex.shared.generated.resources.me_fill_subclass_label
import dndplayerscodex.shared.generated.resources.me_fill_subclass_placeholder
import dndplayerscodex.shared.generated.resources.me_speed_title
import dndplayerscodex.shared.generated.resources.me_step_less
import dndplayerscodex.shared.generated.resources.me_step_more
import org.jetbrains.compose.resources.stringResource

private const val SPEED_STEP = 5

@Composable
internal fun StepClass(
    draft: FillDraft,
    send: (OverlayEvent) -> Unit,
) {
    FormTextField(
        label = stringResource(Res.string.me_fill_class_label),
        value = draft.characterClass,
        onChange = { send(OverlayEvent.TextChanged(OverlayField.CLASS, it)) },
        placeholder = stringResource(Res.string.me_fill_class_placeholder),
    )
    FormTextField(
        label = stringResource(Res.string.me_fill_subclass_label),
        value = draft.subclass,
        onChange = { send(OverlayEvent.TextChanged(OverlayField.SUBCLASS, it)) },
        placeholder = stringResource(Res.string.me_fill_subclass_placeholder),
    )
    SectionOverline(stringResource(Res.string.me_fill_level_label), color = AppPalette.TextTertiary)
    NumberRow(draft.level, send)
}

@Composable
internal fun StepAbilities(
    draft: FillDraft,
    send: (OverlayEvent) -> Unit,
) {
    Text(
        stringResource(Res.string.me_fill_abilities_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = AppPalette.TextMuted,
    )
    val strips = abilityStrips(draft.toSheet(), draft.level).associateBy { it.ability }
    Column(
        Modifier
            .fillMaxWidth()
            .appCard(
                background = AppPalette.Background,
                border = AppPalette.Border,
            ).padding(start = 14.dp, end = 12.dp),
    ) {
        Ability.entries.forEach { ability ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 54.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    stringResource(ability.nameRes()),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppPalette.TextPrimary,
                )
                Text(strips[ability]?.bonus.signed(), style = AppText.caption, color = AppPalette.TextSecondary)
                AppStepper(
                    count = draft.abilities[ability] ?: 10,
                    onDecrease = { send(OverlayEvent.FillAbilityStepped(ability, -1)) },
                    onIncrease = { send(OverlayEvent.FillAbilityStepped(ability, 1)) },
                    decreaseDescription = stringResource(Res.string.me_step_less),
                    increaseDescription = stringResource(Res.string.me_step_more),
                    tiles = true,
                    removesAtOne = false,
                )
            }
        }
    }
}

@Composable
internal fun StepSkills(
    draft: FillDraft,
    send: (OverlayEvent) -> Unit,
) {
    val sheet = draft.toSheet()
    ProficiencyBonusRow(headerNumbers(sheet, null, draft.level).proficiency)
    SectionOverline(stringResource(Res.string.me_fill_saves_label), color = AppPalette.TextTertiary)
    SaveChoices(draft, send)
    SectionOverline(stringResource(Res.string.me_fill_skills_label), color = AppPalette.TextTertiary)
    SkillChoices(sheet, draft.level, send)
}

@Composable
private fun ProficiencyBonusRow(bonus: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .appCard(background = AppPalette.Background, border = AppPalette.Border)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(Res.string.me_fill_proficiency_label),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = AppPalette.TextPrimary,
        )
        Text(bonus.signed(), style = AppText.numeral(20.sp), color = AppPalette.TextHeading)
        Text(
            stringResource(Res.string.me_fill_proficiency_hint),
            style = AppText.caption,
            color = AppPalette.TextSecondary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SaveChoices(
    draft: FillDraft,
    send: (OverlayEvent) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Ability.entries.forEach { ability ->
            SelectablePill(
                text = stringResource(ability.nameRes()),
                selected = ability in draft.saves,
                onClick = { send(OverlayEvent.FillSaveToggled(ability)) },
                selectedFill = PillFill.GoldTint,
            )
        }
    }
}

@Composable
private fun SkillChoices(
    sheet: CharacterSheet,
    level: Int,
    send: (OverlayEvent) -> Unit,
) {
    val rows =
        abilityStrips(
            sheet,
            level,
        ).flatMap { it.rows }.filter { it.skill != null }.sortedBy { it.skill?.ordinal }
    Column(
        Modifier
            .fillMaxWidth()
            .appCard(
                background = AppPalette.Background,
                border = AppPalette.Border,
            ).padding(horizontal = 6.dp),
    ) {
        rows.forEach { row ->
            val skill = row.skill ?: return@forEach
            Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { send(OverlayEvent.FillSkillTapped(skill)) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) { ProficiencyMark(row.proficiency) }
                Text(
                    stringResource(skill.nameRes()),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppPalette.TextPrimary,
                )
                Text(
                    row.bonus.signed(),
                    style = AppText.numeral(15.sp),
                    color = AppPalette.TextMuted,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
    }
}

@Composable
internal fun StepNumbers(
    draft: FillDraft,
    send: (OverlayEvent) -> Unit,
) {
    FormNumberField(
        label = stringResource(Res.string.me_fill_hp_max_label),
        value = draft.hpMax,
        range = PartyStatRanges.hpMax,
        onChange = { send(OverlayEvent.FillHpMaxChanged(it)) },
    )
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.me_ac_title),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = AppPalette.TextPrimary,
        )
        StepperBox(draft.armorClass, {
            send(OverlayEvent.FillArmorClassStepped(-1))
        }, { send(OverlayEvent.FillArmorClassStepped(1)) })
    }
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.me_speed_title),
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = AppPalette.TextPrimary,
        )
        StepperBox(draft.speed, {
            send(OverlayEvent.FillSpeedStepped(-SPEED_STEP))
        }, { send(OverlayEvent.FillSpeedStepped(SPEED_STEP)) })
    }
    Text(stringResource(Res.string.me_fill_step4_hint), style = AppText.footnote, color = AppPalette.TextSecondary)
}

@Composable
private fun StepperBox(
    value: Int,
    onLess: () -> Unit,
    onMore: () -> Unit,
) {
    AppStepper(
        count = value,
        onDecrease = onLess,
        onIncrease = onMore,
        decreaseDescription = stringResource(Res.string.me_step_less),
        increaseDescription = stringResource(Res.string.me_step_more),
        tiles = true,
        removesAtOne = false,
    )
}

internal fun FillDraft.toSheet(): CharacterSheet =
    CharacterSheet(abilities = abilities, saves = saves, skillMarks = marks)
