package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_card_components
import incadent.shared.generated.resources.me_spells_card_concentration
import incadent.shared.generated.resources.me_spells_card_duration
import incadent.shared.generated.resources.me_spells_card_range
import incadent.shared.generated.resources.me_spells_card_ritual
import incadent.shared.generated.resources.me_spells_card_time
import incadent.shared.generated.resources.me_spells_component_m
import incadent.shared.generated.resources.me_spells_component_s
import incadent.shared.generated.resources.me_spells_component_v
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Badges(spell: CharacterSpell) {
    if (!spell.concentration && !spell.ritual) return
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (spell.concentration) {
            Badge("◎ " + stringResource(Res.string.me_spells_card_concentration), AppPalette.GoldBright)
        }
        if (spell.ritual) Badge(stringResource(Res.string.me_spells_card_ritual), AppPalette.Arcane)
    }
}

@Composable
private fun Badge(
    text: String,
    color: Color,
) {
    Text(
        text,
        modifier =
            Modifier
                .appCard(
                    background = color.copy(alpha = 0.1f),
                    border = color.copy(alpha = 0.55f),
                ).padding(horizontal = 12.dp, vertical = 6.dp),
        style = AppText.footnote,
        color = color,
    )
}

// empty fields are left out so a card of a short spell stays short
@Composable
internal fun CardGrid(spell: CharacterSpell) {
    val cells =
        listOf(
            stringResource(Res.string.me_spells_card_time) to spell.castingTime,
            stringResource(Res.string.me_spells_card_range) to spell.range,
            stringResource(Res.string.me_spells_card_components) to if (spell.hasComponents) "x" else "",
            stringResource(Res.string.me_spells_card_duration) to spell.duration,
        ).filter { it.second.isNotBlank() }
    if (cells.isEmpty()) return
    Column(Modifier.fillMaxWidth().appCard(background = AppPalette.Background, border = AppPalette.BorderSubtle)) {
        cells.chunked(2).forEachIndexed { index, row ->
            if (index > 0) AppDivider()
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                row.forEachIndexed { column, (label, value) ->
                    if (column > 0) VerticalLine()
                    Column(Modifier.weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionOverline(label, color = AppPalette.TextTertiary)
                        if (value ==
                            "x"
                        ) {
                            Components(spell)
                        } else {
                            Text(value, style = MaterialTheme.typography.bodyLarge, color = AppPalette.TextHeading)
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun VerticalLine() {
    Box(Modifier.fillMaxHeight().width(1.dp).background(AppPalette.BorderSubtle))
}

// the three letters, a lit one is on the spell and the material is written under them
@Composable
private fun Components(spell: CharacterSpell) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Letter(stringResource(Res.string.me_spells_component_v), spell.verbal)
        Letter(stringResource(Res.string.me_spells_component_s), spell.somatic)
        Letter(stringResource(Res.string.me_spells_component_m), spell.hasMaterial)
    }
    if (spell.hasMaterial &&
        spell.material.isNotBlank()
    ) {
        Text(spell.material, style = AppText.footnote, color = AppPalette.TextSecondary)
    }
}

@Composable
private fun Letter(
    text: String,
    on: Boolean,
) {
    Box(
        Modifier
            .size(width = 24.dp, height = 24.dp)
            .appCard(
                background = if (on) AppPalette.Arcane.copy(alpha = 0.12f) else Color.Transparent,
                border = if (on) AppPalette.Arcane else AppPalette.BorderSubtle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppText.caption, color = if (on) AppPalette.Arcane else AppPalette.TextTertiary)
    }
}
