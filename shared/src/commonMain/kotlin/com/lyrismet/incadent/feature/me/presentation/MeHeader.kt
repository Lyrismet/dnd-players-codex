package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.designsystem.component.PortraitBadge
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.icons.sheet.SheetIcons
import com.lyrismet.incadent.core.designsystem.numeral
import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.core.format.orDash
import com.lyrismet.incadent.feature.codex.domain.grouping.classLine
import com.lyrismet.incadent.feature.me.domain.isDown
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.campaign_player_level_format
import incadent.shared.generated.resources.me_fallen_line
import incadent.shared.generated.resources.me_hp_label
import incadent.shared.generated.resources.me_hp_of_format
import incadent.shared.generated.resources.me_hp_temp_format
import incadent.shared.generated.resources.me_hp_zero
import incadent.shared.generated.resources.me_menu_description
import incadent.shared.generated.resources.me_menu_edit
import incadent.shared.generated.resources.me_menu_fill
import incadent.shared.generated.resources.me_menu_not_mine
import incadent.shared.generated.resources.me_menu_spells_hide
import incadent.shared.generated.resources.me_menu_spells_show
import org.jetbrains.compose.resources.stringResource

private val PartyBlue = StatusColor(AppPalette.Azure, AppPalette.Surface, AppPalette.PartyRowBorder)

@Composable
internal fun MeNameRow(
    character: MeCharacter,
    onEvent: (MeEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PortraitBadge(
                portrait = character.portrait,
                color = PartyBlue,
                size = 52.dp,
                shape = RoundedCornerShape(26.dp, 26.dp, 8.dp, 8.dp),
            ) {
                Text(
                    character.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = AppPalette.Azure,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    character.name,
                    style = MaterialTheme.typography.headlineLarge,
                    color = AppPalette.TextHeading,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(characterLine(character), style = AppText.footnote, color = AppPalette.TextSecondary)
            }
            MenuButton(character.menuOpen) { onEvent(MeEvent.MenuToggled) }
        }
        if (character.fallen) FallenNote()
        if (character.menuOpen) MeMenu(character.readOnly, character.spellsHidden, onEvent)
    }
}

@Composable
private fun characterLine(character: MeCharacter): String {
    val level = character.level?.let { stringResource(Res.string.campaign_player_level_format, it) }
    return joinWithDot(character.race, classLine(character.characterClass, character.subclass), level.orEmpty())
}

@Composable
private fun MenuButton(
    open: Boolean,
    onClick: () -> Unit,
) {
    val description = stringResource(Res.string.me_menu_description)
    Box(
        modifier =
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (open) AppPalette.SurfaceElevated else AppPalette.Surface)
                .border(1.5.dp, if (open) AppPalette.GoldDim else AppPalette.BorderSubtle, CircleShape)
                .clickable(onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text("⋯", color = AppPalette.TextMuted, fontSize = 16.sp)
    }
}

@Composable
private fun FallenNote() {
    Text(
        stringResource(Res.string.me_fallen_line),
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.DeadBackground,
                    border = AppPalette.BorderHover,
                ).padding(horizontal = 12.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = AppPalette.TextMuted,
    )
}

@Composable
private fun MeMenu(
    readOnly: Boolean,
    spellsHidden: Boolean,
    onEvent: (MeEvent) -> Unit,
) {
    Column(Modifier.fillMaxWidth().appCard(background = AppPalette.SurfacePopover, border = AppPalette.BorderPopover)) {
        if (!readOnly) {
            MenuRow(
                stringResource(Res.string.me_menu_edit),
                AppPalette.TextPrimary,
            ) { onEvent(MeEvent.Menu(MenuAction.EDIT)) }
            MenuRow(
                stringResource(Res.string.me_menu_fill),
                AppPalette.TextPrimary,
            ) { onEvent(MeEvent.Menu(MenuAction.FILL)) }
            MenuRow(
                stringResource(
                    if (spellsHidden) Res.string.me_menu_spells_show else Res.string.me_menu_spells_hide,
                ),
                AppPalette.TextPrimary,
            ) { onEvent(MeEvent.Menu(MenuAction.TOGGLE_SPELLS)) }
            MenuRow(stringResource(Res.string.me_menu_not_mine), AppPalette.MaroonBright) {
                onEvent(MeEvent.Menu(MenuAction.NOT_MINE))
            }
        }
    }
}

@Composable
private fun MenuRow(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Text(
        text,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(
                    min = 48.dp,
                ).clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 13.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = color,
    )
}

/** the hit points: current over max, temporary ones and a bar, a press opens the keypad */
@Composable
internal fun MeHitPointsCard(
    character: MeCharacter,
    onEvent: (MeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vitals = character.vitals
    val down = vitals.isDown
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Card,
                    border = if (down) AppPalette.Maroon else AppPalette.BorderSubtle,
                    onClick = { onEvent(MeEvent.Hp(HitPointsEvent.Opened)) },
                ).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon16(SheetIcons.Heart, if (down) AppPalette.MaroonBright else AppPalette.EmeraldBright)
            Text(
                stringResource(Res.string.me_hp_label).uppercase(),
                style = AppText.fieldOverline,
                color = AppPalette.TextTertiary,
            )
            Text(
                vitals.hpCurrent.orDash(),
                style = AppText.numeral(28.sp),
                color = if (down) AppPalette.MaroonBright else AppPalette.TextHeading,
            )
            Text(
                stringResource(Res.string.me_hp_of_format, vitals.hpMax.orDash()),
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextSecondary,
            )
            if (vitals.tempHp > 0) {
                Text(
                    stringResource(Res.string.me_hp_temp_format, vitals.tempHp),
                    modifier =
                        Modifier
                            .clip(
                                AppShapes.Small,
                            ).background(AppPalette.Azure.copy(alpha = 0.14f))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppPalette.Azure,
                )
            }
            if (down) {
                Text(
                    stringResource(Res.string.me_hp_zero),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppPalette.MaroonBright,
                )
            }
        }
        HitPointsBar(vitals.hpCurrent ?: 0, vitals.hpMax ?: 0, vitals.tempHp)
    }
}

@Composable
internal fun Icon16(
    icon: ImageVector,
    tint: Color,
) {
    androidx.compose.material3.Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
}

@Composable
private fun HitPointsBar(
    current: Int,
    max: Int,
    temp: Int,
) {
    val total = (max + temp).coerceAtLeast(1).toFloat()
    Row(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(AppPalette.BorderSubtle),
    ) {
        if (current > 0) Box(Modifier.weight(current / total).fillMaxHeight().background(AppPalette.EmeraldBright))
        if (temp > 0) Box(Modifier.weight(temp / total).fillMaxHeight().background(AppPalette.Azure))
        val rest = total - current - temp
        if (rest > 0) Box(Modifier.weight(rest / total).fillMaxHeight())
    }
}
