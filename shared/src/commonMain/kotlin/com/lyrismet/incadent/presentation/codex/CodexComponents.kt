package com.lyrismet.incadent.presentation.codex

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppSearchField
import com.lyrismet.incadent.core.designsystem.component.ChoiceChip
import com.lyrismet.incadent.core.designsystem.component.HeaderActionButton
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.SegmentedControl
import com.lyrismet.incadent.core.designsystem.component.appCard

@Composable
internal fun CodexSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    AppSearchField(query = query, onQueryChange = onQueryChange, placeholder = placeholder, modifier = modifier)
}

private const val DIAMOND_ROTATION_DEGREES = 45f

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
        Box(
            Modifier
                .size(7.dp)
                .rotate(DIAMOND_ROTATION_DEGREES)
                .border(1.5.dp, AppPalette.Parchment),
        )
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                title,
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
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
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
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
        Text(count.toString(), fontSize = 11.5.sp, color = AppPalette.TextSecondary)
        Text(
            if (collapsed) "▸" else "▾",
            fontSize = 12.sp,
            color = AppPalette.TextSecondary,
            modifier = Modifier.width(14.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
internal fun CodexSectionHeader(
    text: String,
    count: Int,
    isFirst: Boolean,
    modifier: Modifier = Modifier,
) {
    SectionOverline(
        text = text,
        modifier = modifier.padding(start = 4.dp, end = 4.dp, top = if (isFirst) 4.dp else 12.dp),
        color = AppPalette.TextSecondary,
        letterSpacing = 0.16.em,
        trailingContent = {
            Text(
                count.toString(),
                style =
                    MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.em,
                    ),
                color = AppPalette.TextSecondary,
            )
        },
    )
}

internal data class CodexTabRowItem(
    val tab: CodexTab,
    val label: String,
    val count: Int,
)

@Composable
internal fun CodexTabRow(
    items: List<CodexTabRowItem>,
    selected: CodexTab,
    onSelected: (CodexTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedControl(
        items = items,
        onSelected = { onSelected(it.tab) },
        itemBackground = { item ->
            if (item.tab == selected) AppPalette.SurfaceElevated else Color.Transparent
        },
        modifier = modifier,
        itemHeight = 36.dp,
        itemSpacing = 2.dp,
    ) { item ->
        val isSelected = item.tab == selected
        val tint = if (isSelected) AppPalette.GoldBright else AppPalette.TextSecondary
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.count.toString(),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.5.sp),
                color = tint.copy(alpha = 0.65f),
            )
        }
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
            ChoiceChip(
                text = option.label,
                selected = option.value == selected,
                onClick = { onSelected(option.value) },
            )
        }
    }
}

private val OtherSectionsHintShape = RoundedCornerShape(14.dp)
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
            Box(
                Modifier
                    .padding(top = OTHER_SECTIONS_DIAMOND_TOP_DP.dp)
                    .size(8.dp)
                    .rotate(DIAMOND_ROTATION_DEGREES)
                    .border(1.5.dp, AppPalette.Gold),
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
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 20.sp),
            )
        }
        hint.create?.let { create ->
            HeaderActionButton(
                text = create.label,
                onClick = { eventSink(CodexEvent.EmptyActionClicked(create.entryType, create.name)) },
                foreground = AppPalette.TextPrimary,
                background = Color.Transparent,
                border = AppPalette.BorderHover,
            )
        }
    }
}
