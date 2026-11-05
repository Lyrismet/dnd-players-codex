package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppStepper
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.FormTextField
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.model.ResourceTemplate
import com.lyrismet.incadent.feature.codex.domain.model.SPELL_LEVELS
import com.lyrismet.incadent.feature.me.domain.availableTemplates
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_done
import dndplayerscodex.shared.generated.resources.me_resource_add_done
import dndplayerscodex.shared.generated.resources.me_resource_delete
import dndplayerscodex.shared.generated.resources.me_resource_max_hint
import dndplayerscodex.shared.generated.resources.me_resource_max_label
import dndplayerscodex.shared.generated.resources.me_resource_name_label
import dndplayerscodex.shared.generated.resources.me_resource_name_placeholder
import dndplayerscodex.shared.generated.resources.me_resource_new_hint
import dndplayerscodex.shared.generated.resources.me_resource_new_title
import dndplayerscodex.shared.generated.resources.me_resource_overline
import dndplayerscodex.shared.generated.resources.me_resource_title
import dndplayerscodex.shared.generated.resources.me_slots_hint
import dndplayerscodex.shared.generated.resources.me_slots_level
import dndplayerscodex.shared.generated.resources.me_slots_overline
import dndplayerscodex.shared.generated.resources.me_slots_title
import dndplayerscodex.shared.generated.resources.me_spells_pick_hint
import dndplayerscodex.shared.generated.resources.me_spells_pick_none
import dndplayerscodex.shared.generated.resources.me_spells_pick_title
import dndplayerscodex.shared.generated.resources.me_spells_tile_ability
import dndplayerscodex.shared.generated.resources.me_step_less
import dndplayerscodex.shared.generated.resources.me_step_more
import dndplayerscodex.shared.generated.resources.me_template_action_surge
import dndplayerscodex.shared.generated.resources.me_template_arcane_recovery
import dndplayerscodex.shared.generated.resources.me_template_bardic_inspiration
import dndplayerscodex.shared.generated.resources.me_template_channel_divinity
import dndplayerscodex.shared.generated.resources.me_template_divine_sense
import dndplayerscodex.shared.generated.resources.me_template_indomitable
import dndplayerscodex.shared.generated.resources.me_template_ki_points
import dndplayerscodex.shared.generated.resources.me_template_lay_on_hands
import dndplayerscodex.shared.generated.resources.me_template_rage
import dndplayerscodex.shared.generated.resources.me_template_second_wind
import dndplayerscodex.shared.generated.resources.me_template_sorcery_points
import dndplayerscodex.shared.generated.resources.me_template_spell_slots
import dndplayerscodex.shared.generated.resources.me_template_wild_shape
import dndplayerscodex.shared.generated.resources.me_templates_hint
import dndplayerscodex.shared.generated.resources.me_templates_overline
import dndplayerscodex.shared.generated.resources.me_templates_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SlotsSheet(
    edit: MeOverlay.Slots,
    send: (OverlayEvent) -> Unit,
) {
    EditSheet(
        overline = stringResource(Res.string.me_slots_overline),
        title = stringResource(Res.string.me_slots_title),
        onClose = { send(OverlayEvent.Closed) },
        onDone = { send(OverlayEvent.Applied) },
    ) {
        Column(Modifier.fillMaxWidth().appCard(background = AppPalette.Background, border = AppPalette.Border)) {
            SPELL_LEVELS.forEach { level ->
                if (level > SPELL_LEVELS.first) AppDivider()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 14.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(Res.string.me_slots_level, level),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppPalette.TextPrimary,
                    )
                    AppStepper(
                        count = edit.maxima[level] ?: 0,
                        onDecrease = { send(OverlayEvent.SlotStepped(level, -1)) },
                        onIncrease = { send(OverlayEvent.SlotStepped(level, 1)) },
                        decreaseDescription = stringResource(Res.string.me_step_less),
                        increaseDescription = stringResource(Res.string.me_step_more),
                        tiles = true,
                        removesAtOne = false,
                    )
                }
            }
        }
        Text(stringResource(Res.string.me_slots_hint), style = AppText.footnote, color = AppPalette.TextSecondary)
    }
}

