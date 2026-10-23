package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import kotlin.test.Test
import kotlin.test.assertEquals

class CampaignFileCompatTest {
    @Test
    fun `a species line written under the old damage name is still read`() {
        val line = """{"t":"species","id":1,"name":"Скелет","creatureType":"UNDEAD","damage":"FIRE:VULNERABILITY"}"""
        val record = FileJson.decodeFromString<FileRecord>(line)
        assertEquals(FileRecord.Species(1, "Скелет", CreatureType.UNDEAD, "FIRE:VULNERABILITY"), record)
    }

    @Test
    fun `an npc line without vulnerabilities reads as empty`() {
        val line = """{"t":"npc","id":2,"name":"Вазгар","status":"ENEMY"}"""
        val record = FileJson.decodeFromString<FileRecord>(line) as FileRecord.Npc
        assertEquals("", record.vulnerabilities)
    }
}
