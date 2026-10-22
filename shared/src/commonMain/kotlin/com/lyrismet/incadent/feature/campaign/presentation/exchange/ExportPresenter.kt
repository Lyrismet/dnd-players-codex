package com.lyrismet.incadent.feature.campaign.presentation.exchange

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.lyrismet.incadent.core.errors.rememberGuardedScope
import com.lyrismet.incadent.core.file.FileStorage
import com.lyrismet.incadent.core.toast.autoDismiss
import com.lyrismet.incadent.feature.campaign.domain.exchange.exportFileName
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeCatalog
import com.lyrismet.incadent.feature.campaign.domain.model.ExportEstimate
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignExchangeRepository
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.error_action_failed
import dndplayerscodex.shared.generated.resources.exchange_save_failed
import dndplayerscodex.shared.generated.resources.exchange_saved_toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// a run of quick ticks only measures the last pick
private const val ESTIMATE_DEBOUNCE_MILLIS = 200L

class ExportPresenter(
    private val screen: ExportScreen,
    private val repository: CampaignExchangeRepository,
    private val storage: FileStorage,
    private val navigator: Navigator,
) : Presenter<ExportState> {
    // one flat merge of the export's state and its events, splitting it would only move the vars around
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    @Composable
    override fun present(): ExportState {
        val catalog by produceState<ExchangeCatalog?>(null) { value = repository.loadCatalog(screen.campaignId) }
        var whole by remember { mutableStateOf(true) }
        var hand by remember(catalog) { mutableStateOf(catalog?.let { ExchangePick.handPick(it.options, it.graph) }) }
        var photos by remember { mutableStateOf(true) }
        var writing by remember { mutableStateOf(false) }
        var file by remember { mutableStateOf<ExportedFile?>(null) }
        val toast = remember { mutableStateOf<String?>(null) }
        toast.autoDismiss()
        val failedText = stringResource(Res.string.error_action_failed)
        val savedText = stringResource(Res.string.exchange_saved_toast)
        val saveFailedText = stringResource(Res.string.exchange_save_failed)
        val scope =
            rememberGuardedScope {
                writing = false
                toast.value = failedText
            }

        val loaded = catalog
        val everything = remember(loaded) { loaded?.let { ExchangePick.everything(it.options, it.graph) } }
        val active = if (whole) everything else hand
        // the text of the file does not depend on the photos switch, so a flip of it does not measure again
        val measured = active?.selection
        val estimate by produceState<ExportEstimate?>(null, measured) {
            // the first measure comes at once, only a run of later picks waits
            if (value != null) delay(ESTIMATE_DEBOUNCE_MILLIS)
            value = measured?.let { repository.estimate(screen.campaignId, it) }
        }
        val view = remember(hand) { hand?.toView() }

        return ExportState(
            loaded = loaded != null,
            writing = writing,
            campaignName = loaded?.campaignName.orEmpty(),
            whole = whole,
            pick = if (whole) null else view,
            recordCount = active?.recordCount ?: 0,
            sessionCount = active?.sessionCount ?: 0,
            photos = photos,
            estimate = estimate,
            canExport = loaded != null && (whole || active?.isEmpty == false),
            file = file,
            toast = toast.value,
        ) { event ->
            when (event) {
                is ExportEvent.WholeSelected -> whole = event.whole
                is ExportEvent.Pick -> hand = hand?.reduce(event.event)
                ExportEvent.PhotosToggled -> photos = !photos
                is ExportEvent.DeliverClicked ->
                    if (measured != null && loaded != null && !writing) {
                        writing = true
                        scope.launch {
                            val fileName = exportFileName(loaded.campaignName)
                            repository.export(
                                screen.campaignId,
                                measured.copy(photos = photos),
                                storage.createSink(fileName),
                            )
                            file = ExportedFile(fileName, event.delivery)
                            writing = false
                        }
                    }
                is ExportEvent.Delivered -> {
                    file = null
                    event.saved?.let { toast.value = if (it) savedText else saveFailedText }
                }
                ExportEvent.Closed -> navigator.pop()
            }
        }
    }
}
