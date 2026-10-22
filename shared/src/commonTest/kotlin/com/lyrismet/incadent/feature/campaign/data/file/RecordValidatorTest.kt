package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.feature.campaign.domain.model.ImportFailedException
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RecordValidatorTest {
    private val date = LocalDateTime.parse("2026-01-02T03:04")

    private fun accepts(vararg records: FileRecord) {
        val validator = RecordValidator()
        records.forEach(validator::accept)
    }

    private fun rejects(vararg records: FileRecord) {
        assertFailsWith<ImportFailedException> { accepts(*records) }
    }

    @Test
    fun `a record may point at one written above it`() {
        accepts(
            FileRecord.Species(1, "Wolf", CreatureType.BEAST),
            FileRecord.Place(1, "Town"),
            FileRecord.Place(2, "Inn", parentId = 1),
            FileRecord.Npc(1, "Bob", NpcStatus.NEUTRAL, speciesId = 1, placeIds = listOf(2)),
            FileRecord.Quest(1, "Find", QuestStatus.ACTIVE, giverNpcId = 1, placeId = 2),
        )
    }

    @Test
    fun `a place cannot be its own parent or point forward`() {
        rejects(FileRecord.Place(1, "Loop", parentId = 1))
        rejects(FileRecord.Place(1, "A", parentId = 2), FileRecord.Place(2, "B", parentId = 1))
    }

    @Test
    fun `a missing species place or giver is rejected`() {
        rejects(FileRecord.Npc(1, "Bob", NpcStatus.NEUTRAL, speciesId = 9))
        rejects(FileRecord.Npc(1, "Bob", NpcStatus.NEUTRAL, placeIds = listOf(9)))
        rejects(FileRecord.Quest(1, "Q", QuestStatus.ACTIVE, giverNpcId = 9))
        rejects(FileRecord.Quest(1, "Q", QuestStatus.ACTIVE, placeId = 9))
    }

    @Test
    fun `a repeated id is rejected`() {
        rejects(FileRecord.Place(1, "A"), FileRecord.Place(1, "B"))
        rejects(FileRecord.Session(1, "A", date), FileRecord.Session(1, "B", date))
    }

    @Test
    fun `an entry needs its session and a photo needs its owner`() {
        rejects(FileRecord.Entry(1, date, "text"))
        rejects(FileRecord.Photo(PhotoOwner.NPC, 1, "AAAA"))
        rejects(FileRecord.Place(1, "A"), FileRecord.Photo(PhotoOwner.NPC, 1, "AAAA"))
        accepts(FileRecord.Place(1, "A"), FileRecord.Photo(PhotoOwner.PLACE, 1, "AAAA"))
    }

    @Test
    fun `sections out of order are rejected`() {
        rejects(FileRecord.Session(1, "A", date), FileRecord.Place(1, "Town"))
    }
}
