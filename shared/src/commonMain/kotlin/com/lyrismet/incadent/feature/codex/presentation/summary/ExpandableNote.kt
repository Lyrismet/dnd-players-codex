package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.feature.mention.domain.MentionSegment
import com.lyrismet.incadent.feature.mention.presentation.MentionText
import com.lyrismet.incadent.feature.mention.presentation.hasMention

private const val COLLAPSED_LINES = 6
private val ARROW_SIZE = 26.sp

/** a note cut to a few lines with "…", a ▾ centered under it opens the rest and a ▴ closes it again */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExpandableNote(
    text: String,
    segments: List<MentionSegment>,
    style: TextStyle,
    color: Color,
    onEntityClick: (EntityRef) -> Unit,
) {
    var expanded by remember(text) { mutableStateOf(false) }
    var cut by remember(text) { mutableStateOf(false) }
    val lines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES
    Column(modifier = Modifier.fillMaxWidth()) {
        if (segments.hasMention()) {
            MentionText(
                segments,
                style,
                color,
                onEntityClick,
                maxLines = lines,
                // the indicator is only composed while something is cut off, so it tells the arrow to show
                overflow =
                    FlowRowOverflow.expandIndicator {
                        DisposableEffect(Unit) {
                            cut = true
                            onDispose { cut = false }
                        }
                        Text("…", style = style, color = color)
                    },
            )
        } else {
            Text(
                text,
                style = style,
                color = color,
                maxLines = lines,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) cut = it.hasVisualOverflow },
            )
        }
        if (cut || expanded) NoteToggle(expanded) { expanded = !expanded }
    }
}

// the arrow gets a tight strip of its own under the text, so it never sits on a line of it
@Composable
private fun NoteToggle(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(top = 4.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            if (expanded) "▴" else "▾",
            style = AppText.notice.copy(fontSize = ARROW_SIZE, lineHeight = ARROW_SIZE),
            color = AppPalette.GoldBright,
        )
    }
}
