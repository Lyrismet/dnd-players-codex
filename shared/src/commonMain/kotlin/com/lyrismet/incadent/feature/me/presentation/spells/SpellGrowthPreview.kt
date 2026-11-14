package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.toRomanNumeral
import com.lyrismet.incadent.feature.codex.domain.model.CANTRIP_TIERS
import com.lyrismet.incadent.feature.codex.domain.model.CantripGrowth
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.GrowthTarget
import com.lyrismet.incadent.feature.codex.domain.model.SpellAt
import com.lyrismet.incadent.feature.codex.domain.model.atCharacterLevel
import com.lyrismet.incadent.feature.codex.domain.model.atCircle
import com.lyrismet.incadent.feature.codex.domain.model.cantripTier
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_growth_targets_extra
import incadent.shared.generated.resources.me_spells_growth_tier_last
import incadent.shared.generated.resources.me_spells_growth_tier_range
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** what the spell comes to on each level or circle as tiles, the one the character is on is gold, a change violet */
@Composable
internal fun GrowthPreview(
    spell: CharacterSpell,
    characterLevel: Int,
    maxCircle: Int,
) {
    val ats =
        remember(spell, maxCircle) {
            if (spell.isCantrip) {
                (listOf(1) + CANTRIP_TIERS).map { spell.atCharacterLevel(it) }
            } else {
                (spell.level..maxCircle.coerceAtLeast(spell.level)).map { spell.atCircle(it) }
            }
        }
    val tiers = listOf(1) + CANTRIP_TIERS
    val labels =
        if (spell.isCantrip) {
            tiers.mapIndexed { index, from ->
                if (index == tiers.lastIndex) {
                    stringResource(Res.string.me_spells_growth_tier_last, from)
                } else {
                    stringResource(Res.string.me_spells_growth_tier_range, from, tiers[index + 1] - 1)
                }
            }
        } else {
            ats.indices.map { (spell.level + it).toRomanNumeral() }
        }
    val current = if (spell.isCantrip) cantripTier(characterLevel) else -1
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ats.forEachIndexed { index, at ->
            val now = index == current
            val border =
                when {
                    now -> AppPalette.Gold
                    index > 0 && at != ats[index - 1] -> AppPalette.Arcane
                    else -> AppPalette.BorderSubtle
                }
            GrowthTile(labels[index], spell, at, border, now)
        }
    }
}

@Composable
private fun GrowthTile(
    label: String,
    spell: CharacterSpell,
    at: SpellAt,
    border: Color,
    now: Boolean,
) {
    Column(
        Modifier
            .widthIn(min = TileWidth)
            .appCard(AppShapes.Field, AppPalette.Background, border)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = AppText.caption, color = if (now) AppPalette.GoldBright else AppPalette.TextTertiary)
        AtSummary(spell, at)
    }
}

/** what changes in one tile: the growing damage and projectiles, the duration, the targets, the range, the notes */
@Composable
private fun AtSummary(
    spell: CharacterSpell,
    at: SpellAt,
) {
    val rules = spell.growth.rules
    val targets = rules.any { it.target == GrowthTarget.TARGETS }
    val duration = rules.any { it.target == GrowthTarget.DURATION }
    val range = rules.any { it.target == GrowthTarget.RANGE }
    val growingRows =
        if (spell.isCantrip) {
            at.damage
        } else {
            at.damage.filterIndexed { i, _ -> rules.any { it.target == GrowthTarget.DICE && it.damageIndex == i } }
        }
    val projectiles = !spell.isCantrip && rules.any { it.target == GrowthTarget.PROJECTILES }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (projectiles) Text("${at.projectiles} ×", style = AppText.footnote, color = AppPalette.TextPrimary)
        DamageLine(growingRows, if (spell.isCantrip) at.projectiles else 1, AppText.footnote)
        if (duration) {
            Text(
                "◎ " + durationLabel(at.durationUnit, at.durationValue),
                style = AppText.footnote,
                color = AppPalette.TextPrimary,
            )
        }
        if (targets) {
            Text(
                pluralStringResource(Res.plurals.me_spells_growth_targets_extra, at.extraTargets, at.extraTargets),
                style = AppText.footnote,
                color = AppPalette.TextPrimary,
            )
        }
        if (range) Text(at.range, style = AppText.footnote, color = AppPalette.TextPrimary)
        at.notes.forEach { Text(it, style = AppText.footnote, color = AppPalette.TextPrimary, maxLines = 1) }
    }
}

private val TileWidth = 64.dp

/** whether a cantrip is set to grow, the arrow of the list says the numbers are counted by the level */
internal val CharacterSpell.growsWithLevel: Boolean
    get() = isCantrip && growth.cantrip != CantripGrowth.NONE
