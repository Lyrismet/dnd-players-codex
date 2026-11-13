package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppDashedTile
import com.lyrismet.incadent.core.designsystem.component.HoldFrame
import com.lyrismet.incadent.core.designsystem.component.HoldToEditField
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.EMPTY_VALUE_DASH
import com.lyrismet.incadent.feature.codex.presentation.style.DamageTypeShield
import com.lyrismet.incadent.feature.codex.presentation.summary.FeatureChip
import com.lyrismet.incadent.feature.codex.presentation.summary.FeaturesBottomSheet
import com.lyrismet.incadent.feature.codex.presentation.summary.FeaturesSheetActions
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_features_add_shield
import incadent.shared.generated.resources.combat_features_header
import incadent.shared.generated.resources.combat_features_more
import incadent.shared.generated.resources.combat_features_only
import org.jetbrains.compose.resources.stringResource

/** shields and facts of the participant, no higher than three lines, a hold opens the sheet to edit them */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun FeaturesBlockUi(
    block: FeaturesBlock,
    send: (CombatRoundEvent) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    HoldToEditField(
        onHeld = { send(CombatRoundEvent.FeaturesOpened()) },
        frame = HoldFrame.Note,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.combat_features_header),
                    color = AppPalette.TextTertiary,
                    fontSize = AppFontSize.Tiny,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.weight(1f),
                )
                if (block.onlyInFight) {
                    Text(
                        stringResource(Res.string.combat_features_only),
                        color = AppPalette.GoldBright,
                        fontSize = AppFontSize.Tiny,
                    )
                }
            }
            if (editing) {
                ShieldEditor(block.cells, send) {
                    editing = false
                    send(CombatRoundEvent.ShieldsEditDone)
                }
            } else {
                ShieldLine(block) {
                    editing = true
                    send(CombatRoundEvent.ShieldsEditStarted)
                }
            }
            if (!editing && block.facts.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), maxLines = 1) {
                    block.facts.forEach { fact -> FeatureChip(fact) { send(CombatRoundEvent.FeaturesOpened(fact)) } }
                }
            }
            if (!editing && block.hidden > 0) {
                Text(
                    stringResource(Res.string.combat_features_more, block.hidden),
                    color = AppPalette.GoldBright,
                    fontSize = AppFontSize.Small,
                    modifier = Modifier.clickable { send(CombatRoundEvent.FeaturesOpened()) },
                )
            }
        }
    }
}

/** the shields the participant has, in one line, and the dashed button that opens every type for a tap */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ShieldLine(
    block: FeaturesBlock,
    onEdit: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        AppDashedTile(
            onClick = onEdit,
            minHeight = 28.dp,
            cornerRadius = 14.dp,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                stringResource(Res.string.combat_features_add_shield),
                color = AppPalette.TextSecondary,
                fontSize = AppFontSize.Small,
            )
        }
        if (block.shields.isEmpty() && block.facts.isEmpty()) {
            Text(
                EMPTY_VALUE_DASH,
                color = AppPalette.TextTertiary,
                fontSize = AppFontSize.Small,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), maxLines = 1) {
            block.shields.forEach { DamageTypeShield(it.type, it.state, width = 22.dp) }
        }
    }
}

/** every damage type as a tile, a tap walks its state on and the tick closes the editing */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ShieldEditor(
    cells: List<ShieldCell>,
    send: (CombatRoundEvent) -> Unit,
    onDone: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        cells.forEach { cell ->
            Row(modifier = Modifier.clickable { send(CombatRoundEvent.ShieldTapped(cell.type)) }.padding(2.dp)) {
                DamageTypeShield(cell.type, cell.state, width = 24.dp)
            }
        }
        Text(
            "✓",
            color = AppPalette.Background,
            fontSize = AppFontSize.Medium,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier
                    .defaultMinSize(minHeight = 28.dp)
                    .appCard(
                        shape = AppShapes.Card,
                        background = AppPalette.Gold,
                        border = AppPalette.Gold,
                        onClick = onDone,
                    ).padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

/** the shared sheet of the features, filled with the facts and the shields of the chosen participant */
@Composable
internal fun FeaturesSheetUi(
    sheet: FeaturesSheet,
    send: (CombatRoundEvent) -> Unit,
) {
    val overline =
        sheet.name.uppercase() +
            if (sheet.onlyInFight) " · " + stringResource(Res.string.combat_features_only).uppercase() else ""
    FeaturesBottomSheet(
        overline = overline,
        facts = sheet.facts,
        text = sheet.text,
        shields = sheet.shields,
        actions =
            FeaturesSheetActions(
                onTextChanged = { send(CombatRoundEvent.FactTextChanged(it)) },
                onAdd = { send(CombatRoundEvent.FactAdded) },
                onRemove = { send(CombatRoundEvent.FactRemoved(it)) },
                onShieldChanged = { type, state -> send(CombatRoundEvent.VulnerabilityChanged(type, state)) },
                onClose = { send(CombatRoundEvent.FeaturesClosed) },
            ),
    )
}
