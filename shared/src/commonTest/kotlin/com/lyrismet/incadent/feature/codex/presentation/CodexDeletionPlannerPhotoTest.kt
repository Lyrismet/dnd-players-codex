package com.lyrismet.incadent.feature.codex.presentation

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.FakeLocationRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.FakeNpcRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.FakePartyRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.FakeQuestRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

private val thumb = byteArrayOf(1)
private val full = byteArrayOf(1, 2, 3)
private val labels = EntityKind.entries.associateWith { it.name }

// the list rows carry only the thumbnail, the repository record carries the compressed full picture too
private fun listRecords(
    parties: List<PartyMember> = emptyList(),
    npcs: List<Npc> = emptyList(),
    locations: List<Location> = emptyList(),
) = CodexRecords(parties = parties, npcs = npcs, quests = emptyList(), locations = locations)

class CodexDeletionPlannerPhotoTest {
    @Test
    fun `undoing an npc delete brings back the full picture not just the thumbnail`() =
        runTest {
            val stored =
                Npc(1, "Громм", NpcStatus.FRIEND, NpcLifeState.ALIVE, "", emptyList(), "", Photo(thumb, full))
            val npcs = FakeNpcRepository(stored)
            val planner = planner(npcs = npcs)
            val plan =
                planner.planFor(
                    EntityRef.Npc(1),
                    listRecords(npcs = listOf(stored.copy(portrait = Photo(thumb)))),
                    labels,
                )!!

            plan.delete()
            plan.restore()

            assertContentEquals(
                full,
                npcs.all
                    .single()
                    .portrait
                    ?.full,
            )
        }

    @Test
    fun `undoing a party delete brings back the full picture not just the thumbnail`() =
        runTest {
            val stored =
                PartyMember(1, "Лира", "", "", null, "", true, PartyPresence.IN, null, null, "", Photo(thumb, full))
            val party = FakePartyRepository(stored)
            val plan =
                planner(party = party)
                    .planFor(
                        EntityRef.Party(1),
                        listRecords(parties = listOf(stored.copy(portrait = Photo(thumb)))),
                        labels,
                    )!!

            plan.delete()
            plan.restore()

            assertContentEquals(
                full,
                party.all
                    .single()
                    .portrait
                    ?.full,
            )
        }

    @Test
    fun `undoing a place delete brings back the full cover not just the thumbnail`() =
        runTest {
            val stored = Location(1, "Таверна", "", cover = Photo(thumb, full))
            val locations = FakeLocationRepository(stored)
            val plan =
                planner(locations = locations)
                    .planFor(
                        EntityRef.Location(1),
                        listRecords(locations = listOf(stored.copy(cover = Photo(thumb)))),
                        labels,
                    )!!

            plan.delete()
            plan.restore()

            assertContentEquals(
                full,
                locations.all
                    .single()
                    .cover
                    ?.full,
            )
        }

    private fun planner(
        npcs: FakeNpcRepository = FakeNpcRepository(),
        party: FakePartyRepository = FakePartyRepository(),
        locations: FakeLocationRepository = FakeLocationRepository(),
    ) = CodexDeletionPlanner(npcs, party, FakeQuestRepository(), locations)
}
