package com.lyrismet.incadent.feature.session.presentation.list.downtime

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.session.domain.DowntimeLedger
import com.lyrismet.incadent.feature.session.domain.model.Downtime
import com.lyrismet.incadent.feature.session.domain.repository.DowntimeRepository
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.downtime_applied_subtitle
import incadent.shared.generated.resources.downtime_applied_title
import incadent.shared.generated.resources.downtime_deleted_applied_suffix
import incadent.shared.generated.resources.downtime_deleted_title
import incadent.shared.generated.resources.downtime_gold_format
import incadent.shared.generated.resources.downtime_item_format
import incadent.shared.generated.resources.downtime_reverted_subtitle
import incadent.shared.generated.resources.downtime_reverted_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString

/** turns the presses of the downtime card, its window and its editor into writes, a closed campaign takes none */
@Suppress("LongParameterList")
internal class DowntimeHandler(
    private val sheet: MutableState<DowntimeSheetState>,
    private val repository: DowntimeRepository,
    private val ledger: DowntimeLedger,
    private val undoController: UndoController,
    // the character marked as mine, whose sheet takes the gold and the things
    private val memberId: Long?,
    private val readOnly: Boolean,
    // one write at a time, so a double press cannot put the loot into the sheet twice
    private val lock: Mutex,
    private val cardOf: (downtimeId: Long) -> DowntimeCard?,
) {
    fun onEvent(
        scope: CoroutineScope,
        event: DowntimeEvent,
    ) {
        val open = sheet.value
        when (event) {
            DowntimeEvent.Dismissed -> sheet.value = DowntimeSheetState.Hidden
            is DowntimeEvent.CardClicked ->
                cardOf(event.id)?.let { sheet.value = DowntimeSheetState.Detail(it, memberId != null, readOnly) }
            is DowntimeEvent.DraftChanged -> if (open is DowntimeSheetState.Edit) sheet.value = open.copy(event.draft)
            else -> if (!readOnly) onWrite(scope, event, open)
        }
    }

    private fun onWrite(
        scope: CoroutineScope,
        event: DowntimeEvent,
        open: DowntimeSheetState,
    ) {
        val card = (open as? DowntimeSheetState.Detail)?.card
        when (event) {
            is DowntimeEvent.NewClicked ->
                sheet.value =
                    DowntimeSheetState.Edit(
                        DowntimeDraft(event.afterSessionId, event.afterLabel, event.nextLabel),
                        memberId != null,
                    )
            DowntimeEvent.EditClicked ->
                card?.let {
                    sheet.value =
                        DowntimeSheetState.Edit(it.toDraft(), memberId != null)
                }
            DowntimeEvent.SaveClicked ->
                (open as? DowntimeSheetState.Edit)?.let { scope.launch { lock.withLock { save(it.draft) } } }
            DowntimeEvent.DeleteClicked -> card?.let { scope.launch { lock.withLock { delete(it) } } }
            DowntimeEvent.ApplyClicked -> card?.let { scope.launch { lock.withLock { applyStored(it.id) } } }
            DowntimeEvent.RevertClicked -> card?.let { scope.launch { lock.withLock { revertStored(it.id) } } }
            else -> Unit
        }
    }

    private suspend fun save(draft: DowntimeDraft) {
        if (!draft.canSave) return
        val previous = draft.id.takeIf { it != 0L }?.let { repository.getById(it) }
        val next = draft.toDowntime(previous)
        sheet.value = DowntimeSheetState.Hidden
        val wasApplied = previous?.isApplied == true
        if (previous != null && wasApplied && next.sameLootAs(previous)) {
            repository.upsert(next.copy(items = previous.items))
            return
        }
        if (previous != null && wasApplied) ledger.revert(previous)
        val plain = next.copy(appliedTo = null)
        val id = repository.upsert(plain)
        if (memberId != null && plain.hasLoot) applyStored(id)
    }

    // puts the gold and things of a stored downtime into the sheet and offers to take them out again
    private suspend fun applyStored(id: Long) {
        val member = memberId ?: return
        val downtime = repository.getById(id)?.takeIf { !it.isApplied } ?: return
        putIn(id, member)
        undoController.show(
            getString(Res.string.downtime_applied_title, lootSummary(downtime)),
            getString(Res.string.downtime_applied_subtitle),
        ) {
            lock.withLock { takeOut(id) }
        }
    }

    private suspend fun revertStored(id: Long) {
        val member = repository.getById(id)?.takeIf { it.isApplied }?.appliedTo ?: return
        val downtime = takeOut(id) ?: return
        undoController.show(
            getString(Res.string.downtime_reverted_title, lootSummary(downtime)),
            getString(Res.string.downtime_reverted_subtitle),
        ) {
            lock.withLock { putIn(id, member) }
        }
    }

    // the downtime as it was before the things came out
    private suspend fun takeOut(id: Long): Downtime? {
        val downtime = repository.getById(id)?.takeIf { it.isApplied } ?: return null
        // not cancelled halfway, the sheet and the record would then disagree
        withContext(NonCancellable) { repository.upsert(ledger.revert(downtime)) }
        return downtime
    }

    private suspend fun putIn(
        id: Long,
        member: Long,
    ) {
        val downtime = repository.getById(id)?.takeIf { !it.isApplied } ?: return
        withContext(NonCancellable) { repository.upsert(ledger.apply(downtime, member)) }
    }

    // the sheet gets back what the record put in, undoing the delete puts it in again
    private suspend fun delete(card: DowntimeCard) {
        val downtime = repository.getById(card.id) ?: return
        val member = downtime.appliedTo
        if (downtime.isApplied) ledger.revert(downtime)
        repository.delete(card.id)
        sheet.value = DowntimeSheetState.Hidden
        val subtitle =
            if (member !=
                null
            ) {
                getString(Res.string.downtime_deleted_applied_suffix, card.shortText)
            } else {
                card.shortText
            }
        undoController.show(getString(Res.string.downtime_deleted_title), subtitle) {
            lock.withLock {
                val plain =
                    downtime.copy(
                        id = 0,
                        appliedTo = null,
                        appliedGold = 0,
                        items = downtime.items.map { it.copy(itemId = 0) },
                    )
                val restored = repository.upsert(plain)
                if (member != null) putIn(restored, member)
            }
        }
    }
}

/** "+120 gp · Healing potion ×2", the gold and the things a downtime puts into a sheet */
private suspend fun lootSummary(downtime: Downtime): String {
    val parts = mutableListOf<String>()
    if (downtime.goldDelta != 0) {
        parts += getString(Res.string.downtime_gold_format, signedGold(downtime.goldDelta))
    }
    downtime.items.forEach { parts += getString(Res.string.downtime_item_format, it.name, it.quantity) }
    return parts.joinToString(" · ")
}
