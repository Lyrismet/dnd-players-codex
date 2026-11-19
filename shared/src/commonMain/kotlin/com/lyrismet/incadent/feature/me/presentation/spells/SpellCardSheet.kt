package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppDamageGlyph
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.AppTextInputStyle
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.HoldFrame
import com.lyrismet.incadent.core.designsystem.component.HoldHintCard
import com.lyrismet.incadent.core.designsystem.component.HoldToEditField
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.OverlineStyle
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.SheetCloseButton
import com.lyrismet.incadent.core.designsystem.component.UndoToast
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.dashedBorder
import com.lyrismet.incadent.core.designsystem.component.floatingBarAnchor
import com.lyrismet.incadent.feature.codex.domain.model.CastKind
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.DurationUnit
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_card_delete_spell
import incadent.shared.generated.resources.me_spells_card_description_hint
import incadent.shared.generated.resources.me_spells_card_edit_form
import incadent.shared.generated.resources.me_spells_card_empty
import incadent.shared.generated.resources.me_spells_card_over
import incadent.shared.generated.resources.me_spells_description_cancel
import incadent.shared.generated.resources.me_spells_description_done
import incadent.shared.generated.resources.quick_edit_hold_tip
import incadent.shared.generated.resources.quick_edit_hold_tip_dismiss
import org.jetbrains.compose.resources.stringResource

/** the card of a spell with what it does and costs, a hold on any block opens that block to change */
@Composable
internal fun SpellCardSheet(
    spell: CharacterSpell,
    state: SpellsBlockState,
    send: (SpellsEvent) -> Unit,
) {
    val close = { send(SpellsEvent.CardClosed) }
    var menu by remember(spell.id) { mutableStateOf(false) }
    val editable = !state.readOnly
    val hold = { block: SpellBlock -> send(SpellsEvent.BlockHeld(spell.id, block)) }
    AppBottomSheet(onDismissRequest = close) {
        Box {
            AppSheetColumn(
                scroll = true,
                spacing = 14.dp,
                header = {
                    CardHeader(spell, editable, { menu = !menu }, close) { hold(SpellBlock.NAME) }
                },
            ) {
                if (menu && editable) CardMenu(spell, send)
                if (editable && state.holdTip && state.descriptionDraft == null) {
                    HoldHintCard(
                        text = stringResource(Res.string.quick_edit_hold_tip),
                        dismissLabel = stringResource(Res.string.quick_edit_hold_tip_dismiss),
                        onDismiss = { send(SpellsEvent.HoldTipDismissed) },
                    )
                }
                Badges(spell)
                DamageBlock(spell, state.characterLevel, if (editable) hold else null)
                CardGrid(spell, if (editable) hold else null)
                CardDescription(spell, state, send)
                GrowthReference(spell, state, if (editable) hold else null)
                if (editable) SpendChips(spell, state.columns, send)
            }
            UndoToast(
                action = state.undo,
                onUndo = { send(SpellsEvent.UndoClicked) },
                onFollowUp = {},
                onDismiss = { send(SpellsEvent.UndoDismissed) },
                modifier = floatingBarAnchor(),
            )
        }
    }
}

@Composable
private fun CardHeader(
    spell: CharacterSpell,
    editable: Boolean,
    onMore: () -> Unit,
    onClose: () -> Unit,
    onNameHeld: () -> Unit,
) {
    val look = if (spell.school.isBlank()) null else schoolOf(spell.school)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            if (spell.school.isBlank()) {
                SectionOverline(text = levelLabel(spell), color = AppPalette.Arcane, style = OverlineStyle.Section)
            } else {
                SectionOverline(
                    text = stringResource(Res.string.me_spells_card_over, levelLabel(spell), "").trimEnd() + " ",
                    color = AppPalette.Arcane,
                    style = OverlineStyle.Section,
                    inlineContent = { SchoolTag(spell.school, look) },
                )
            }
            val title = @Composable {
                Text(
                    spell.name,
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = AppFontSize.SheetTitle),
                    color = AppPalette.TextHeading,
                )
            }
            if (editable) HoldToEditField(onNameHeld, HoldFrame.Title) { title() } else title()
        }
        if (editable) {
            IconBadge(size = 40.dp, background = AppPalette.BorderSubtle, onClick = onMore) {
                Text("⋯", fontSize = AppFontSize.Body, color = AppPalette.TextMuted)
            }
        }
        SheetCloseButton(onClick = onClose, size = 40.dp)
    }
}

