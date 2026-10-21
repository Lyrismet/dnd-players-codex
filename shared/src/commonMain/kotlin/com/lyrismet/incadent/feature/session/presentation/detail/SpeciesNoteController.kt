package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummaryItem
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.tokenQuantities
import com.lyrismet.incadent.feature.mention.domain.withTokenQuantity
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import com.lyrismet.incadent.feature.session.domain.repository.SessionEntryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** the species chip a card was opened from - the [occurrence]th token of that species in the note */
internal data class SpeciesNotePoint(
    val entryId: Long,
    val occurrence: Int,
    val speciesId: Long,
) {
    val ref: MentionRef get() = MentionRef(EntityKind.SPECIES, speciesId)
}

/** keeps the chip a species card came from, so the card can show and change how many the note counts */
internal class SpeciesNoteController(
    private val point: MutableState<SpeciesNotePoint?>,
    private val entryRepository: SessionEntryRepository,
) {
    fun open(opened: SpeciesNotePoint) {
        point.value = opened
    }

    fun clear() {
        point.value = null
    }

    /** puts the count of the remembered chip into the open card, a card of another species keeps none */
    fun withNoteQuantity(
        item: EntitySummaryItem?,
        entries: List<SessionEntry>,
    ): EntitySummaryItem? {
        val current = point.value
        if (item !is EntitySummaryItem.SpeciesSummary ||
            current == null ||
            item.ref.id != current.speciesId
        ) {
            return item
        }
        return item.copy(noteQuantity = quantityOf(current, entries))
    }

    /** moves the count by [delta] inside the note's text, without marking the note edited */
    fun step(
        scope: CoroutineScope,
        entries: List<SessionEntry>,
        delta: Int,
    ) {
        val current = point.value
        val entry = current?.let { shown -> entries.find { it.id == shown.entryId } }
        val quantity = current?.let { quantityOf(it, entries) }
        if (current == null || entry == null || quantity == null) return
        val body = withTokenQuantity(entry.body, current.ref, current.occurrence, quantity + delta)
        if (body != entry.body) scope.launch { entryRepository.rewriteBody(entry.id, body) }
    }

    private fun quantityOf(
        current: SpeciesNotePoint,
        entries: List<SessionEntry>,
    ): Int? =
        entries
            .find { it.id == current.entryId }
            ?.let { tokenQuantities(it.body, current.ref).getOrNull(current.occurrence) }
}
