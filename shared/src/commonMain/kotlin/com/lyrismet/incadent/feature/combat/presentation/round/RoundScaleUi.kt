package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSectionTitleHint
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.appCard
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_panel_mark_title
import incadent.shared.generated.resources.hp_exact_change
import incadent.shared.generated.resources.hp_exact_label
import incadent.shared.generated.resources.hp_exact_none
import incadent.shared.generated.resources.hp_exact_set
import incadent.shared.generated.resources.hp_help_body
import incadent.shared.generated.resources.hp_help_title
import incadent.shared.generated.resources.hp_scale_estimate_title
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private const val PERMILLE = 1000f
private const val DIM_ALPHA = 0.4f
private const val ZONE_DOWN = 0.12f
private const val ZONE_BARELY = 0.37f
private const val ZONE_BLOODIED = 0.62f
private const val ZONE_WOUNDED = 0.87f
private val ScaleTicks = listOf(0.25f, 0.5f, 0.75f)
private val ThumbSize = 24.dp
private val TrackHeight = 6.dp
private val TickWidth = 2.dp
private val ScaleHeight = 28.dp

/** the scale "how many are left", its estimate with the source and the row to type the exact number */
@Composable
internal fun ScaleBlock(
    scale: PanelScale,
    send: (CombatRoundEvent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppSectionTitleHint(
            title = stringResource(Res.string.combat_panel_mark_title),
            helpTitle = stringResource(Res.string.hp_help_title),
            helpBody = stringResource(Res.string.hp_help_body),
        )
        ScaleSlider(scale) { send(CombatRoundEvent.ScalePicked(it)) }
        ScaleWords(scale.words)
        EstimateRow(scale)
        ExactRow(scale.exact) { send(CombatRoundEvent.Calc(CalcEvent.Opened(CalcMode.SET))) }
    }
}

/** the track and the thumb share one inset, so the words under them stand where the places on the scale are */
@Composable
private fun ScaleSlider(
    scale: PanelScale,
    onPicked: (Int) -> Unit,
) {
    var drag by remember(scale.thumb) { mutableStateOf<Float?>(null) }
    var width by remember { mutableIntStateOf(0) }
    val inset = with(LocalDensity.current) { (ThumbSize / 2).toPx() }
    val place = drag ?: scale.thumb?.let { it / PERMILLE }
    val tone = zoneColor(place ?: 0f)
    val current = rememberUpdatedState(drag)
    val placeAt = { x: Float -> ((x - inset) / (width - 2 * inset).coerceAtLeast(1f)).coerceIn(0f, 1f) }
    val commit = { current.value?.let { onPicked((it * PERMILLE).roundToInt()) } }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(ScaleHeight)
                .onSizeChanged { width = it.width }
                .pointerInput(width) {
                    detectTapGestures { offset ->
                        drag = placeAt(offset.x)
                        commit()
                    }
                }.pointerInput(width) {
                    detectHorizontalDragGestures(
                        onDragStart = { drag = placeAt(it.x) },
                        onDragEnd = { commit() },
                        onDragCancel = { drag = null },
                        onHorizontalDrag = { change, _ -> drag = placeAt(change.position.x) },
                    )
                },
        contentAlignment = Alignment.CenterStart,
    ) {
        ScaleTrack(dim = place == null, modifier = Modifier.padding(horizontal = ThumbSize / 2))
        if (place != null) {
            Box(
                modifier =
                    Modifier.offset {
                        IntOffset((place * (width - 2 * inset)).roundToInt(), 0)
                    },
            ) { ScaleThumb(tone) }
        }
    }
}

@Composable
private fun ScaleThumb(tone: Color) {
    Box(
        modifier =
            Modifier
                .size(ThumbSize)
                .background(AppPalette.TextHeading, CircleShape)
                .border(3.dp, tone, CircleShape),
    )
}

