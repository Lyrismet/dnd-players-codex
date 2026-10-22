package com.lyrismet.incadent.feature.campaign.presentation.sheet

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.file.rememberFilePicker

/** opens the system file picker, a file picked comes back as [CampaignSheetEvent.FilePicked] */
@Composable
fun rememberImportPicker(onEvent: (CampaignSheetEvent) -> Unit): () -> Unit =
    rememberFilePicker { onEvent(CampaignSheetEvent.FilePicked(it)) }
