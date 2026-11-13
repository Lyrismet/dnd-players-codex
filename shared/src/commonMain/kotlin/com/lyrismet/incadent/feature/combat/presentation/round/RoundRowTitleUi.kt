package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppLineHeight
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.Dot
import com.lyrismet.incadent.core.designsystem.component.dashedBorder
import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.feature.combat.domain.round.fitRole
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_link_battle_only
import incadent.shared.generated.resources.combat_queue_moves
import incadent.shared.generated.resources.combat_round_me
import org.jetbrains.compose.resources.stringResource

private val ROLE_INDENT = 12.dp
private val NAME_GAP = 6.dp
private val BATTLE_ONLY_PADDING = 5.dp
private val BATTLE_ONLY_DOT = 9.dp

@Composable
internal fun RoundRowTitle(row: RoundRow) {
    BoxWithConstraints {
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val base = LocalTextStyle.current
        val width = maxWidth

        fun measured(
            text: String,
            size: TextUnit,
            weight: FontWeight?,
        ): Dp =
            with(density) {
                measurer
                    .measure(text, base.copy(fontSize = size, fontWeight = weight))
                    .size.width
                    .toDp()
            }
        Column {
            RowNameLine(row, width, ::measured)
            RowRoleLine(row, width - ROLE_INDENT) { text -> measured(text, AppFontSize.Small, null) }
        }
    }
}

@Composable
private fun RowNameLine(
    row: RoundRow,
    width: Dp,
    measured: (String, TextUnit, FontWeight?) -> Dp,
) {
    val battleOnly = stringResource(Res.string.combat_link_battle_only)
    val name = measured(row.name, AppFontSize.MediumPlus, FontWeight.SemiBold)
    val me = stringResource(Res.string.combat_round_me)
    val meWidth = if (row.own) measured(me, AppFontSize.Small, FontWeight.Bold) + NAME_GAP else 0.dp
    // the dashed label shrinks to a dashed circle before the name is cut
    val shrunk =
        row.link == RowLink.BATTLE_ONLY &&
            name + (measured(battleOnly, AppFontSize.Tiny, FontWeight.SemiBold) + BATTLE_ONLY_PADDING * 2) + meWidth +
            ROLE_INDENT +
            NAME_GAP >
            width
    Row(horizontalArrangement = Arrangement.spacedBy(NAME_GAP), verticalAlignment = Alignment.CenterVertically) {
        Dot(size = 6.dp, color = RoundColors.side(row.side))
        if (row.current) MovesPlaque()
        Text(
            text = row.name,
            color = AppPalette.TextPrimary,
            fontSize = AppFontSize.MediumPlus,
            lineHeight = AppLineHeight.Row,
            fontWeight = FontWeight.SemiBold,
            textDecoration = if (row.struck) TextDecoration.LineThrough else TextDecoration.None,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (row.own) Text(me, color = AppPalette.Gold, fontSize = AppFontSize.Small, fontWeight = FontWeight.Bold)
        when (row.link) {
            RowLink.CODEX -> CodexMark()
            RowLink.BATTLE_ONLY -> BattleOnlyMark(battleOnly, shrunk)
            RowLink.NONE -> Unit
        }
        row.lastHit?.let {
            Text(it, color = AppPalette.MaroonBright, fontSize = AppFontSize.Small, fontWeight = FontWeight.Bold)
        }
    }
}

/** the gold label of the one who moves, it is always gold and never animated */
private val PLAQUE_HEIGHT = 17.dp
private val PLAQUE_TEXT = 9.5.sp
private val PLAQUE_SPACING = 1.14.sp

@Composable
private fun MovesPlaque() {
    Box(
        modifier =
            Modifier
                .height(
                    PLAQUE_HEIGHT,
                ).background(AppPalette.Gold, AppShapes.Badge)
                .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.combat_queue_moves),
            color = AppPalette.Background,
            fontSize = PLAQUE_TEXT,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = PLAQUE_SPACING,
            lineHeight = PLAQUE_TEXT,
            style = TextStyle(lineHeightStyle = CenteredLine),
            maxLines = 1,
        )
    }
}

@Composable
internal fun BattleOnlyMark(
    text: String,
    shrunk: Boolean,
) {
    if (shrunk) {
        Box(
            Modifier
                .size(
                    BATTLE_ONLY_DOT,
                ).dashedBorder(
                    AppPalette.TextTertiary,
                    cornerRadius = BATTLE_ONLY_DOT,
                    dashLength = 2.dp,
                    dashGap = 2.dp,
                ),
        )
    } else {
        Text(
            text = text,
            color = AppPalette.TextMuted,
            fontSize = AppFontSize.Tiny,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier =
                Modifier
                    .dashedBorder(TEMP_BORDER, cornerRadius = 6.dp, dashLength = 3.dp, dashGap = 2.dp)
                    .padding(horizontal = BATTLE_ONLY_PADDING, vertical = 1.dp),
        )
    }
}

@Composable
private fun RowRoleLine(
    row: RoundRow,
    width: Dp,
    measured: (String) -> Dp,
) {
    fun told(role: String) = joinWithDot(role, row.roleTail)
    val role = row.roleHint?.let(::told) ?: told(fitRole(row.role) { measured(told(it)) <= width })
    Text(
        text = role,
        color = if (row.roleHint != null) AppPalette.TextTertiary else AppPalette.TextSecondary,
        fontStyle = if (row.roleHint != null) FontStyle.Italic else FontStyle.Normal,
        fontSize = AppFontSize.Small,
        lineHeight = AppLineHeight.Row,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = ROLE_INDENT),
    )
}

private const val DIAMOND_TURN = 45f
private val DIAMOND = Color(0xFF8C7A3E)
private val TEMP_BORDER = Color(0xFF5A5E6E)

/** the small hollow diamond of a participant that comes from the codex, a creature of a species has none */
@Composable
internal fun CodexMark(size: Dp = 6.dp) {
    Box(
        Modifier
            .padding(horizontal = 2.dp)
            .size(size)
            .rotate(DIAMOND_TURN)
            .border(1.3.dp, DIAMOND),
    )
}