/** the school in its colour with its icon before it, a school of own words stays plain */
@Composable
private fun SchoolTag(
    school: String,
    look: School?,
) {
    val color = look?.color ?: AppPalette.Arcane
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (look != null) AppDamageGlyph(look.path, color, size = 14.dp)
        Text(
            school.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = OverlineStyle.Section.letterSpacing),
            color = color,
            maxLines = 1,
        )
    }
}

/** the two things the "⋯" of the card does that no block does, the whole form and the delete */
@Composable
private fun CardMenu(
    spell: CharacterSpell,
    send: (SpellsEvent) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .appCard(AppShapes.Field, AppPalette.Background, AppPalette.BorderHover)
            .padding(horizontal = 14.dp),
    ) {
        MenuRow(stringResource(Res.string.me_spells_card_edit_form), AppPalette.TextPrimary) {
            send(SpellsEvent.EditRequested(spell.id))
        }
        AppDivider()
        MenuRow(stringResource(Res.string.me_spells_card_delete_spell), AppPalette.MaroonBright) {
            send(SpellsEvent.Deleted(spell.id))
        }
    }
}

@Composable
private fun MenuRow(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .appCard(AppShapes.Field, Color.Transparent, null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

/** the text of the spell that a hold turns into a field, a spell with nothing yet says where to add it */
@Composable
private fun CardDescription(
    spell: CharacterSpell,
    state: SpellsBlockState,
    send: (SpellsEvent) -> Unit,
) {
    val draft = state.descriptionDraft
    when {
        draft != null -> DescriptionEditor(draft, send)
        spell.text.isNotBlank() -> {
            val body = @Composable {
                Text(spell.text, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextPrimary)
            }
            if (state.readOnly) {
                body()
            } else {
                HoldToEditField({ send(SpellsEvent.DescriptionHeld(spell.id)) }, HoldFrame.Description) { body() }
            }
        }
        spell.isBare -> {
            Text(
                stringResource(Res.string.me_spells_card_empty),
                Modifier
                    .fillMaxWidth()
                    .dashedBorder(AppPalette.BorderHover, cornerRadius = 12.dp)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                style = AppText.footnote,
                color = AppPalette.TextSecondary,
            )
        }
    }
}

// nothing but the name, so the card has no block that could be held
private val CharacterSpell.isBare: Boolean
    get() =
        text.isBlank() &&
            damage.none { it.dice.isNotBlank() } &&
            range.isBlank() &&
            !hasComponents &&
            duration.isBlank() &&
            durationUnit == DurationUnit.NONE &&
            castKind == CastKind.ACTION

@Composable
private fun DescriptionEditor(
    draft: String,
    send: (SpellsEvent) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .appCard(AppShapes.Field, AppPalette.Background, AppPalette.Gold, borderWidth = 1.5.dp)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppTextInput(
            value = draft,
            onValueChange = { send(SpellsEvent.DescriptionChanged(it)) },
            placeholder = stringResource(Res.string.me_spells_card_description_hint),
            singleLine = false,
            minLines = 5,
            style =
                AppTextInputStyle(
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = AppPalette.TextPrimary),
                    chrome = null,
                    contentPadding = PaddingValues(),
                    placeholderColor = AppPalette.TextTertiary,
                ),
            autoFocus = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)) {
            AppButton(
                text = stringResource(Res.string.me_spells_description_cancel),
                onClick = { send(SpellsEvent.DescriptionClosed) },
                tone = ButtonTone.Neutral,
            )
            AppButton(
                text = stringResource(Res.string.me_spells_description_done),
                onClick = { send(SpellsEvent.DescriptionSaved) },
                variant = ButtonVariant.Solid,
            )
        }
    }
}
