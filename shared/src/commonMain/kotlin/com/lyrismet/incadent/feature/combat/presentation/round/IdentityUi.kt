package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppInputChrome
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.AppTextInputStyle
import com.lyrismet.incadent.core.designsystem.component.AppTitleSubtitle
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormPickField
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.TagChip
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.dashedBorder
import com.lyrismet.incadent.feature.codex.presentation.form.DuplicateNpc
import com.lyrismet.incadent.feature.combat.domain.round.NameChoice
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_identity_done
import incadent.shared.generated.resources.combat_identity_dup_title
import incadent.shared.generated.resources.combat_identity_link_title
import incadent.shared.generated.resources.combat_identity_to_codex
import incadent.shared.generated.resources.combat_link_carry
import incadent.shared.generated.resources.combat_link_codex_open
import incadent.shared.generated.resources.combat_link_codex_title
import incadent.shared.generated.resources.combat_link_label_title
import incadent.shared.generated.resources.combat_link_open
import incadent.shared.generated.resources.combat_link_overline
import incadent.shared.generated.resources.combat_link_title
import incadent.shared.generated.resources.combat_name_title
import incadent.shared.generated.resources.combat_role_title
import org.jetbrains.compose.resources.stringResource

private const val DIAMOND_TURN = 45f
private val GOLD_LINE = AppPalette.Gold.copy(alpha = 0.4f)
private val LABEL_DASH = Color(0xFF4A4D5C)

/** the name of the participant, and below it what ties it to the codex or what a label can become */
@Composable
internal fun IdentityBlock(
    identity: IdentityState,
    send: (CombatRoundEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RenameField(identity, send)
        RolePick(identity, send)
        when (identity.kind) {
            IdentityKind.CODEX -> CodexLinkCard(identity, send)
            IdentityKind.LABELLED -> LabelledBlocks(identity, send)
        }
    }
}

