package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FlameSweepEffect
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummaryItem.PartySummary
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_party_chronicle_damage
import dndplayerscodex.shared.generated.resources.codex_party_chronicle_fights
import dndplayerscodex.shared.generated.resources.codex_party_chronicle_sessions
import dndplayerscodex.shared.generated.resources.codex_party_chronicle_title
import dndplayerscodex.shared.generated.resources.codex_party_epitaph
import dndplayerscodex.shared.generated.resources.codex_party_epitaph_foes
import dndplayerscodex.shared.generated.resources.codex_party_fell_in_battle
import dndplayerscodex.shared.generated.resources.codex_party_revive
import dndplayerscodex.shared.generated.resources.entity_sheet_party_presence_label
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PartySummaryBody(
    party: PartySummary,
    actions: EntitySummarySheetActions,
) {
    // the flame goes over the card once, the card itself changes under it when the record does
    var revived by remember { mutableStateOf(false) }
    FlameSweepEffect(play = revived) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            PartyBody(party, actions) { revived = true }
        }
    }
}

@Composable
private fun PartyBody(
    party: PartySummary,
    actions: EntitySummarySheetActions,
    onRevived: () -> Unit,
) {
    val fall = party.fall
    EntityHeader(
        emblem = party.emblem,
        title = party.name,
        subtitle = party.subtitle,
        strikeThrough = fall != null,
        showEditHint = actions.onEditClicked != null,
        quick = actions.quickEdit,
    )
    if (fall != null) Epitaph(fall, actions)
    if (fall == null) PresencePicker(party, actions)
    EntityDescription(party.description, party.descriptionSegments, actions)
    EntityFactsGrid(party.facts, actions.onEntityRefClicked, actions.quickEdit)
    if (fall != null) Chronicle(fall, actions.onPartyRevived?.let { revive -> { revive(party.ref.id) } }, onRevived)
    EntityRelatedNotesSection(party.name, party.relatedNotes, actions.onRelatedNoteClicked)
    EntityDeleteButton(party.ref, actions)
}

@Composable
private fun PresencePicker(
    party: PartySummary,
    actions: EntitySummarySheetActions,
) {
    val onSelected = actions.onPartyPresenceSelected ?: return
    val title = stringResource(Res.string.entity_sheet_party_presence_label)
    if (actions.statusesReadOnly) {
        party.presenceOptions.firstOrNull { it.isSelected }?.let { EntityStatusSeals(listOf(StatusSeal(title, it))) }
    } else {
        EntityStatusSection(
            title = title,
            options = party.presenceOptions,
            onSelected = { presence -> onSelected(party.ref.id, presence) },
        )
    }
}

/** "Пал · Сессия XIII · в бою с Гоблин ×3", a tap opens the session the fight was saved to */
@Composable
private fun Epitaph(
    fall: PartyFall,
    actions: EntitySummarySheetActions,
) {
    val session = fall.sessionLabel ?: stringResource(Res.string.codex_party_fell_in_battle)
    val text =
        if (fall.foes.isBlank()) {
            stringResource(Res.string.codex_party_epitaph, session)
        } else {
            stringResource(Res.string.codex_party_epitaph_foes, session, fall.foes)
        }
    val open = fall.sessionNoteId?.let { id -> { actions.onRelatedNoteClicked(id, 0L) } }
    Text(
        text = text,
        modifier = if (open != null) Modifier.clickable(onClick = open) else Modifier,
        style = AppText.rowSubtitle,
        color = AppPalette.Parchment,
    )
}

@Composable
private fun Chronicle(
    fall: PartyFall,
    onRevive: (() -> Unit)?,
    onRevived: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FormFieldLabel(stringResource(Res.string.codex_party_chronicle_title), modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChronicleTile(
                fall.sessions,
                pluralStringResource(Res.plurals.codex_party_chronicle_sessions, fall.sessions),
                Modifier.weight(1f),
            )
            ChronicleTile(
                fall.fights,
                pluralStringResource(Res.plurals.codex_party_chronicle_fights, fall.fights),
                Modifier.weight(1f),
            )
            ChronicleTile(fall.damage, stringResource(Res.string.codex_party_chronicle_damage), Modifier.weight(1f))
        }
        if (onRevive != null) {
            AppButton(
                text = stringResource(Res.string.codex_party_revive),
                onClick = {
                    onRevived()
                    onRevive()
                },
                modifier = Modifier.fillMaxWidth(),
                tone = ButtonTone.Gold,
                variant = ButtonVariant.Tinted,
                size = ButtonSize.Primary,
            )
        }
    }
}

@Composable
private fun ChronicleTile(
    value: Int,
    label: String,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.appCard(background = AppPalette.Background).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = value.toString(), style = MaterialTheme.typography.headlineSmall, color = AppPalette.TextHeading)
        Text(text = label, color = AppPalette.TextSecondary, fontSize = AppFontSize.CaptionPlus)
    }
}
