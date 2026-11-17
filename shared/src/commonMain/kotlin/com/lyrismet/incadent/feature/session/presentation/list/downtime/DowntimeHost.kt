package com.lyrismet.incadent.feature.session.presentation.list.downtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.session.domain.DowntimeLedger
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.feature.session.domain.nextSessionIds
import com.lyrismet.incadent.feature.session.domain.repository.DowntimeRepository
import com.lyrismet.incadent.feature.session.domain.sessionNumberLabels
import kotlinx.coroutines.sync.Mutex

/** the downtimes of the open campaign, the window that is open over them and what handles their presses */
internal class DowntimeHost(
    val cards: List<DowntimeCard>,
    val sheet: DowntimeSheetState,
    val handler: DowntimeHandler,
)

@Composable
@Suppress("LongParameterList")
internal fun rememberDowntimeHost(
    sessions: List<SessionNote>,
    numbering: SessionNumbering,
    memberId: Long?,
    readOnly: Boolean,
    repository: DowntimeRepository,
    ledger: DowntimeLedger,
    undoController: UndoController,
): DowntimeHost {
    val downtimes by repository.observeAll().collectAsState(initial = emptyList())
    val sheet = remember { mutableStateOf<DowntimeSheetState>(DowntimeSheetState.Hidden) }
    val labels = remember(sessions, numbering) { sessions.sessionNumberLabels(numbering) }
    val nextIds = remember(sessions) { sessions.nextSessionIds() }
    val cards =
        remember(downtimes, labels, nextIds) {
            downtimes.mapNotNull { downtime ->
                labels[downtime.afterSessionId]?.let {
                    downtime.toCard(
                        it,
                        nextIds[downtime.afterSessionId]?.let(labels::get),
                    )
                }
            }
        }
    val latestCards by rememberUpdatedState(cards)
    val lock = remember { Mutex() }
    val handler =
        remember(repository, ledger, undoController, memberId, readOnly) {
            DowntimeHandler(
                sheet,
                repository,
                ledger,
                undoController,
                memberId = memberId,
                readOnly = readOnly,
                lock = lock,
                cardOf = { id -> latestCards.firstOrNull { it.id == id } },
            )
        }
    // an open window follows its downtime and the rights of now, and goes when the downtime is gone
    val shown =
        when (val open = sheet.value) {
            is DowntimeSheetState.Detail ->
                cards
                    .firstOrNull { it.id == open.card.id }
                    ?.let { open.copy(card = it, canApply = memberId != null, readOnly = readOnly) }
                    ?: DowntimeSheetState.Hidden
            is DowntimeSheetState.Edit ->
                if (readOnly) DowntimeSheetState.Hidden else open.copy(canApply = memberId != null)
            DowntimeSheetState.Hidden -> open
        }
    return DowntimeHost(cards, shown, handler)
}
