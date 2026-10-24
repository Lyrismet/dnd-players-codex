package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppLoader
import kotlinx.coroutines.delay

private const val LOADER_DELAY_MS = 150L

/** a card was asked for and its summary has not come back yet */
fun isEntityLoading(
    ref: EntityRef?,
    summary: EntitySummaryItem?,
): Boolean = ref != null && summary == null

/** the entity card's place while its summary is still being built - it only shows if the wait is not short */
@Composable
fun EntityLoadingSheet(onDismiss: () -> Unit) {
    var waited by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(LOADER_DELAY_MS)
        waited = true
    }
    if (!waited) return
    AppBottomSheet(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { AppLoader() }
    }
}
