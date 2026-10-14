package com.lyrismet.incadent.core.campaign

import com.lyrismet.incadent.domain.model.TransferCandidate
import com.lyrismet.incadent.domain.model.TransferCandidates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CampaignCreateDraftTest {
    private val candidates =
        TransferCandidates(
            places =
                listOf(
                    TransferCandidate(1, "Сан-Читлан", ""),
                    TransferCandidate(2, "Порт", "Сан-Читлан"),
                ),
            npcs = listOf(TransferCandidate(10, "Хозяин таверны", "Порт")),
        )

    @Test
    fun `a blank name cannot proceed`() {
        assertFalse(CampaignCreateDraft(name = "  ").canProceed)
        assertTrue(CampaignCreateDraft(name = "Морской дракон").canProceed)
    }

    @Test
    fun `same world needs a source before it can proceed`() {
        val draft = CampaignCreateDraft(name = "Дракон").withWorld(sameWorld = true)

        assertFalse(draft.canProceed)
        assertTrue(draft.withSource("c1").canProceed)
    }

    @Test
    fun `places start checked and npcs unchecked`() {
        val draft = CampaignCreateDraft(name = "X", sameWorld = true, sourceId = "c1")

        assertEquals(2, draft.checkedCount(CampaignTransferTab.PLACES, candidates))
        assertEquals(0, draft.checkedCount(CampaignTransferTab.NPCS, candidates))
    }

    @Test
    fun `toggling a place unchecks it and toggling again checks it`() {
        val draft = CampaignCreateDraft(sameWorld = true, sourceId = "c1")

        val unchecked = draft.withItemToggled(2, candidates)

        assertFalse(unchecked.isChecked(CampaignTransferTab.PLACES, 2))
        assertTrue(unchecked.withItemToggled(2, candidates).isChecked(CampaignTransferTab.PLACES, 2))
    }

    @Test
    fun `select all clears a fully checked tab and checks it again`() {
        val draft = CampaignCreateDraft(sameWorld = true, sourceId = "c1")

        val cleared = draft.withAllToggled(candidates)
        assertEquals(0, cleared.checkedCount(CampaignTransferTab.PLACES, candidates))

        assertEquals(2, cleared.withAllToggled(candidates).checkedCount(CampaignTransferTab.PLACES, candidates))
    }

    @Test
    fun `a different source resets the selection`() {
        val draft =
            CampaignCreateDraft(sameWorld = true, sourceId = "c1")
                .withItemToggled(1, candidates)
                .withSource("c2")

        assertTrue(draft.isChecked(CampaignTransferTab.PLACES, 1))
    }

    @Test
    fun `a new world transfers nothing`() {
        assertNull(CampaignCreateDraft(name = "X").toTransfer(candidates))
    }

    @Test
    fun `transfer carries the checked places and npcs and the tag flags`() {
        val transfer =
            checkNotNull(
                CampaignCreateDraft(sameWorld = true, sourceId = "c1", copyPhrases = false)
                    .withItemToggled(1, candidates)
                    .copy(tab = CampaignTransferTab.NPCS)
                    .withItemToggled(10, candidates)
                    .toTransfer(candidates),
            )

        assertEquals(setOf(2L), transfer.placeIds)
        assertEquals(setOf(10L), transfer.npcIds)
        assertTrue(transfer.copyTags)
        assertFalse(transfer.copyPhrases)
    }

    @Test
    fun `search matches the name or the path`() {
        val draft = CampaignCreateDraft(sameWorld = true, sourceId = "c1", query = "читлан")

        assertEquals(listOf(1L, 2L), draft.visibleRows(candidates).map { it.id })
        assertEquals(listOf(2L), draft.copy(query = "порт").visibleRows(candidates).map { it.id })
    }
}
