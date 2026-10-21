package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_quantity_hint
import org.jetbrains.compose.resources.stringResource

// the pale line that tells what a digit typed now will do to the species just inserted
@Composable
internal fun QuantityHint(name: String) {
    Text(
        stringResource(Res.string.session_detail_quantity_hint, name),
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        style = MaterialTheme.typography.bodySmall,
        color = AppPalette.TextMuted,
    )
}
