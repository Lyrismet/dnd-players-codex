package com.lyrismet.incadent.feature.campaign.presentation.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppSwitch
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.format.fileSizeOf
import com.lyrismet.incadent.core.localization.currentAppLanguage
import com.lyrismet.incadent.core.preferences.AppLanguage
import com.lyrismet.incadent.feature.campaign.domain.model.ExportEstimate
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.exchange_approx
import dndplayerscodex.shared.generated.resources.exchange_photos_hint
import dndplayerscodex.shared.generated.resources.exchange_photos_none
import dndplayerscodex.shared.generated.resources.exchange_photos_off
import dndplayerscodex.shared.generated.resources.exchange_photos_on
import dndplayerscodex.shared.generated.resources.exchange_photos_title
import dndplayerscodex.shared.generated.resources.exchange_size_kb
import dndplayerscodex.shared.generated.resources.exchange_size_mb
import org.jetbrains.compose.resources.stringResource

/** a size in bytes as "12 КБ" or "1,5 МБ", the decimal sign follows the app language */
@Composable
fun fileSizeText(
    bytes: Long,
    approximate: Boolean = false,
): String {
    val size = fileSizeOf(bytes, if (currentAppLanguage() == AppLanguage.RUSSIAN) ',' else '.')
    val unit = if (size.isMegabytes) Res.string.exchange_size_mb else Res.string.exchange_size_kb
    val text = stringResource(unit, size.number)
    return if (approximate) stringResource(Res.string.exchange_approx, text) else text
}

/**
 * the "С фото" switch with what the photos weigh, a file with no photos only says so in a plain line. Without an
 * [estimate] (an import) the card holds the hint instead of sizes.
 */
@Composable
fun ExchangePhotosCard(
    photos: Boolean,
    estimate: ExportEstimate?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (estimate != null && estimate.photoCount == 0) {
        Text(
            text =
                stringResource(
                    Res.string.exchange_photos_none,
                    fileSizeText(estimate.textBytes, approximate = true),
                ),
            modifier = modifier,
            color = AppPalette.TextSecondary,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Small),
        )
        return
    }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Background, AppPalette.BorderSubtle, onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.exchange_photos_title),
                color = AppPalette.TextPrimary,
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontSize = AppFontSize.Medium,
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
            Text(
                text = estimate?.let { photosNote(it, photos) } ?: stringResource(Res.string.exchange_photos_hint),
                modifier = Modifier.padding(top = 1.dp),
                color = AppPalette.TextSecondary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Small),
            )
        }
        AppSwitch(checked = photos, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun photosNote(
    estimate: ExportEstimate,
    photos: Boolean,
): String {
    val text = fileSizeText(estimate.textBytes, approximate = true)
    val withPhotos = fileSizeText(estimate.textBytes + estimate.photoBytes, approximate = true)
    return if (photos) {
        stringResource(Res.string.exchange_photos_on, withPhotos, estimate.photoCount, text)
    } else {
        stringResource(Res.string.exchange_photos_off, text, withPhotos)
    }
}
