package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppLineHeight
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppDamageShield
import com.lyrismet.incadent.core.designsystem.component.FormFieldLabel
import com.lyrismet.incadent.core.designsystem.component.HoldFrame
import com.lyrismet.incadent.core.designsystem.component.HoldToEditField
import com.lyrismet.incadent.feature.codex.domain.grouping.effectiveVulnerabilities
import com.lyrismet.incadent.feature.codex.domain.grouping.isInheritedVulnerability
import com.lyrismet.incadent.feature.codex.domain.grouping.nextOwnVulnerability
import com.lyrismet.incadent.feature.codex.domain.grouping.shields
import com.lyrismet.incadent.feature.codex.domain.grouping.stateOf
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.presentation.style.shieldColor
import com.lyrismet.incadent.feature.codex.presentation.style.shieldIconPath
import com.lyrismet.incadent.feature.codex.presentation.style.toShieldStyle
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_vulnerabilities_hint
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_vulnerabilities_hint_inherited
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_vulnerabilities_label
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_vulnerabilities_none
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_vulnerabilities_step
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_vulnerabilities_tip_inherited
import dndplayerscodex.shared.generated.resources.entity_sheet_species_vulnerabilities_done
import dndplayerscodex.shared.generated.resources.entity_sheet_species_vulnerabilities_tip_format
import org.jetbrains.compose.resources.stringResource

private const val INHERITED_ALPHA = 0.6f
private val LABEL_WIDTH = 104.dp
private val ResistanceText = Color(0xFFF0B43C)

private fun DamageState.labelColor(): Color =
    when (this) {
        DamageState.VULNERABILITY -> AppPalette.MaroonBright
        DamageState.IMMUNITY -> AppPalette.TextPrimary
        else -> ResistanceText
    }

/** the "уязвимости" row of an npc card - its shields by state, and with [onChange] a hold opens the shield editor */
@Composable
internal fun NpcVulnerabilitySection(
    vulnerabilities: NpcVulnerabilities,
    onChange: ((DamageType, DamageState) -> Unit)?,
    onEdited: ((Map<DamageType, DamageState>) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var before by remember { mutableStateOf<Map<DamageType, DamageState>?>(null) }
    val opened = before
    if (opened != null && onChange != null) {
        NpcVulnerabilityEditor(
            vulnerabilities = vulnerabilities,
            onChange = onChange,
            onDone = {
                before = null
                onEdited?.invoke(opened)
            },
            modifier = modifier,
        )
        return
    }
    val rows: @Composable () -> Unit = { NpcVulnerabilityRows(vulnerabilities) }
    if (onChange == null) {
        rows()
    } else {
        HoldToEditField(
            onHeld = { before = vulnerabilities.own },
            frame = HoldFrame.Description,
            modifier = modifier,
            content = rows,
        )
    }
}

@Composable
private fun NpcVulnerabilityRows(vulnerabilities: NpcVulnerabilities) {
    val effective = effectiveVulnerabilities(vulnerabilities.fromSpecies, vulnerabilities.own)
    val groups = effective.shields().groupBy({ it.second }, { it.first })
    val stateLabels = damageStateLabels()
    var tipped by remember { mutableStateOf<DamageType?>(null) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (groups.isEmpty()) {
            RowLine(stringResource(Res.string.entity_sheet_npc_vulnerabilities_label), AppPalette.TextTertiary) {
                Text("-", color = AppPalette.TextTertiary, fontSize = AppFontSize.Body)
            }
        }
        groups.forEach { (state, types) ->
            RowLine(stateLabels.getValue(state), state.labelColor()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    types.forEach { type ->
                        val inherited = isInheritedVulnerability(vulnerabilities.fromSpecies, vulnerabilities.own, type)
                        Shield(
                            type = type,
                            state = state,
                            modifier =
                                Modifier
                                    .alpha(if (inherited) INHERITED_ALPHA else 1f)
                                    .clickable { tipped = if (tipped == type) null else type },
                        )
                    }
                }
            }
        }
        tipped?.let { TipLine(it, vulnerabilities) }
    }
}

@Composable
private fun RowLine(
    label: String,
    color: Color,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.width(LABEL_WIDTH),
            color = color,
            fontSize = AppFontSize.Body,
            lineHeight = AppLineHeight.Row,
        )
        content()
    }
}

@Composable
private fun Shield(
    type: DamageType,
    state: DamageState,
    modifier: Modifier = Modifier,
) {
    AppDamageShield(
        iconPath = type.shieldIconPath(),
        iconColor = type.shieldColor(),
        style = state.toShieldStyle(),
        modifier = modifier.heightIn(min = 34.dp).padding(horizontal = 5.dp, vertical = 4.dp),
    )
}

