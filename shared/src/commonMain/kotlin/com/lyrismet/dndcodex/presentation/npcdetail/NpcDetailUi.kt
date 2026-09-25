package com.lyrismet.dndcodex.presentation.npcdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.toStatusColor

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
                else -> {
                    val statusColor = state.npc.status.toStatusColor()
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            state.npc.name,
                            style = MaterialTheme.typography.headlineLarge,
                            color = AppPalette.TextHeading,
                        )
                        Text(
                            state.npc.status.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor.foreground,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            state.npc.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppPalette.TextPrimary,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { state.eventSink(NpcDetailEvent.BackClicked) }) {
                            Text("Back")
                        }
                    }
                }
            }
        }
    }
}
