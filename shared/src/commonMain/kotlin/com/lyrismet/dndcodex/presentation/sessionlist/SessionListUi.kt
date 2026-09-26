package com.lyrismet.dndcodex.presentation.sessionlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.component.HeaderActionButton
import com.lyrismet.dndcodex.core.designsystem.component.ScreenHeader
import com.lyrismet.dndcodex.core.designsystem.component.SectionOverline
import com.lyrismet.dndcodex.core.designsystem.component.TagChip

@Composable
fun SessionListUi(
    state: SessionListState,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            ScreenHeader(
                overline = state.campaignName,
                title = "Дневник сессий",
                overlineTrailingContent = {
                    HeaderActionButton(
                        text = "+ Сессия",
                        onClick = { state.eventSink(SessionListEvent.NewSessionClicked) },
                    )
                },
            )
            if (state.sessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Нет ни одной сессии - нажмите «+ Сессия», чтобы начать",
                        color = AppPalette.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        SectionOverline(
                            text = "Архив",
                            color = AppPalette.TextSecondary,
                            trailingContent = {
                                Text(
                                    state.sessions.size.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppPalette.TextSecondary,
                                )
                            },
                        )
                    }
                    items(state.sessions, key = { it.id }) { session ->
                        SessionArchiveRow(
                            session = session,
                            onClick = { state.eventSink(SessionListEvent.SessionClicked(session.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionArchiveRow(
    session: SessionListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppPalette.SurfaceVariant)
                .clickable(onClick = onClick)
                .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = session.numberLabel,
            style = MaterialTheme.typography.titleMedium,
            color = AppPalette.GoldDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(52.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(session.title, style = MaterialTheme.typography.titleMedium, color = AppPalette.TextHeading)
            Text(session.dateLabel, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextSecondary)
            if (session.hasMentions) {
                TagChip(
                    text = "Есть упоминания",
                    foreground = AppPalette.Gold,
                    background = AppPalette.Gold.copy(alpha = 0.08f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
