package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.icons.sheet.SheetIcons
import com.lyrismet.incadent.core.designsystem.numeral
import com.lyrismet.incadent.core.format.EMPTY_VALUE_DASH
import com.lyrismet.incadent.core.format.orDash
import com.lyrismet.incadent.core.format.signed
import com.lyrismet.incadent.feature.codex.domain.model.Proficiency
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_popover_adjust
import dndplayerscodex.shared.generated.resources.me_popover_adjustment
import dndplayerscodex.shared.generated.resources.me_popover_base
import dndplayerscodex.shared.generated.resources.me_popover_by_level
import dndplayerscodex.shared.generated.resources.me_popover_dexterity
import dndplayerscodex.shared.generated.resources.me_popover_initiative
import dndplayerscodex.shared.generated.resources.me_popover_level
import dndplayerscodex.shared.generated.resources.me_popover_passive
import dndplayerscodex.shared.generated.resources.me_popover_proficiency
import dndplayerscodex.shared.generated.resources.me_popover_skill
import dndplayerscodex.shared.generated.resources.me_popover_wisdom
import dndplayerscodex.shared.generated.resources.me_proficiency_expertise
import dndplayerscodex.shared.generated.resources.me_proficiency_none
import dndplayerscodex.shared.generated.resources.me_proficiency_proficient
import dndplayerscodex.shared.generated.resources.me_tile_armor_class
import dndplayerscodex.shared.generated.resources.me_tile_initiative
import dndplayerscodex.shared.generated.resources.me_tile_passive
import dndplayerscodex.shared.generated.resources.me_tile_proficiency
import dndplayerscodex.shared.generated.resources.me_tile_speed
import org.jetbrains.compose.resources.stringResource

/** the five numbers under the hit points, armor class and speed open a keypad, the others explain themselves */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MeTiles(
    character: MeCharacter,
    onEvent: (MeEvent) -> Unit,
) {
    val numbers = character.numbers
    // a big system font puts the tiles in two rows, so no label is cut
    val perRow = if (LocalDensity.current.fontScale > LARGE_FONT_SCALE) TILES_PER_BIG_ROW else TILES_PER_ROW
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            maxItemsInEachRow = perRow,
        ) {
            Tile(
                MeTile.ARMOR_CLASS,
                SheetIcons.Shield,
                numbers.armorClass.orDash(),
                stringResource(Res.string.me_tile_armor_class),
                character,
            ) {
                onEvent(it)
            }
            Tile(
                MeTile.INITIATIVE,
                SheetIcons.Bolt,
                numbers.initiative.signed(),
                stringResource(Res.string.me_tile_initiative),
                character,
            ) {
                onEvent(it)
            }
            Tile(
                MeTile.SPEED,
                SheetIcons.Boot,
                numbers.speed.orDash(),
                stringResource(Res.string.me_tile_speed),
                character,
            ) {
                onEvent(it)
            }
            Tile(
                MeTile.PROFICIENCY,
                SheetIcons.Medal,
                numbers.proficiency.signed(),
                stringResource(Res.string.me_tile_proficiency),
                character,
            ) {
                onEvent(it)
            }
            Tile(
                MeTile.PASSIVE_PERCEPTION,
                SheetIcons.Eye,
                numbers.passivePerception.orDash(),
                stringResource(Res.string.me_tile_passive),
                character,
            ) {
                onEvent(it)
            }
        }
        character.popover?.let { MePopover(it, character, onEvent) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.FlowRowScope.Tile(
    tile: MeTile,
    icon: ImageVector,
    value: String,
    label: String,
    character: MeCharacter,
    onEvent: (MeEvent) -> Unit,
) {
    val selected = character.popover == tile
    val blue = tile == MeTile.ARMOR_CLASS
    Column(
        modifier =
            Modifier
                .weight(1f)
                .heightIn(min = 70.dp)
                .appCard(
                    shape = AppShapes.Field,
                    background = if (blue) AppPalette.Azure.copy(alpha = 0.08f) else AppPalette.Surface,
                    border =
                        when {
                            selected -> AppPalette.Gold
                            blue -> AppPalette.Azure.copy(alpha = 0.3f)
                            else -> AppPalette.BorderSubtle
                        },
                    borderWidth = if (selected) 1.5.dp else 1.dp,
                    onClick = { onEvent(MeEvent.TileTapped(tile)) },
                ).padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Icon16(icon, if (blue) AppPalette.Azure else AppPalette.TextMuted)
        Text(
            value,
            style = AppText.numeral(19.sp),
            color =
                if (value ==
                    EMPTY_VALUE_DASH
                ) {
                    AppPalette.TextTertiary
                } else {
                    AppPalette.TextHeading
                },
        )
        Text(
            label,
            style = AppText.caption.copy(fontSize = 10.sp),
            color = AppPalette.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

private class PopoverContent(
    val title: String,
    val value: String,
    val rows: List<Pair<String, String>>,
)

@Composable
private fun popoverContent(
    tile: MeTile,
    character: MeCharacter,
): PopoverContent {
    val b = character.breakdown
    val numbers = character.numbers
    val adjustment = stringResource(Res.string.me_popover_adjustment)
    return when (tile) {
        MeTile.INITIATIVE ->
            PopoverContent(
                stringResource(Res.string.me_popover_initiative),
                numbers.initiative.signed(),
                listOf(
                    stringResource(Res.string.me_popover_dexterity) to b.dexterityBonus.signed(),
                    adjustment to b.initiativeAdjustment.signed(),
                ),
            )
        MeTile.PROFICIENCY ->
            PopoverContent(
                stringResource(Res.string.me_popover_proficiency),
                numbers.proficiency.signed(),
                listOf(
                    stringResource(Res.string.me_popover_level) to b.level.toString(),
                    stringResource(Res.string.me_popover_by_level) to "",
                ),
            )
        else ->
            PopoverContent(
                stringResource(Res.string.me_popover_passive),
                numbers.passivePerception.orDash(),
                listOf(
                    stringResource(Res.string.me_popover_base) to PASSIVE_BASE,
                    stringResource(Res.string.me_popover_wisdom) to b.wisdomBonus.signed(),
                    stringResource(Res.string.me_popover_skill) to proficiencyLabel(b.perceptionProficiency),
                    adjustment to b.passiveAdjustment.signed(),
                ),
            )
    }
}

private const val PASSIVE_BASE = "10"

@Composable
private fun proficiencyLabel(proficiency: Proficiency): String =
    stringResource(
        when (proficiency) {
            Proficiency.NONE -> Res.string.me_proficiency_none
            Proficiency.PROFICIENT -> Res.string.me_proficiency_proficient
            Proficiency.EXPERTISE -> Res.string.me_proficiency_expertise
        },
    )

/** the card a tile opens to show what its number is made of */
@Composable
private fun MePopover(
    tile: MeTile,
    character: MeCharacter,
    onEvent: (MeEvent) -> Unit,
) {
    val content = popoverContent(tile, character)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(background = AppPalette.SurfacePopover, border = AppPalette.BorderPopover)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                content.title,
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = AppPalette.TextHeading,
            )
            Text(content.value, style = AppText.numeral(22.sp), color = AppPalette.TextHeading)
        }
        content.rows.forEach { (label, text) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextMuted)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextPrimary)
            }
        }
        if (tile != MeTile.PROFICIENCY && !character.readOnly) {
            AppButton(
                text = stringResource(Res.string.me_popover_adjust),
                onClick = { onEvent(MeEvent.AdjustmentRequested) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