/** the track runs from grey at "down" to green at "whole" through the colours of the words, a tick between them */
@Composable
private fun ScaleTrack(
    dim: Boolean,
    modifier: Modifier = Modifier,
) {
    val tick = AppPalette.Surface
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(TrackHeight)
                .alpha(if (dim) DIM_ALPHA else 1f)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            zoneColor(0f),
                            zoneColor(ZONE_DOWN),
                            zoneColor(ZONE_BARELY),
                            zoneColor(ZONE_BLOODIED),
                            zoneColor(1f),
                        ),
                    ),
                    CircleShape,
                ).drawWithContent {
                    drawContent()
                    ScaleTicks.forEach { part ->
                        drawLine(
                            tick,
                            Offset(size.width * part, 0f),
                            Offset(size.width * part, size.height),
                            TickWidth.toPx(),
                        )
                    }
                },
    )
}

private fun zoneColor(place: Float): Color =
    when {
        place < ZONE_DOWN -> AppPalette.TextMuted
        place < ZONE_BARELY -> RoundColors.wound(WoundTone.BARELY)
        place < ZONE_BLOODIED -> RoundColors.wound(WoundTone.BLOODIED)
        place < ZONE_WOUNDED -> RoundColors.wound(WoundTone.WOUNDED)
        else -> RoundColors.wound(WoundTone.WHOLE)
    }

/** the centre of a word stands under its place on the scale, the two at the ends keep their edge on the track end */
@Composable
private fun ScaleWords(words: List<ScaleWord>) {
    Layout(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ThumbSize / 2),
        content = {
            words.forEach { word ->
                Text(
                    text = word.label,
                    color = if (word.near) AppPalette.TextHeading else AppPalette.TextTertiary,
                    fontSize = AppFontSize.Tiny,
                    fontWeight = if (word.near) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        layout(constraints.maxWidth, placeables.maxOf { it.height }) {
            placeables.forEachIndexed { index, placeable ->
                val centre = constraints.maxWidth * words[index].permille / PERMILLE
                val x = (centre - placeable.width / 2f).coerceIn(0f, (constraints.maxWidth - placeable.width).toFloat())
                placeable.placeRelative(x.roundToInt(), 0)
            }
        }
    }
}

@Composable
private fun EstimateRow(scale: PanelScale) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(shape = AppShapes.Field, background = AppPalette.Background, border = AppPalette.BorderSubtle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            stringResource(Res.string.hp_scale_estimate_title),
            color = AppPalette.TextTertiary,
            style = AppText.fieldOverlineStrong,
        )
        Text(
            scale.estimate,
            color = scale.thumb?.let { zoneColor(it / PERMILLE) } ?: AppPalette.TextMuted,
            fontSize = AppFontSize.MediumPlus,
            fontWeight = FontWeight.Bold,
        )
        if (scale.source.isNotEmpty()) Text(scale.source, color = AppPalette.TextMuted, fontSize = AppFontSize.Caption)
    }
}

@Composable
private fun ExactRow(
    exact: String?,
    onOpen: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 50.dp)
                .appCard(
                    shape = AppShapes.Field,
                    background = Color.Transparent,
                    border = if (exact != null) AppPalette.Gold.copy(alpha = 0.45f) else AppPalette.BorderHover,
                ).padding(start = 12.dp, end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.hp_exact_label),
                color = AppPalette.TextTertiary,
                style = AppText.fieldOverlineStrong,
            )
            Text(
                exact ?: stringResource(Res.string.hp_exact_none),
                color = if (exact != null) AppPalette.TextPrimary else AppPalette.TextTertiary,
                fontSize = AppFontSize.Body,
            )
        }
        AppButton(
            text = stringResource(if (exact != null) Res.string.hp_exact_change else Res.string.hp_exact_set),
            onClick = onOpen,
            tone = ButtonTone.Gold,
            variant = ButtonVariant.Tinted,
            size = ButtonSize.Medium,
        )
    }
}
