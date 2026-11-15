package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppDashedTile
import com.lyrismet.incadent.core.designsystem.component.AppHelpHint

// the small caption of the slots with its help hint, kept here because a shared package cannot import its feature root
@Composable
internal fun SectionCaption(
    title: String,
    helpTitle: String,
    helpBody: String,
) {
    Row(Modifier.height(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), style = AppText.fieldOverline, color = AppPalette.TextTertiary)
        AppHelpHint(helpTitle, helpBody, Modifier.requiredSize(28.dp))
    }
}

/** the gold dashed tile that adds a spell, a damage row or a growth rule */
@Composable
internal fun SpellAddTile(
    label: String,
    onClick: () -> Unit,
) {
    AppDashedTile(onClick, Modifier.fillMaxWidth(), color = AppPalette.Gold.copy(alpha = 0.45f), cornerRadius = 11.dp) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = AppPalette.GoldBright,
        )
    }
}

internal fun <T> List<T>.replaceAt(
    index: Int,
    change: (T) -> T,
): List<T> = mapIndexed { i, old -> if (i == index) change(old) else old }

internal fun <T> List<T>.removeAt(index: Int): List<T> = filterIndexed { i, _ -> i != index }
