package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_fill_back
import dndplayerscodex.shared.generated.resources.me_fill_finish
import dndplayerscodex.shared.generated.resources.me_fill_next
import dndplayerscodex.shared.generated.resources.me_fill_overline_format
import dndplayerscodex.shared.generated.resources.me_fill_skip
import dndplayerscodex.shared.generated.resources.me_fill_title_1
import dndplayerscodex.shared.generated.resources.me_fill_title_2
import dndplayerscodex.shared.generated.resources.me_fill_title_3
import dndplayerscodex.shared.generated.resources.me_fill_title_4
import org.jetbrains.compose.resources.stringResource

/** the four steps that fill the sheet from the paper one: class and level, scores, saves and skills, the rest */
@Composable
internal fun MeFillSheet(
    draft: FillDraft,
    send: (OverlayEvent) -> Unit,
) {
    val titles =
        listOf(
            Res.string.me_fill_title_1,
            Res.string.me_fill_title_2,
            Res.string.me_fill_title_3,
            Res.string.me_fill_title_4,
        )
    AppBottomSheet(onDismissRequest = { send(OverlayEvent.Closed) }) {
        AppSheetColumn(
            scroll = true,
            header = {
                AppSheetHeader(
                    title = stringResource(titles[draft.step - 1]),
                    onClose = { send(OverlayEvent.Closed) },
                    overline = stringResource(Res.string.me_fill_overline_format, draft.step, FillDraft.LAST_STEP),
                )
            },
        ) {
            Progress(draft.step)
            when (draft.step) {
                FillDraft.STEP_CLASS -> StepClass(draft, send)
                FillDraft.STEP_ABILITIES -> StepAbilities(draft, send)
                FillDraft.STEP_SKILLS -> StepSkills(draft, send)
                else -> StepNumbers(draft, send)
            }
            FillButtons(draft.step, send)
        }
    }
}

@Composable
private fun Progress(step: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(FillDraft.LAST_STEP) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(
                        androidx.compose.foundation.shape
                            .RoundedCornerShape(2.dp),
                    ).background(if (index < step) AppPalette.Gold else AppPalette.BorderSubtle),
            )
        }
    }
}

@Composable
private fun FillButtons(
    step: Int,
    send: (OverlayEvent) -> Unit,
) {
    val last = step == FillDraft.LAST_STEP
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (step > 1) {
            AppButton(
                text = stringResource(Res.string.me_fill_back),
                onClick = { send(OverlayEvent.FillBack) },
                tone = ButtonTone.Neutral,
                size = ButtonSize.Primary,
                variant = ButtonVariant.Solid,
                modifier = Modifier.width(120.dp),
            )
        }
        FormPrimaryButton(
            text = stringResource(if (last) Res.string.me_fill_finish else Res.string.me_fill_next),
            onClick = { send(if (last) OverlayEvent.Applied else OverlayEvent.FillNext) },
            modifier = Modifier.weight(1f),
        )
    }
    AppButton(
        text = stringResource(Res.string.me_fill_skip),
        onClick = { send(if (last) OverlayEvent.Closed else OverlayEvent.FillSkip) },
        tone = ButtonTone.Neutral,
        modifier = Modifier.fillMaxWidth(),
        size = ButtonSize.Large,
    )
}
