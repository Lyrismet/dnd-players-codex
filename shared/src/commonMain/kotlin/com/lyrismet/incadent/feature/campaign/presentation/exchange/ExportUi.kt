package com.lyrismet.incadent.feature.campaign.presentation.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppLoader
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.StatusMark
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.file.rememberFileSharing
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.exchange_export_overline
import dndplayerscodex.shared.generated.resources.exchange_photos_footnote
import dndplayerscodex.shared.generated.resources.exchange_records_count
import dndplayerscodex.shared.generated.resources.exchange_save
import dndplayerscodex.shared.generated.resources.exchange_scope_all
import dndplayerscodex.shared.generated.resources.exchange_scope_pick
import dndplayerscodex.shared.generated.resources.exchange_sessions_count
import dndplayerscodex.shared.generated.resources.exchange_share
import dndplayerscodex.shared.generated.resources.exchange_summary_hint
import dndplayerscodex.shared.generated.resources.exchange_summary_line
import dndplayerscodex.shared.generated.resources.exchange_too_large
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val PHOTOS_SLOT_HEIGHT = 64.dp

/** shows the export as one bottom sheet over the app: what to take, the photos switch and the two ways to deliver */
@Composable
fun ExportUi(
    state: ExportState,
    modifier: Modifier = Modifier,
) {
    val sink = state.eventSink
    val sharing = rememberFileSharing(onSaved = { sink(ExportEvent.Delivered(it)) })
    // the presenter has written the file, only the view can open the system dialog for it
    LaunchedEffect(state.file) {
        val file = state.file ?: return@LaunchedEffect
        when (file.delivery) {
            ExportDelivery.SHARE -> {
                sharing.share(file.fileName)
                sink(ExportEvent.Delivered(saved = null))
            }
            ExportDelivery.SAVE -> {
                sharing.save(file.fileName)
                // the dialog answers through onSaved, a cancelled one only frees the buttons
                sink(ExportEvent.Delivered(saved = null))
            }
        }
    }
    Box(modifier = modifier) {
        AppBottomSheet(onDismissRequest = { sink(ExportEvent.Closed) }) {
            AppSheetColumn(spacing = 14.dp, scroll = true) {
                AppSheetHeader(
                    overline = stringResource(Res.string.exchange_export_overline),
                    title = "«${state.campaignName}»",
                    onClose = { sink(ExportEvent.Closed) },
                )
                ExportBody(state)
            }
        }
    }
}

@Composable
private fun ExportBody(state: ExportState) {
    val sink = state.eventSink
    AppSegmentedTabs(
        tabs =
            listOf(
                SegmentedTab(true, stringResource(Res.string.exchange_scope_all)),
                SegmentedTab(false, stringResource(Res.string.exchange_scope_pick)),
            ),
        selected = state.whole,
        onSelected = { sink(ExportEvent.WholeSelected(it)) },
        itemHeight = 36.dp,
        containerBackground = AppPalette.Background,
    )
    if (state.whole) {
        ExchangeSummary(state.recordCount, state.sessionCount, stringResource(Res.string.exchange_summary_hint))
    }
    state.pick?.let { view -> ExchangePickSection(view, onEvent = { sink(ExportEvent.Pick(it)) }) }
    // until the first measure is in, nobody knows whether there are photos, so a loader stands in for the card
    val estimate = state.estimate
    if (estimate == null) {
        Box(modifier = Modifier.fillMaxWidth().height(PHOTOS_SLOT_HEIGHT), contentAlignment = Alignment.Center) {
            AppLoader()
        }
    } else {
        ExchangePhotosCard(photos = state.photos, estimate = estimate, onToggle = { sink(ExportEvent.PhotosToggled) })
    }
    Text(
        text = stringResource(Res.string.exchange_photos_footnote),
        color = AppPalette.TextSecondary,
        style = AppText.footnote,
    )
    if (state.tooLarge) {
        Text(
            text = stringResource(Res.string.exchange_too_large),
            color = AppPalette.MaroonBright,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.CaptionPlus),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppButton(
            text = stringResource(Res.string.exchange_share),
            onClick = { sink(ExportEvent.DeliverClicked(ExportDelivery.SHARE)) },
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Solid,
            size = ButtonSize.Primary,
            enabled = state.canGo,
            leading = deliveryMark(state.status, ExportDelivery.SHARE),
        )
        AppButton(
            text = stringResource(Res.string.exchange_save),
            onClick = { sink(ExportEvent.DeliverClicked(ExportDelivery.SAVE)) },
            modifier = Modifier.fillMaxWidth(),
            tone = ButtonTone.Neutral,
            variant = ButtonVariant.Ghost,
            size = ButtonSize.Primary,
            enabled = state.canGo,
            contentColor = AppPalette.TextPrimary,
            leading = deliveryMark(state.status, ExportDelivery.SAVE),
        )
    }
}

// the mark sits in the button of the delivery it belongs to
private fun deliveryMark(
    status: DeliveryStatus?,
    delivery: ExportDelivery,
): (
    @Composable RowScope.(
        Color,
    ) -> Unit
)? =
    status?.takeIf { it.delivery == delivery }?.let { current -> { StatusMark(current.kind) } }

@Composable
internal fun ExchangeSummary(
    records: Int,
    sessions: Int,
    hint: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Background, AppPalette.BorderSubtle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text =
                stringResource(
                    Res.string.exchange_summary_line,
                    pluralStringResource(Res.plurals.exchange_records_count, records, records),
                    pluralStringResource(Res.plurals.exchange_sessions_count, sessions, sessions),
                ),
            color = AppPalette.TextPrimary,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = AppFontSize.Medium),
        )
        Text(
            text = hint,
            color = AppPalette.TextSecondary,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Small),
        )
    }
}
