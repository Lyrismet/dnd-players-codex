package com.lyrismet.incadent.feature.me.presentation.spells

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.SpellEffect
import com.lyrismet.incadent.feature.codex.domain.model.atShown
import com.lyrismet.incadent.feature.codex.domain.model.pinnedCircle
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_row_free
import org.jetbrains.compose.resources.stringResource

/** the spells of one level in one dark box, a line between them */
@Composable
internal fun SpellList(
    list: List<CharacterSpell>,
    characterLevel: Int,
    send: (SpellsEvent) -> Unit,
) {
    Column(Modifier.fillMaxWidth().appCard(background = AppPalette.Background, border = AppPalette.BorderSubtle)) {
        list.forEachIndexed { index, spell ->
            if (index > 0) AppDivider()
            SpellRow(spell, characterLevel, send)
        }
    }
}

/** the name with its meta line under it, a press on the row opens the card */
@Composable
private fun SpellRow(
    spell: CharacterSpell,
    characterLevel: Int,
    send: (SpellsEvent) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    background = AppPalette.Background,
                    border = null,
                    onClick = { send(SpellsEvent.CardOpened(spell.id)) },
                ).padding(start = 12.dp, end = 12.dp)
                .heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CastKindGlyph(spell.castKind)
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    spell.name,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppPalette.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (spell.concentration) Text("◎", style = AppText.caption, color = AppPalette.GoldBright)
            }
            DetailLine(spell, characterLevel)
        }
        if (!spell.isCantrip) CircleMark(spell)
        Text("›", style = MaterialTheme.typography.titleMedium, color = AppPalette.TextTertiary)
    }
}

/** the circle of the spell in big letters, a pinned one shows its own circle small, an arrow and the pinned one lit */
@Composable
private fun CircleMark(spell: CharacterSpell) {
    val pinned = spell.pinnedCircle
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (pinned == null) {
            Text(
                spell.level.toRomanNumeral(),
                style = MaterialTheme.typography.titleMedium,
                color = AppPalette.TextSecondary,
            )
        } else {
            Text(spell.level.toRomanNumeral(), style = AppText.caption, color = AppPalette.TextTertiary)
            Text("→", style = AppText.caption, color = AppPalette.TextTertiary)
            Text(
                pinned.toRomanNumeral(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = AppPalette.GoldBright,
            )
        }
    }
}

// damage with the range when the spell has damage, else the school, the range and the duration
@Composable
private fun DetailLine(
    spell: CharacterSpell,
    characterLevel: Int,
) {
    val range = spell.range.trim()
    val at = spell.atShown(characterLevel)
    if (spell.hasFreeCasts) {
        val free = stringResource(Res.string.me_spells_row_free, spell.freeLeft, spell.freeMax)
        Text(
            listOf(free, range).filter { it.isNotEmpty() }.joinToString(" · "),
            style = AppText.caption,
            color = AppPalette.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    } else if (at.damage.any { it.dice.isNotBlank() || it.effect is SpellEffect.Condition }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DamageLine(at.damage, at.projectiles, AppText.caption)
            StatesChip(at.damage.mapNotNull { it.effect as? SpellEffect.Condition })
            if (spell.growsWithLevel) {
                Text(
                    "↑" + (spell.pinnedCircle?.toRomanNumeral().orEmpty()),
                    style = AppText.caption,
                    color = AppPalette.Arcane,
                )
            }
            if (range.isNotEmpty()) {
                Text(
                    "· $range",
                    style = AppText.caption,
                    color = AppPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    } else {
        val duration = durationLabel(spell)
        val line =
            listOf(
                spell.school.trim(),
                range,
                if (duration.isNotEmpty() && spell.concentration) "◎ $duration" else duration,
            ).filter { it.isNotEmpty() }.joinToString(" · ")
        if (line.isNotEmpty()) {
            Text(
                line,
                style = AppText.caption,
                color = AppPalette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatesChip(states: List<SpellEffect.Condition>) {
    if (states.isEmpty()) return
    SpellEffectGlyph(states.first(), size = 12.dp)
    Text(
        states.joinToString(", ") { effectLabel(it) },
        style = AppText.caption,
        color = AppPalette.TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
