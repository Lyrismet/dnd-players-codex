package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppDamageShield
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.HoldFrame
import com.lyrismet.incadent.core.designsystem.component.HoldToEditField
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.dashedTopRule
import com.lyrismet.incadent.feature.codex.domain.grouping.shields
import com.lyrismet.incadent.feature.codex.domain.grouping.stateOf
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.presentation.style.shieldColor
import com.lyrismet.incadent.feature.codex.presentation.style.shieldIconPath
import com.lyrismet.incadent.feature.codex.presentation.style.toShieldStyle
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_species_damage_done
import dndplayerscodex.shared.generated.resources.entity_sheet_species_damage_empty
import dndplayerscodex.shared.generated.resources.entity_sheet_species_damage_hint
import dndplayerscodex.shared.generated.resources.entity_sheet_species_damage_tip_format
import dndplayerscodex.shared.generated.resources.entity_sheet_species_damage_title
import org.jetbrains.compose.resources.stringResource

private const val GRID_COLUMNS = 3

private val LegendStates = listOf(DamageState.VULNERABILITY, DamageState.IMMUNITY, DamageState.RESISTANCE)

/** the "сопротивления" block of a species card - its shields, and with [onChange] a hold turns them into the grid */
@Composable
internal fun SpeciesDamageSection(
    damage: Map<DamageType, DamageState>,
    onChange: ((DamageType, DamageState) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf(false) }
    val title = stringResource(Res.string.entity_sheet_species_damage_title).uppercase()
    if (editing && onChange != null) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FormFieldLabel(text = title, fontSize = AppFontSize.Caption)
            DamageGridEditor(damage, onChange)
            Text(
                stringResource(Res.string.entity_sheet_species_damage_done),
                modifier = Modifier.heightIn(min = 40.dp).clickable { editing = false },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AppPalette.Gold,
            )
        }
        return
    }
    val shields: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            FormFieldLabel(text = title, fontSize = AppFontSize.Caption)
            DamageShieldsView(damage)
        }
    }
    Box(modifier = modifier) {
        if (onChange == null) {
            shields()
        } else {
            HoldToEditField(onHeld = { editing = true }, frame = HoldFrame.Description, content = shields)
        }
    }
}

/** the shields a species has marks for, a tap on one names it, or a hint to edit when there are none */
@Composable
private fun DamageShieldsView(damage: Map<DamageType, DamageState>) {
    val marks = damage.shields()
    if (marks.isEmpty()) {
        Text(
            stringResource(Res.string.entity_sheet_species_damage_empty),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = AppFontSize.Body),
            color = AppPalette.TextSecondary,
        )
        return
    }
    val typeLabels = damageTypeLabels()
    val stateLabels = damageStateLabels()
    var tipped by remember { mutableStateOf<DamageType?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            marks.forEach { (type, state) ->
                AppDamageShield(
                    iconPath = type.shieldIconPath(),
                    iconColor = type.shieldColor(),
                    style = state.toShieldStyle(),
                    modifier =
                        Modifier
                            .heightIn(min = 44.dp)
                            .clickable { tipped = if (tipped == type) null else type }
                            .padding(horizontal = 5.dp, vertical = 4.dp),
                )
            }
        }
        tipped?.let { type ->
            Text(
                stringResource(
                    Res.string.entity_sheet_species_damage_tip_format,
                    typeLabels.getValue(type),
                    stateLabels.getValue(damage.stateOf(type)).lowercase(),
                ),
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontSize = AppFontSize.Body,
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = AppPalette.GoldBright,
            )
        }
        ShieldLegend()
    }
}

@Composable
private fun ShieldLegend() {
    val stateLabels = damageStateLabels()
    FlowRow(
        modifier = Modifier.fillMaxWidth().dashedTopRule(AppPalette.Border).padding(top = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LegendStates.forEach { state ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AppDamageShield(
                    iconPath = "",
                    iconColor = AppPalette.TextSecondary,
                    style = state.toShieldStyle(),
                    width = 14.dp,
                )
                Text(
                    stateLabels.getValue(state),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = AppFontSize.CaptionPlus),
                    color = AppPalette.TextSecondary,
                )
            }
        }
    }
}

/** all damage types as a grid, a tap on one opens the five states below it */
@Composable
internal fun DamageGridEditor(
    damage: Map<DamageType, DamageState>,
    onChange: (DamageType, DamageState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeLabels = damageTypeLabels()
    val stateLabels = damageStateLabels()
    var open by remember { mutableStateOf<DamageType?>(null) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DamageType.entries.chunked(GRID_COLUMNS).forEach { rowTypes ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                rowTypes.forEach { type ->
                    DamageTile(
                        label = typeLabels.getValue(type),
                        type = type,
                        state = damage.stateOf(type),
                        isOpen = open == type,
                        onClick = { open = if (open == type) null else type },
                        modifier = Modifier.weight(1f),
                    )
                }
                // a short last row keeps its tiles the width of the others
                repeat(GRID_COLUMNS - rowTypes.size) { Spacer(Modifier.weight(1f)) }
            }
            open?.takeIf { it in rowTypes }?.let { type ->
                DamageStatePicker(
                    type = type,
                    title = typeLabels.getValue(type),
                    current = damage.stateOf(type),
                    labels = stateLabels,
                    onPick = {
                        onChange(type, it)
                        open = null
                    },
                )
            }
        }
        Text(
            stringResource(Res.string.entity_sheet_species_damage_hint),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = AppFontSize.Caption),
            color = AppPalette.TextTertiary,
        )
    }
}

@Composable
private fun DamageTile(
    label: String,
    type: DamageType,
    state: DamageState,
    isOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .heightIn(min = 44.dp)
                .appCard(
                    shape = AppShapes.Tile,
                    background = if (isOpen) AppPalette.SurfacePopover else AppPalette.Background,
                    border = if (isOpen) AppPalette.Gold.copy(alpha = 0.6f) else AppPalette.Border,
                    onClick = onClick,
                ).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppDamageShield(type.shieldIconPath(), type.shieldColor(), state.toShieldStyle(), width = 22.dp)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = AppFontSize.Small),
            color = if (state == DamageState.UNKNOWN) AppPalette.TextSecondary else AppPalette.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DamageStatePicker(
    type: DamageType,
    title: String,
    current: DamageState,
    labels: Map<DamageState, String>,
    onPick: (DamageState) -> Unit,
) {
    val frame = RoundedCornerShape(12.dp)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .shadow(8.dp, frame)
                .appCard(
                    shape = frame,
                    background = AppPalette.SurfacePopover,
                    border = AppPalette.Gold.copy(alpha = 0.45f),
                ).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.4.sp,
            color = AppPalette.GoldBright,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DamageState.entries.forEach { state ->
                val isCurrent = state == current
                Row(
                    modifier =
                        Modifier
                            .heightIn(min = 40.dp)
                            .appCard(
                                shape = AppShapes.Tile,
                                background = if (isCurrent) AppPalette.SurfaceElevated else Color.Transparent,
                                border = if (isCurrent) AppPalette.Gold.copy(alpha = 0.6f) else AppPalette.Border,
                                onClick = { onPick(state) },
                            ).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    AppDamageShield(type.shieldIconPath(), type.shieldColor(), state.toShieldStyle(), width = 20.dp)
                    Text(
                        labels.getValue(state),
                        fontSize = AppFontSize.Body,
                        fontWeight = FontWeight.Medium,
                        color = if (isCurrent) AppPalette.TextHeading else AppPalette.TextPrimary,
                    )
                }
            }
        }
    }
}
