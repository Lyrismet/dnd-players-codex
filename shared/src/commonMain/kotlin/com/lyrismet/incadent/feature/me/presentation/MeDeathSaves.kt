package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.me.domain.DeathSaveState
import com.lyrismet.incadent.feature.me.domain.deathSaveState
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_death_add_failure
import incadent.shared.generated.resources.me_death_add_success
import incadent.shared.generated.resources.me_death_dead
import incadent.shared.generated.resources.me_death_failures
import incadent.shared.generated.resources.me_death_mark_fallen
import incadent.shared.generated.resources.me_death_stable
import incadent.shared.generated.resources.me_death_stable_title
import incadent.shared.generated.resources.me_death_successes
import incadent.shared.generated.resources.me_death_title
import incadent.shared.generated.resources.me_death_undo_failure
import org.jetbrains.compose.resources.stringResource

private const val DOTS = 3

/** shown on zero hit points: three success and three failure dots and what the count means */
@Composable
internal fun MeDeathSaves(
    character: MeCharacter,
    onEvent: (MeEvent) -> Unit,
) {
    val vitals = character.vitals
    val state = vitals.deathSaveState()
    val send = { event: DeathSavesEvent -> onEvent(MeEvent.DeathSaves(event)) }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(border = if (state == DeathSaveState.DEAD) AppPalette.Maroon else AppPalette.BorderSubtle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val title = if (state == DeathSaveState.STABLE) Res.string.me_death_stable_title else Res.string.me_death_title
        Text(
            stringResource(title).uppercase(),
            style = AppText.fieldOverline,
            color = if (state == DeathSaveState.DEAD) AppPalette.MaroonBright else AppPalette.TextTertiary,
        )
        DotsRow(DeathDots.Success, stringResource(Res.string.me_death_successes), vitals.deathSavesSuccess) {
            send(DeathSavesEvent.SuccessDot(it))
        }
        DotsRow(DeathDots.Failure, stringResource(Res.string.me_death_failures), vitals.deathSavesFailure) {
            send(DeathSavesEvent.FailureDot(it))
        }
        DeathSaveActions(state, send)
    }
}

@Composable
private fun DeathSaveActions(
    state: DeathSaveState,
    send: (DeathSavesEvent) -> Unit,
) {
    when (state) {
        DeathSaveState.DYING ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppButton(
                    stringResource(Res.string.me_death_add_success),
                    { send(DeathSavesEvent.SuccessAdded) },
                    Modifier.weight(1f),
                    tone = ButtonTone.Gold,
                    size = ButtonSize.Large,
                    contentColor = AppPalette.EmeraldBright,
                )
                AppButton(
                    stringResource(Res.string.me_death_add_failure),
                    { send(DeathSavesEvent.FailureAdded) },
                    Modifier.weight(1f),
                    tone = ButtonTone.Maroon,
                    size = ButtonSize.Large,
                )
            }
        DeathSaveState.STABLE ->
            Text(
                stringResource(Res.string.me_death_stable),
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextDescription,
            )
        DeathSaveState.DEAD -> {
            Text(
                stringResource(Res.string.me_death_dead),
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextDescription,
            )
            AppButton(
                stringResource(Res.string.me_death_mark_fallen),
                { send(DeathSavesEvent.MarkedFallen) },
                Modifier.fillMaxWidth(),
                tone = ButtonTone.Maroon,
                variant = ButtonVariant.Solid,
                size = ButtonSize.Primary,
            )
            AppButton(
                stringResource(Res.string.me_death_undo_failure),
                { send(DeathSavesEvent.FailureRemoved) },
                Modifier.fillMaxWidth(),
                tone = ButtonTone.Neutral,
                size = ButtonSize.Large,
            )
        }
    }
}

/** how the dots of one kind of death save look */
private enum class DeathDots(
    val color: Color,
    val fill: Color,
    val glyph: String,
    val glyphColor: Color,
) {
    Success(AppPalette.EmeraldBright, AppPalette.Emerald, "✓", AppPalette.HealOnFill),
    Failure(AppPalette.MaroonBright, AppPalette.Maroon, "✕", AppPalette.TextHeading),
}

@Composable
private fun DotsRow(
    kind: DeathDots,
    label: String,
    count: Int,
    onDot: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(76.dp), style = MaterialTheme.typography.bodyMedium, color = kind.color)
        repeat(DOTS) { index ->
            val on = index < count
            Box(Modifier.size(44.dp).clickable { onDot(index) }, contentAlignment = Alignment.Center) {
                Box(
                    modifier =
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (on) kind.fill else Color.Transparent)
                            .border(1.5.dp, if (on) Color.Transparent else kind.color.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        kind.glyph,
                        color = if (on) kind.glyphColor else Color.Transparent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
