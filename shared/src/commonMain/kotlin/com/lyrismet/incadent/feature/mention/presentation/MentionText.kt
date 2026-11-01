package com.lyrismet.incadent.feature.mention.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.mention.domain.MentionSegment

/** whether [segments] hold a mention token, a plain note needs no chips */
fun List<MentionSegment>.hasMention(): Boolean = any { it is MentionSegment.Mention }

/** a note with its mentions drawn as chips, wrapped word by word so a chip never sits alone on a line */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MentionText(
    segments: List<MentionSegment>,
    style: TextStyle,
    color: Color,
    onEntityClick: (EntityRef) -> Unit,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: FlowRowOverflow = FlowRowOverflow.Clip,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        maxLines = maxLines,
        overflow = overflow,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        segments.forEach { segment ->
            when (segment) {
                is MentionSegment.Text -> PlainWords(segment.text, style, color)
                is MentionSegment.AutoMatch -> PlainWords(segment.text, style, color)
                is MentionSegment.Mention -> {
                    val chip = segment.entity.toChipItem().copy(quantity = segment.quantity)
                    MentionChip(
                        item = chip,
                        onClick = chip.entityRef?.let { ref -> { onEntityClick(ref) } },
                        fontSize = AppFontSize.Medium,
                        glyphSize = 13.dp,
                        glyphGap = 4.dp,
                        lineHeightFactor = 1.5f,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlainWords(
    text: String,
    style: TextStyle,
    color: Color,
) {
    text.split('\n').forEachIndexed { lineIndex, line ->
        // a full-width empty item forces the flow onto a fresh line for each newline in the note
        if (lineIndex > 0) Spacer(Modifier.fillMaxWidth())
        line.split(' ').forEach { word -> if (word.isNotEmpty()) Text(word, style = style, color = color) }
    }
}
