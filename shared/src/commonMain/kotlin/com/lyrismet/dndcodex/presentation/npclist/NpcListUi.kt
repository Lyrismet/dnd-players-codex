package com.lyrismet.dndcodex.presentation.npclist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun NpcListUi(
    state: NpcListState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { state.eventSink(NpcListEvent.AddSampleNpcClicked) }) {
                Text("+")
            }
        },
    ) { contentPadding ->
        if (state.npcs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No NPCs yet - tap + to add one")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                items(state.npcs, key = { it.id }) { npc ->
                    ListItem(
                        headlineContent = { Text(npc.name) },
                        supportingContent = { Text(npc.status.name) },
                        modifier = Modifier.clickable { state.eventSink(NpcListEvent.NpcClicked(npc.id)) },
                    )
                }
            }
        }
    }
}
