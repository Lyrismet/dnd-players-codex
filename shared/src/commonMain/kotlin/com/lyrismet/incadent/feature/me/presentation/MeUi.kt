package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppLoader

/** draws the screen of the player's own character, nothing else */
@Composable
fun MeUi(
    state: MeState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(AppPalette.Background).statusBarsPadding()) {
        when (val content = state.content) {
            MeContent.Loading -> AppLoader(Modifier.align(Alignment.Center))
            is MeContent.Pick -> MePickUi(content, state.eventSink)
            is MeContent.Character -> MeCharacterUi(content.character, state.eventSink)
        }
    }
}
