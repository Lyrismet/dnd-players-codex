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
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_add
import org.jetbrains.compose.resources.stringResource

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

/** the gold dashed button that adds a spell */
@Composable
internal fun AddSpellButton(onClick: () -> Unit) {
    AppDashedTile(onClick, Modifier.fillMaxWidth(), color = AppPalette.Gold.copy(alpha = 0.45f), cornerRadius = 11.dp) {
        Text(
            stringResource(Res.string.me_spells_add),
            style = MaterialTheme.typography.titleSmall,
            color = AppPalette.GoldBright,
        )
    }
}
