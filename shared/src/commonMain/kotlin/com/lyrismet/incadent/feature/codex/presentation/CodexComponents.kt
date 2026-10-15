package com.lyrismet.incadent.feature.codex.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.DiamondOutline
import com.lyrismet.incadent.core.designsystem.component.PillSize
import com.lyrismet.incadent.core.designsystem.component.SelectablePill
import com.lyrismet.incadent.core.designsystem.component.appCard

/**
 * the header of one place of the tree - a diamond, its name over the path above it, a rule, how many places lie
 * directly in it and the fold chevron. A tap folds or unfolds the section.
 */
@Composable
internal fun CodexPlaceSectionHeader(
    title: String,
    path: String?,
    count: Int,
    collapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(start = 2.dp, end = 2.dp, top = 8.dp)
                .clickable(onClick = onToggle)
                .heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DiamondOutline(size = 7.dp, color = AppPalette.Parchment, strokeWidth = 1.5.dp)
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                title,
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.16.em,
                    ),
                color = AppPalette.Parchment,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (path != null) {
                Text(
                    path,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = AppPalette.TextSecondary,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .widthIn(min = 12.dp)
                    .height(1.dp)
                    .background(AppPalette.Border),
        )
        Text(count.toString(), fontSize = AppFontSize.CaptionPlus, color = AppPalette.TextSecondary)
        Text(
            if (collapsed) "▸" else "▾",
            fontSize = AppFontSize.Small,
            color = AppPalette.TextSecondary,
            modifier = Modifier.width(14.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun <T> CodexFilterChipRow(
    items: List<CodexFilterOption<T>>,
    selected: T?,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { option ->
            SelectablePill(
                text = option.label,
                selected = option.value == selected,
                onClick = { onSelected(option.value) },
                size = PillSize.Small,
                background = AppPalette.Surface,
                unselectedContent = AppPalette.TextDescription,
            )
        }
    }
}

private val OtherSectionsHintShape = AppShapes.Card
private const val OTHER_SECTIONS_DIAMOND_TOP_DP = 6

/** the note over the list a tab with no hits falls back to - what was not found here and how many hits lie elsewhere */
@Composable
internal fun CodexOtherSectionsHint(
    hint: CodexEmptyState.OtherSections,
    eventSink: (CodexEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 6.dp)
                .appCard(
                    shape = OtherSectionsHintShape,
                    background = AppPalette.Gold.copy(alpha = 0.07f),
                    border = AppPalette.Gold.copy(alpha = 0.4f),
                ).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DiamondOutline(
                size = 8.dp,
                strokeWidth = 1.5.dp,
                modifier = Modifier.padding(top = OTHER_SECTIONS_DIAMOND_TOP_DP.dp),
            )
            Text(
                text =
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = AppPalette.TextPrimary)) { append(hint.message) }
                        append("\n")
                        withStyle(SpanStyle(color = AppPalette.TextSecondary)) { append(hint.foundLabel + " ") }
                        withStyle(SpanStyle(color = AppPalette.GoldBright, fontWeight = FontWeight.SemiBold)) {
                            append(hint.count.toString())
                        }
                    },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = AppFontSize.BodyPlus, lineHeight = 20.sp),
            )
        }
        hint.create?.let { create ->
            AppButton(
                text = create.label,
                onClick = { eventSink(CodexEvent.EmptyActionClicked(create.entryType, create.name)) },
                tone = ButtonTone.Neutral,
                contentColor = AppPalette.TextPrimary,
            )
        }
    }
}
