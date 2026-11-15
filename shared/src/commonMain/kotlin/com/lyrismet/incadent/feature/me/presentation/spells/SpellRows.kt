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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.atRank

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
        Text("›", style = MaterialTheme.typography.titleMedium, color = AppPalette.TextTertiary)
    }
}

// damage with the range when the spell has damage, else the school, the range and the duration
@Composable
private fun DetailLine(
    spell: CharacterSpell,
    characterLevel: Int,
) {
    val range = spell.range.trim()
    val at = spell.atRank(characterLevel)
    if (at.damage.any { it.dice.isNotBlank() }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DamageLine(at.damage, at.projectiles, AppText.caption)
            if (spell.growsWithLevel) Text("↑", style = AppText.caption, color = AppPalette.Arcane)
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
