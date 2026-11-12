package com.lyrismet.incadent.feature.combat.presentation.round

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppLineHeight
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.AppTextInputStyle
import com.lyrismet.incadent.core.designsystem.component.AppTitleSubtitle
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.FormPickField
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.TagChip
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.presentation.style.DamageTypeShield
import com.lyrismet.incadent.feature.combat.domain.round.NameChoice
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_name_carry_title
import incadent.shared.generated.resources.combat_name_title
import incadent.shared.generated.resources.combat_promo_learned
import incadent.shared.generated.resources.combat_promo_reject
import incadent.shared.generated.resources.combat_role_title
import org.jetbrains.compose.resources.stringResource

/** the name of a participant made of a species: the field, what it can turn into, and what a new npc would carry */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NamingField(
    naming: NamingState,
    send: (CombatRoundEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FormFieldLabel(stringResource(Res.string.combat_name_title).uppercase())
        AppTextInput(
            value = naming.value,
            onValueChange = { send(CombatRoundEvent.NameChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = naming.placeholder,
            autoFocus = naming.focus,
            style = AppTextInputStyle(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)),
        )
        naming.options.forEach { option -> NameOptionRow(option) { send(CombatRoundEvent.NameChosen(option.choice)) } }
        FormPickField(
            label = stringResource(Res.string.combat_role_title).uppercase(),
            placeholder = naming.role.value,
            onClick = { send(CombatRoundEvent.ClassOpened) },
            title = naming.role.value.takeIf { naming.role.isSet },
            subtitle = naming.role.sub,
        )
        if (naming.carry.isNotEmpty() || naming.carryRole.isNotBlank()) {
            FormFieldLabel(stringResource(Res.string.combat_name_carry_title).uppercase())
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (naming.carryRole.isNotBlank()) {
                    TagChip(
                        naming.carryRole,
                        foreground = AppPalette.Gold,
                        background = AppPalette.Gold.copy(alpha = 0.12f),
                    )
                }
                naming.carry.forEach {
                    TagChip(
                        it,
                        foreground = AppPalette.TextSecondary,
                        background = AppPalette.Background,
                    )
                }
            }
        }
    }
}

@Composable
private fun NameOptionRow(
    option: NameOption,
    onClick: () -> Unit,
) {
    val plain = option.choice is NameChoice.Plain
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Field,
                    background =
                        if (option.choice is NameChoice.New) {
                            AppPalette.Gold.copy(
                                alpha = 0.08f,
                            )
                        } else {
                            AppPalette.Background
                        },
                    border =
                        when {
                            plain -> AppPalette.BorderHover
                            option.choice is NameChoice.New -> AppPalette.Gold.copy(alpha = 0.5f)
                            else -> AppPalette.Border
                        },
                    onClick = onClick,
                ).defaultMinSize(minHeight = if (plain) 44.dp else 52.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (plain) {
            Text(
                text = option.title,
                color = AppPalette.TextPrimary,
                fontSize = AppFontSize.Small,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            return@Row
        }
        OptionTile(option)
        AppTitleSubtitle(
            title = option.title,
            subtitle = option.subtitle,
            titleColor = AppPalette.TextPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

// a plus for a name that makes a new npc, the first letter for a record that is already in the codex
@Composable
private fun OptionTile(option: NameOption) {
    val isNew = option.choice is NameChoice.New
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(32.dp)
                .appCard(
                    shape = AppShapes.Field,
                    background = if (isNew) AppPalette.Gold.copy(alpha = 0.15f) else AppPalette.Surface,
                    border = if (isNew) AppPalette.Gold.copy(alpha = 0.5f) else AppPalette.Border,
                ),
    ) {
        Text(
            text = if (isNew) "+" else option.title.take(1).uppercase(),
            color = AppPalette.Gold,
            fontSize = AppFontSize.Heading,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** what the naming did, and for each thing the fight learned whether the species or only this npc keeps it */
@Composable
internal fun PromotionSheetUi(
    sheet: PromotionSheet,
    send: (CombatRoundEvent) -> Unit,
) {
    AppBottomSheet(onDismissRequest = { send(CombatRoundEvent.PromotionRejected) }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FormFieldLabel(sheet.overline, color = AppPalette.Gold)
            Text(
                sheet.name,
                color = AppPalette.TextPrimary,
                fontSize = AppFontSize.Title,
                fontWeight = FontWeight.SemiBold,
            )
            Column {
                sheet.rows.forEach { PromotionRowView(it) }
            }
            if (sheet.learned.isNotEmpty()) {
                FormFieldLabel(stringResource(Res.string.combat_promo_learned).uppercase())
                sheet.learned.forEach { LearnedRowView(it, send) }
            }
            AppButton(
                text = sheet.confirmLabel,
                onClick = { send(CombatRoundEvent.PromotionConfirmed) },
                variant = ButtonVariant.Solid,
                size = ButtonSize.Primary,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(
                text = stringResource(Res.string.combat_promo_reject),
                onClick = { send(CombatRoundEvent.PromotionRejected) },
                tone = ButtonTone.Neutral,
                variant = ButtonVariant.Ghost,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PromotionRowView(row: PromotionRow) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            row.title,
            color = AppPalette.TextTertiary,
            fontSize = AppFontSize.Small,
            lineHeight = AppLineHeight.Row,
            modifier = Modifier.padding(top = 2.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(row.value, color = AppPalette.TextPrimary, fontSize = AppFontSize.Body, lineHeight = AppLineHeight.Row)
            if (row.hint.isNotEmpty()) {
                Text(
                    row.hint,
                    color = AppPalette.TextTertiary,
                    fontSize = AppFontSize.Caption,
                    lineHeight = AppLineHeight.Row,
                )
            }
        }
    }
    AppDivider()
}

@Composable
private fun LearnedRowView(
    row: LearnedRow,
    send: (CombatRoundEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            DamageTypeShield(row.type, row.state, width = 22.dp)
            Text(row.label, color = AppPalette.TextPrimary, fontSize = AppFontSize.Body, lineHeight = AppLineHeight.Row)
        }
        AppSegmentedTabs(
            tabs = listOf(SegmentedTab(false, row.allLabel), SegmentedTab(true, row.onlyLabel)),
            selected = row.onlyNpc,
            onSelected = { send(CombatRoundEvent.PromotionScopeChanged(row.type, it)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