@Composable
private fun RenameField(
    identity: IdentityState,
    send: (CombatRoundEvent) -> Unit,
) {
    val rename = identity.rename
    val offer = rename.duplicate
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(stringResource(Res.string.combat_name_title).uppercase())
        AppTextInput(
            value = rename.value,
            onValueChange = { send(CombatRoundEvent.RenameChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            style =
                AppTextInputStyle(
                    chrome =
                        AppInputChrome(
                            border = if (rename.changed) AppPalette.Gold.copy(alpha = 0.7f) else AppPalette.Border,
                        ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                ),
            keyboardActions = KeyboardActions(onDone = { send(CombatRoundEvent.RenameConfirmed) }),
            autoFocus = rename.focus,
            trailing =
                if (rename.changed && offer == null) {
                    { DoneButton { send(CombatRoundEvent.RenameConfirmed) } }
                } else {
                    null
                },
        )
        if (rename.hint.isNotEmpty()) {
            Text(rename.hint, color = AppPalette.TextMuted, fontSize = AppFontSize.Caption)
        }
        if (offer != null) {
            val twin = offer.twin
            DuplicateCard(twin, offer.action, offer.note) {
                send(
                    if (identity.kind == IdentityKind.CODEX) {
                        CombatRoundEvent.MergeConfirmed
                    } else {
                        CombatRoundEvent.LinkPicked(twin.id, twin.name)
                    },
                )
            }
        }
    }
}

@Composable
private fun RolePick(
    identity: IdentityState,
    send: (CombatRoundEvent) -> Unit,
) {
    identity.role?.let { role ->
        FormPickField(
            label = stringResource(Res.string.combat_role_title).uppercase(),
            placeholder = role.value,
            onClick = { send(CombatRoundEvent.ClassOpened) },
            title = role.value.takeIf { role.isSet },
            subtitle = role.sub,
        )
    }
}

@Composable
private fun DoneButton(onClick: () -> Unit) {
    Text(
        text = stringResource(Res.string.combat_identity_done),
        color = AppPalette.Background,
        fontSize = AppFontSize.Small,
        fontWeight = FontWeight.Bold,
        modifier =
            Modifier
                .padding(end = 6.dp)
                .appCard(shape = RoundedCornerShape(8.dp), background = AppPalette.Gold, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** the record that already has the typed name, and the way to make the participant one with it */
@Composable
private fun DuplicateCard(
    twin: DuplicateNpc,
    action: String,
    note: String,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = RoundedCornerShape(12.dp),
                    background = AppPalette.Gold.copy(alpha = 0.06f),
                    border = GOLD_LINE,
                ).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Tile(twin.name.take(1).uppercase())
            AppTitleSubtitle(
                title = stringResource(Res.string.combat_identity_dup_title, twin.name),
                subtitle = twin.subtitle,
                modifier = Modifier.weight(1f),
            )
        }
        AppButton(text = action, onClick = onClick, size = ButtonSize.Medium, modifier = Modifier.fillMaxWidth())
        Text(note, color = AppPalette.TextSecondary, fontSize = AppFontSize.Caption)
    }
}

@Composable
private fun Tile(letter: String) {
    IconBadge(size = 34.dp, shape = RoundedCornerShape(10.dp), background = AppPalette.BorderSubtle) {
        Text(letter, color = AppPalette.Gold, fontSize = AppFontSize.Heading, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CodexLinkCard(
    identity: IdentityState,
    send: (CombatRoundEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(stringResource(Res.string.combat_identity_link_title).uppercase())
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .appCard(shape = AppShapes.Field, background = AppPalette.Background, border = AppPalette.Border)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                size = 32.dp,
                shape = RoundedCornerShape(9.dp),
                background = AppPalette.Gold.copy(alpha = 0.12f),
            ) {
                Box(Modifier.size(10.dp).rotate(DIAMOND_TURN).border(1.6.dp, AppPalette.GoldBright))
            }
            AppTitleSubtitle(
                title = stringResource(Res.string.combat_link_codex_title),
                subtitle = identity.linkSub,
                modifier = Modifier.weight(1f),
            )
            AppButton(
                text = stringResource(Res.string.combat_link_codex_open),
                onClick = { send(CombatRoundEvent.CodexOpened) },
                variant = ButtonVariant.Ghost,
            )
        }
    }
}

@Composable
private fun LabelledBlocks(
    identity: IdentityState,
    send: (CombatRoundEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(stringResource(Res.string.combat_identity_link_title).uppercase())
        LabelCard(identity, send)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabelCard(
    identity: IdentityState,
    send: (CombatRoundEvent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .dashedBorder(LABEL_DASH, cornerRadius = 12.dp)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppTitleSubtitle(title = stringResource(Res.string.combat_link_label_title), subtitle = identity.labelSub)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            identity.toCodex?.let { name ->
                AppButton(
                    text = "+ " + stringResource(Res.string.combat_identity_to_codex),
                    onClick = { send(CombatRoundEvent.NameChosen(NameChoice.New(name))) },
                    variant = ButtonVariant.Tinted,
                    modifier = Modifier.weight(1f),
                )
            }
            AppButton(
                text = stringResource(Res.string.combat_link_open),
                onClick = { send(CombatRoundEvent.LinkOpened) },
                variant = ButtonVariant.Ghost,
                modifier = Modifier.weight(1f),
            )
        }
        if (identity.carry.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.combat_link_carry),
                    color = AppPalette.TextTertiary,
                    fontSize = AppFontSize.Tiny,
                )
                identity.carry.forEach {
                    TagChip(
                        it.text,
                        foreground = if (it.gold) AppPalette.GoldBright else AppPalette.TextDescription,
                        background = AppPalette.Background,
                        border = if (it.gold) AppPalette.Gold.copy(alpha = 0.5f) else AppPalette.Border,
                    )
                }
            }
        }
    }
}

/** the npcs of the codex to tie a participant to */
@Composable
internal fun LinkSheetUi(
    sheet: LinkSheet,
    send: (CombatRoundEvent) -> Unit,
) {
    AppBottomSheet(onDismissRequest = { send(CombatRoundEvent.LinkClosed) }) {
        AppSheetColumn(
            spacing = 8.dp,
            scroll = true,
            header = {
                AppSheetHeader(
                    title = stringResource(Res.string.combat_link_title),
                    onClose = { send(CombatRoundEvent.LinkClosed) },
                    overline = stringResource(Res.string.combat_link_overline),
                )
            },
        ) {
            sheet.options.forEach { LinkOptionRow(it, send) }
        }
    }
}

@Composable
private fun LinkOptionRow(
    option: LinkOption,
    send: (CombatRoundEvent) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.Background,
                    border = AppPalette.Border,
                    onClick =
                        if (option.busy) {
                            null
                        } else {
                            (
                                {
                                    send(
                                        CombatRoundEvent.LinkPicked(option.npcId, option.name),
                                    )
                                }
                            )
                        },
                ).defaultMinSize(minHeight = 52.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTitleSubtitle(
            title = option.name,
            subtitle = option.subtitle,
            titleColor = if (option.busy) AppPalette.TextTertiary else AppPalette.TextPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}