@Composable
internal fun ResourceSheet(
    edit: MeOverlay.Resource,
    send: (OverlayEvent) -> Unit,
    onDelete: () -> Unit,
) {
    val isNew = edit.id == 0L
    EditSheet(
        overline = stringResource(Res.string.me_resource_overline),
        title = stringResource(if (isNew) Res.string.me_resource_new_title else Res.string.me_resource_title),
        onClose = { send(OverlayEvent.Closed) },
        onDone = { send(OverlayEvent.Applied) },
        doneLabel = stringResource(if (isNew) Res.string.me_resource_add_done else Res.string.me_done),
        doneEnabled = edit.name.isNotBlank(),
    ) {
        FormTextField(
            label = stringResource(Res.string.me_resource_name_label),
            value = edit.name,
            onChange = { send(OverlayEvent.TextChanged(OverlayField.RESOURCE_NAME, it)) },
            placeholder = stringResource(Res.string.me_resource_name_placeholder),
        )
        SectionOverline(stringResource(Res.string.me_resource_max_label), color = AppPalette.TextTertiary)
        NumberRow(edit.max, send, spread = true)
        Text(
            stringResource(if (isNew) Res.string.me_resource_new_hint else Res.string.me_resource_max_hint),
            style = AppText.footnote,
            color = AppPalette.TextSecondary,
        )
        if (edit.id != 0L) {
            AppButton(
                text = stringResource(Res.string.me_resource_delete),
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                tone = ButtonTone.Maroon,
                height = 48.dp,
            )
        }
    }
}

@Composable
internal fun SpellAbilitySheet(
    edit: MeOverlay.SpellAbility,
    onChoose: (Ability?) -> Unit,
    onClose: () -> Unit,
) {
    AppBottomSheet(onDismissRequest = onClose) {
        AppSheetColumn(
            scroll = true,
            header = {
                AppSheetHeader(
                    title = stringResource(Res.string.me_spells_pick_title),
                    onClose = onClose,
                    overline = stringResource(Res.string.me_spells_tile_ability),
                )
            },
        ) {
            Column(Modifier.fillMaxWidth().appCard(background = AppPalette.Background, border = AppPalette.Border)) {
                Ability.entries.forEach { ability ->
                    PickRow(stringResource(ability.nameRes()), edit.selected == ability) { onChoose(ability) }
                    AppDivider()
                }
                PickRow(stringResource(Res.string.me_spells_pick_none), edit.selected == null) { onChoose(null) }
            }
            Text(
                stringResource(Res.string.me_spells_pick_hint),
                style = AppText.footnote,
                color = AppPalette.TextSecondary,
            )
        }
    }
}

@Composable
internal fun TemplatesSheet(
    resources: List<CharacterResource>,
    onChoose: (ResourceTemplate, String) -> Unit,
    onClose: () -> Unit,
) {
    AppBottomSheet(onDismissRequest = onClose) {
        AppSheetColumn(
            scroll = true,
            header = {
                AppSheetHeader(
                    title = stringResource(Res.string.me_templates_title),
                    onClose = onClose,
                    overline = stringResource(Res.string.me_templates_overline),
                )
            },
        ) {
            Column(Modifier.fillMaxWidth().appCard(background = AppPalette.Background, border = AppPalette.Border)) {
                val templates = availableTemplates(resources)
                templates.forEachIndexed { index, template ->
                    if (index > 0) AppDivider()
                    val name = stringResource(template.nameRes())
                    PickRow(name, selected = false) { onChoose(template, name) }
                }
            }
            Text(
                stringResource(Res.string.me_templates_hint),
                style = AppText.footnote,
                color = AppPalette.TextSecondary,
            )
        }
    }
}

@Composable
private fun PickRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            label,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) AppPalette.GoldBright else AppPalette.TextPrimary,
        )
    }
}

private fun ResourceTemplate.nameRes(): StringResource =
    when (this) {
        ResourceTemplate.SPELL_SLOTS -> Res.string.me_template_spell_slots
        ResourceTemplate.RAGE -> Res.string.me_template_rage
        ResourceTemplate.BARDIC_INSPIRATION -> Res.string.me_template_bardic_inspiration
        ResourceTemplate.CHANNEL_DIVINITY -> Res.string.me_template_channel_divinity
        ResourceTemplate.WILD_SHAPE -> Res.string.me_template_wild_shape
        ResourceTemplate.SECOND_WIND -> Res.string.me_template_second_wind
        ResourceTemplate.ACTION_SURGE -> Res.string.me_template_action_surge
        ResourceTemplate.INDOMITABLE -> Res.string.me_template_indomitable
        ResourceTemplate.KI_POINTS -> Res.string.me_template_ki_points
        ResourceTemplate.SORCERY_POINTS -> Res.string.me_template_sorcery_points
        ResourceTemplate.LAY_ON_HANDS -> Res.string.me_template_lay_on_hands
        ResourceTemplate.DIVINE_SENSE -> Res.string.me_template_divine_sense
        ResourceTemplate.ARCANE_RECOVERY -> Res.string.me_template_arcane_recovery
    }
