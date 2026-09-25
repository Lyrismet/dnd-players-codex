package com.lyrismet.dndcodex.presentation.npcdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NpcDetailUi(
    state: NpcDetailState,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            when {
                state.isLoading -> Text("Loading...", modifier = Modifier.align(Alignment.Center))
                state.npc == null -> Text("NPC not found", modifier = Modifier.align(Alignment.Center))
                else ->
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(state.npc.name)
                        Text(state.npc.status.name)
                        Text(state.npc.description)
                        Button(onClick = { state.eventSink(NpcDetailEvent.BackClicked) }) {
                            Text("Back")
                        }
                    }
            }
        }
    }
}
