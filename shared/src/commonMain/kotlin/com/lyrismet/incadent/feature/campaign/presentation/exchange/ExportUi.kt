package com.lyrismet.incadent.feature.campaign.presentation.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.lyrismet.incadent.core.designsystem.component.AppToggleCard
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.StatusMark
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.file.rememberFileSharing
import com.lyrismet.incadent.core.help.HelpHint
import com.lyrismet.incadent.core.help.HelpTopic
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.exchange_export_file
import incadent.shared.generated.resources.exchange_export_overline
import incadent.shared.generated.resources.exchange_friend_banner
import incadent.shared.generated.resources.exchange_live_export_note
import incadent.shared.generated.resources.exchange_mode_friend
import incadent.shared.generated.resources.exchange_mode_self
import incadent.shared.generated.resources.exchange_mode_self_note
import incadent.shared.generated.resources.exchange_party_notes_hint
import incadent.shared.generated.resources.exchange_party_notes_title
import incadent.shared.generated.resources.exchange_photos_footnote
import incadent.shared.generated.resources.exchange_records_count
import incadent.shared.generated.resources.exchange_save
import incadent.shared.generated.resources.exchange_scope_all
import incadent.shared.generated.resources.exchange_scope_pick
import incadent.shared.generated.resources.exchange_sessions_count
import incadent.shared.generated.resources.exchange_share
import incadent.shared.generated.resources.exchange_summary_hint
import incadent.shared.generated.resources.exchange_summary_line
import incadent.shared.generated.resources.exchange_too_large
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
    val share = rememberFileSharing()
    // the presenter has written the file, only the view can open the system share sheet for it
    LaunchedEffect(state.file) {
        val file = state.file ?: return@LaunchedEffect
        share(file)
        sink(ExportEvent.Delivered(saved = null))
    }
    Box(modifier = modifier) {
        AppBottomSheet(onDismissRequest = { sink(ExportEvent.Closed) }) {
            AppSheetColumn(spacing = 14.dp, scroll = true, header = {
                AppSheetHeader(
                    overline = stringResource(Res.string.exchange_export_overline),
                    title = "«${state.campaignName}»",
                    onClose = { sink(ExportEvent.Closed) },
                )
            }) {
                ExportBody(state)
            }
        }
        state.savePrompt?.let { ExportSaveSheet(it, onEvent = sink) }
    }
}

@Suppress("LongMethod")
@Composable
private fun ExportBody(state: ExportState) {
    val sink = state.eventSink
    val friend = state.mode == ExportMode.FRIEND
    AppSegmentedTabs(
        tabs =
            listOf(
                SegmentedTab(ExportMode.SELF, stringResource(Res.string.exchange_mode_self)),
                SegmentedTab(ExportMode.FRIEND, stringResource(Res.string.exchange_mode_friend)),
            ),
        selected = state.mode,
        onSelected = { sink(ExportEvent.ModeSelected(it)) },
        itemHeight = 36.dp,
        containerBackground = AppPalette.Background,
    )
    if (friend) {
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
    }
    if (state.whole || !friend) {
        val hint = if (friend) Res.string.exchange_summary_hint else Res.string.exchange_mode_self_note
        ExchangeSummary(state.recordCount, state.sessionCount, stringResource(hint))
    }
    state.liveFight?.let {
        LiveFightCard(
            fight = it,
            note = stringResource(Res.string.exchange_live_export_note),
            checked = state.liveFightOn,
            onToggle = { sink(ExportEvent.LiveFightToggled) },
        )
    }
    state.pick?.let { view -> ExchangePickSection(view, onEvent = { sink(ExportEvent.Pick(it)) }) }
    if (friend) FriendNotice(state)
    Text(
        text = stringResource(Res.string.exchange_export_file, state.fileName),
        color = AppPalette.TextSecondary,
        style = AppText.footnote,
    )
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

// the switch that lets the notes about the party go too, and what a friend does not get with the "?" that lists it
@Composable
private fun FriendNotice(state: ExportState) {
    AppToggleCard(
        title = stringResource(Res.string.exchange_party_notes_title),
        hint = stringResource(Res.string.exchange_party_notes_hint),
        checked = state.partyNotes,
        onToggle = { state.eventSink(ExportEvent.PartyNotesToggled) },
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Background, AppPalette.Border)
                .padding(start = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.exchange_friend_banner),
            modifier = Modifier.weight(1f),
            color = AppPalette.TextDescription,
            style = MaterialTheme.typography.bodyMedium,
        )
        HelpHint(HelpTopic.FRIEND_EXPORT)
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
