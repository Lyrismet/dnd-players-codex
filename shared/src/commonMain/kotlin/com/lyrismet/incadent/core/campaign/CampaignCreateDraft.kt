package com.lyrismet.incadent.core.campaign

import com.lyrismet.incadent.domain.model.CampaignTransfer
import com.lyrismet.incadent.domain.model.TransferCandidate
import com.lyrismet.incadent.domain.model.TransferCandidates

/**
 * the new-campaign form. Places start all checked and npcs all unchecked, so only the exceptions are stored - a source
 * with no candidates loaded yet has nothing to check either way.
 */
data class CampaignCreateDraft(
    val name: String = "",
    val sameWorld: Boolean = false,
    val sourceId: String? = null,
    val copyTags: Boolean = true,
    val copyPhrases: Boolean = true,
    val step: CampaignCreateStep = CampaignCreateStep.FORM,
    val tab: CampaignTransferTab = CampaignTransferTab.PLACES,
    val query: String = "",
    val uncheckedPlaceIds: Set<Long> = emptySet(),
    val checkedNpcIds: Set<Long> = emptySet(),
    // set once create is launched so a second tap cannot insert a duplicate
    val submitting: Boolean = false,
) {
    /** a same-world campaign needs its source, and every campaign its name */
    val canProceed: Boolean
        get() = savableCampaignName(name) != null && (!sameWorld || sourceId != null)

    fun withWorld(sameWorld: Boolean): CampaignCreateDraft =
        copy(sameWorld = sameWorld, sourceId = if (sameWorld) sourceId else null)

    // a different source starts a different selection
    fun withSource(id: String): CampaignCreateDraft =
        if (id == sourceId) {
            this
        } else {
            copy(sourceId = id, uncheckedPlaceIds = emptySet(), checkedNpcIds = emptySet())
        }

    fun withItemToggled(
        id: Long,
        candidates: TransferCandidates,
    ): CampaignCreateDraft =
        when (tab) {
            CampaignTransferTab.PLACES -> copy(uncheckedPlaceIds = uncheckedPlaceIds.toggled(id))
            CampaignTransferTab.NPCS -> copy(checkedNpcIds = checkedNpcIds.toggled(id))
        }.takeIf { candidates.hasId(tab, id) } ?: this

    /** checks every row of the open tab, or clears them when they were all checked already */
    fun withAllToggled(candidates: TransferCandidates): CampaignCreateDraft {
        val ids = candidates.of(tab).map { it.id }.toSet()
        return when (tab) {
            CampaignTransferTab.PLACES ->
                copy(uncheckedPlaceIds = if (uncheckedPlaceIds.none { it in ids }) ids else emptySet())
            CampaignTransferTab.NPCS ->
                copy(checkedNpcIds = if (ids.isNotEmpty() && ids.all { it in checkedNpcIds }) emptySet() else ids)
        }
    }

    fun isChecked(
        tab: CampaignTransferTab,
        id: Long,
    ): Boolean =
        when (tab) {
            CampaignTransferTab.PLACES -> id !in uncheckedPlaceIds
            CampaignTransferTab.NPCS -> id in checkedNpcIds
        }

    fun checkedCount(
        tab: CampaignTransferTab,
        candidates: TransferCandidates,
    ): Int = candidates.of(tab).count { isChecked(tab, it.id) }

    fun transferCount(candidates: TransferCandidates): Int =
        CampaignTransferTab.entries.sumOf { checkedCount(it, candidates) }

    /** null when the world is new - nothing is copied, a same-world campaign copies what is checked */
    fun toTransfer(candidates: TransferCandidates): CampaignTransfer? {
        val source = sourceId?.takeIf { sameWorld } ?: return null
        return CampaignTransfer(
            sourceId = source,
            placeIds =
                candidates.places
                    .map { it.id }
                    .filter { isChecked(CampaignTransferTab.PLACES, it) }
                    .toSet(),
            npcIds =
                candidates.npcs
                    .map { it.id }
                    .filter { isChecked(CampaignTransferTab.NPCS, it) }
                    .toSet(),
            copyTags = copyTags,
            copyPhrases = copyPhrases,
        )
    }

    /** the open tab's rows that match the search, by name or by the subtitle that tells same-named ones apart */
    fun visibleRows(candidates: TransferCandidates): List<CampaignTransferRow> {
        val needle = query.trim()
        return candidates
            .of(tab)
            .filter { needle.isEmpty() || it.matches(needle) }
            .map { CampaignTransferRow(it.id, it.name, it.subtitle, isChecked(tab, it.id)) }
    }

    private fun TransferCandidate.matches(needle: String): Boolean =
        name.contains(needle, ignoreCase = true) || subtitle.contains(needle, ignoreCase = true)
}

private fun Set<Long>.toggled(id: Long): Set<Long> = if (id in this) this - id else this + id

private fun TransferCandidates.of(tab: CampaignTransferTab): List<TransferCandidate> =
    when (tab) {
        CampaignTransferTab.PLACES -> places
        CampaignTransferTab.NPCS -> npcs
    }

private fun TransferCandidates.hasId(
    tab: CampaignTransferTab,
    id: Long,
): Boolean = of(tab).any { it.id == id }
