package com.lyrismet.dndcodex.presentation.npclist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.component.EmptyStatePlaceholder
import com.lyrismet.dndcodex.core.designsystem.toStatusColor

@Composable
fun NpcListUi(
    state: NpcListState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { state.eventSink(NpcListEvent.AddSampleNpcClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Text("+")
            }
        },
    ) { contentPadding ->
        if (state.npcs.isEmpty()) {
            EmptyStatePlaceholder(
                text = "No NPCs yet - tap + to add one",
                modifier = Modifier.padding(contentPadding),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                items(state.npcs, key = { it.id }) { npc ->
                    val statusColor = npc.status.toStatusColor()
                    ListItem(
                        headlineContent = {
                            Text(
                                npc.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = AppPalette.TextHeading,
                            )
                        },
                        supportingContent = {
                            Text(
                                npc.status.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor.foreground,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = AppPalette.Surface),
                        modifier = Modifier.clickable { state.eventSink(NpcListEvent.NpcClicked(npc.id)) },
                    )
                }
            }
        }
    }
}