/** every damage type as a shield, a tap walks its state through the cycle and the hint says where the next one goes */
@Composable
internal fun NpcVulnerabilityEditor(
    vulnerabilities: NpcVulnerabilities,
    onChange: (DamageType, DamageState) -> Unit,
    onDone: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var last by remember { mutableStateOf<DamageType?>(null) }
    val effective = effectiveVulnerabilities(vulnerabilities.fromSpecies, vulnerabilities.own)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FormFieldLabel(
                text = stringResource(Res.string.entity_sheet_npc_vulnerabilities_label).uppercase(),
                modifier = Modifier.weight(1f),
                fontSize = AppFontSize.Caption,
            )
            if (onDone != null) {
                Text(
                    stringResource(Res.string.entity_sheet_species_vulnerabilities_done),
                    modifier = Modifier.heightIn(min = 40.dp).clickable(onClick = onDone),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppPalette.Gold,
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DamageType.entries.forEach { type ->
                val inherited = isInheritedVulnerability(vulnerabilities.fromSpecies, vulnerabilities.own, type)
                val shown = effective.stateOf(type).takeIf { it != DamageState.NORMAL } ?: DamageState.UNKNOWN
                Shield(
                    type = type,
                    state = shown,
                    modifier =
                        Modifier
                            .alpha(if (inherited) INHERITED_ALPHA else 1f)
                            .clickable {
                                last = type
                                onChange(
                                    type,
                                    nextOwnVulnerability(vulnerabilities.fromSpecies, vulnerabilities.own, type),
                                )
                            },
                )
            }
        }
        val tapped = last
        val hint = editorHint(vulnerabilities, tapped)
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = AppFontSize.Caption),
            color = if (tapped == null) AppPalette.TextTertiary else AppPalette.GoldBright,
        )
    }
}

private fun DamageState.isShield() =
    this == DamageState.VULNERABILITY || this == DamageState.IMMUNITY || this == DamageState.RESISTANCE

/** before any tap how to use the shields, after one what that shield is now and what the next tap makes of it */
@Composable
private fun editorHint(
    vulnerabilities: NpcVulnerabilities,
    tapped: DamageType?,
): String {
    val species = vulnerabilities.fromSpecies
    if (tapped == null) {
        val base = stringResource(Res.string.entity_sheet_npc_vulnerabilities_hint)
        val name = vulnerabilities.speciesName?.takeIf { species.shields().isNotEmpty() }
        return if (name ==
            null
        ) {
            base
        } else {
            base + stringResource(Res.string.entity_sheet_npc_vulnerabilities_hint_inherited, name)
        }
    }
    val stateLabels = damageStateLabels()
    val none = stringResource(Res.string.entity_sheet_npc_vulnerabilities_none)
    val after = vulnerabilities.own + (tapped to nextOwnVulnerability(species, vulnerabilities.own, tapped))

    fun wordOf(own: Map<DamageType, DamageState>) =
        effectiveVulnerabilities(species, own)
            .stateOf(tapped)
            .takeIf {
                it.isShield()
            }?.let { stateLabels.getValue(it).lowercase() }
            ?: none
    return stringResource(
        Res.string.entity_sheet_npc_vulnerabilities_step,
        damageTypeLabels().getValue(tapped),
        wordOf(vulnerabilities.own),
        wordOf(after),
    )
}

@Composable
private fun TipLine(
    type: DamageType,
    vulnerabilities: NpcVulnerabilities,
) {
    val stateLabels = damageStateLabels()
    val state = effectiveVulnerabilities(vulnerabilities.fromSpecies, vulnerabilities.own).stateOf(type)
    val from =
        vulnerabilities.speciesName?.takeIf {
            isInheritedVulnerability(vulnerabilities.fromSpecies, vulnerabilities.own, type)
        }
    val typeLabel = damageTypeLabels().getValue(type)
    val tip =
        if (from != null) {
            stringResource(
                Res.string.entity_sheet_npc_vulnerabilities_tip_inherited,
                typeLabel,
                stateLabels.getValue(state).lowercase(),
                from,
            )
        } else {
            stringResource(
                Res.string.entity_sheet_species_vulnerabilities_tip_format,
                typeLabel,
                stateLabels.getValue(state).lowercase(),
            )
        }
    Text(
        tip,
        modifier = Modifier.padding(start = LABEL_WIDTH + 12.dp, bottom = 6.dp),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = AppFontSize.Body, fontWeight = FontWeight.SemiBold),
        color = AppPalette.GoldBright,
    )
}
