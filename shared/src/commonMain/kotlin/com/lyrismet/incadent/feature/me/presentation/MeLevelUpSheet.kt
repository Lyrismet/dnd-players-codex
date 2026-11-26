package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.format.signed
import com.lyrismet.incadent.feature.codex.domain.grouping.classesLine
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.class_level_template
import incadent.shared.generated.resources.me_edit_overline
import incadent.shared.generated.resources.me_level_up_done
import incadent.shared.generated.resources.me_level_up_gain_hint
import incadent.shared.generated.resources.me_level_up_gain_label
import incadent.shared.generated.resources.me_level_up_level_format
import incadent.shared.generated.resources.me_level_up_levels_label
import incadent.shared.generated.resources.me_level_up_no_max_hint
import incadent.shared.generated.resources.me_level_up_title
import incadent.shared.generated.resources.me_level_up_total_format
import org.jetbrains.compose.resources.stringResource

/** the hit points a new level adds, typed by the player and offered again at the next level */
@Composable
internal fun MeLevelUpSheet(
    levelUp: MeOverlay.LevelUp,
    send: (OverlayEvent) -> Unit,
) {
    EditSheet(
        overline = stringResource(Res.string.me_edit_overline),
        title = stringResource(Res.string.me_level_up_title),
        subtitle = stringResource(Res.string.me_level_up_level_format, levelUp.from, levelUp.from + levelUp.levels),
        onClose = { send(OverlayEvent.Closed) },
        onDone = { send(OverlayEvent.Applied) },
        doneLabel = stringResource(Res.string.me_level_up_done),
        doneEnabled = levelUp.levels >= 1,
    ) {
        SectionOverline(stringResource(Res.string.me_level_up_levels_label), color = AppPalette.TextTertiary)
        if (levelUp.classes.size >= 2) {
            val levelTemplate = stringResource(Res.string.class_level_template)
            levelUp.classes.forEachIndexed { index, entry ->
                Text(
                    listOf(entry).classesLine(levelTemplate),
                    style = AppText.footnote,
                    color = AppPalette.TextSecondary,
                )
                NumberRow(
                    levelUp.added[index],
                    send,
                    spread = true,
                    onStep = { OverlayEvent.ClassLevelsStepped(index, it) },
                    signed = true,
                )
            }
        } else {
            NumberRow(levelUp.levels, send, spread = true, onStep = { OverlayEvent.LevelsStepped(it) })
        }
        if (levelUp.hasMax) {
            SectionOverline(stringResource(Res.string.me_level_up_gain_label), color = AppPalette.TextTertiary)
            NumberRow(levelUp.gain, send, spread = true, signed = true)
            if (levelUp.levels > 1) {
                Text(
                    stringResource(Res.string.me_level_up_total_format, (levelUp.gain * levelUp.levels).signed()),
                    style = AppText.footnote,
                    color = AppPalette.TextSecondary,
                )
            }
            Text(
                stringResource(Res.string.me_level_up_gain_hint),
                style = AppText.footnote,
                color = AppPalette.TextSecondary,
            )
        } else {
            Text(
                stringResource(Res.string.me_level_up_no_max_hint),
                style = AppText.footnote,
                color = AppPalette.TextSecondary,
            )
        }
    }
}
