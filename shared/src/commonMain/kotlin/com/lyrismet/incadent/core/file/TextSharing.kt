package com.lyrismet.incadent.core.file

import androidx.compose.runtime.Composable

/** hands a plain text to the system share sheet */
@Composable
expect fun rememberTextSharing(): (String) -> Unit
