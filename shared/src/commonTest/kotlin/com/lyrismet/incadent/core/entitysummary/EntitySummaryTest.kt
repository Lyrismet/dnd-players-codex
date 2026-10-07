package com.lyrismet.incadent.core.entitysummary

import com.lyrismet.incadent.core.mention.mentionCandidates
import com.lyrismet.incadent.core.mention.mentionEntitiesFrom
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.model.SessionEntry
import com.lyrismet.incadent.domain.model.SessionNote
import com.lyrismet.incadent.domain.model.SessionNumbering
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val QUEST_PREFIX = "Квест: "

private val npcStatusLabels =
    mapOf(
        NpcStatus.FRIEND to "Друг",
        NpcStatus.ENEMY to "Враг",
        NpcStatus.NEUTRAL to "Нейтрал",
    )

private val npcLifeLabels =
    mapOf(
        NpcLifeState.ALIVE to "Жив",
        NpcLifeState.DEAD to "Мёртв",
    )

private val partyPresenceLabels =
    mapOf(
        PartyPresence.IN to "В составе",
        PartyPresence.AWAY to "Отсутствует",
    )

private val questStatusLabels =
    mapOf(
        QuestStatus.ACTIVE to "Активен",
        QuestStatus.COMPLETED to "Выполнен",
        QuestStatus.FAILED to "Провален",
    )

private val ragnar =
    Npc(
        id = 1,
        name = "Рагнар",
        status = NpcStatus.FRIEND,
        lifeState = NpcLifeState.ALIVE,
        description = "Кузнец",
        locationId = 2,
        race = "Дварф",
        faction = "Гильдия",
    )
private val lira =
    PartyMember(
        id = 7,
        name = "Лира",
        characterClass = "Волшебница",
        race = "Эльфийка",
        level = 5,
        playerName = "",
        isPlayerCharacter = true,
        presence = PartyPresence.IN,
        hpMax = 28,
        hpCurrent = 22,
        armorClass = 12,
        initiativeBonus = 3,
        description = "Школа Воплощения",
        portraitUri = null,
    )
private val forge = Location(id = 2, name = "Кузница", type = "Мастерская", description = "", region = "Север")
private val swordQuest =
    Quest(id = 3, title = "Меч", status = QuestStatus.ACTIVE, reward = "50 золота", givenByNpcId = 1, locationId = null)

private val mentioningNote =
    SessionNote(id = 100, title = "Встреча", sessionDate = LocalDateTime(2026, 9, 1, 10, 0), endedAt = null)
private val mentioningEntry =
    SessionEntry(
        id = 1000,
        sessionNoteId = 100,
        createdAt = LocalDateTime(2026, 9, 1, 10, 5),
        body = "Поговорили с @Рагнар про меч",
    )
private val unrelatedNote =
    SessionNote(id = 200, title = "Другая", sessionDate = LocalDateTime(2026, 9, 2, 10, 0), endedAt = null)
private val unrelatedEntry =
    SessionEntry(
        id = 2000,
        sessionNoteId = 200,
        createdAt = LocalDateTime(2026, 9, 2, 10, 5),
        body = "Ничего интересного",
    )

private fun lookup(
    npcs: List<Npc> = listOf(ragnar),
    locations: List<Location> = listOf(forge),
    quests: List<Quest> = listOf(swordQuest),
    numbering: SessionNumbering = SessionNumbering.ROMAN,
    parties: List<PartyMember> = listOf(lira),
): EntityLookup {
    val candidates = mentionCandidates(mentionEntitiesFrom(npcs, locations, quests), QUEST_PREFIX)
    return EntityLookup(
        npcs = npcs,
        locations = locations,
        quests = quests,
        npcStatusLabels = npcStatusLabels,
        npcLifeLabels = npcLifeLabels,
        questStatusLabels = questStatusLabels,
        parties = parties,
        partyPresenceLabels = partyPresenceLabels,
        partyYouLabel = "Вы",
        sessionNotes = listOf(mentioningNote, unrelatedNote),
        sessionEntries = listOf(mentioningEntry, unrelatedEntry),
        mentionCandidates = candidates,
        sessionNumbering = numbering,
    )
}

class EntitySummaryTest {
    @Test
    fun `buildEntitySummary returns null when the ref cannot be resolved`() {
        assertNull(buildEntitySummary(EntityRef.Npc(999), lookup()))
    }

    @Test
    fun `buildEntitySummary for an npc includes its location and given quests`() {
        val summary = buildEntitySummary(EntityRef.Npc(1), lookup()) as EntitySummaryItem.NpcSummary

        assertEquals("Рагнар", summary.name)
        assertEquals("Дварф · Гильдия", summary.subtitle)
        assertEquals(3, summary.facts.size)
        assertEquals(1, summary.groups.size)
        assertEquals(
            1,
            summary.groups
                .single()
                .items.size,
        )
    }

    @Test
    fun `buildEntitySummary for an npc without a location keeps the location row as a dash and omits the groups`() {
        val loner = ragnar.copy(id = 5, locationId = null)
        val summary = buildEntitySummary(EntityRef.Npc(5), lookup(npcs = listOf(loner))) as EntitySummaryItem.NpcSummary

        assertEquals(3, summary.facts.size)
        assertEquals(FactValue.Text(""), summary.facts.last().value)
        assertTrue(summary.groups.isEmpty())
    }

    @Test
    fun `buildEntitySummary for a quest includes the giver and location when present`() {
        val summary = buildEntitySummary(EntityRef.Quest(3), lookup()) as EntitySummaryItem.QuestSummary

        assertEquals("Меч", summary.title)
        // giver, reward and location - swordQuest has no location, so that row is a dash
        assertEquals(3, summary.facts.size)
        assertEquals(FactValue.Text(""), summary.facts.last().value)
    }

    @Test
    fun `buildEntitySummary for a quest carries its description`() {
        val described = swordQuest.copy(id = 7, description = "Найти три фрагмента")
        val summary =
            buildEntitySummary(EntityRef.Quest(7), lookup(quests = listOf(described))) as EntitySummaryItem.QuestSummary

        assertEquals("Найти три фрагмента", summary.description)
    }

    @Test
    fun `buildEntitySummary for a quest without a giver keeps the giver row as a dash`() {
        val orphanQuest = swordQuest.copy(id = 6, givenByNpcId = null)
        val summary =
            buildEntitySummary(
                EntityRef.Quest(6),
                lookup(quests = listOf(orphanQuest)),
            ) as EntitySummaryItem.QuestSummary

        assertEquals(FactValue.Text(""), summary.facts.first().value)
    }

    @Test
    fun `buildEntitySummary for a location groups the npcs and quests found there`() {
        val questAtForge = swordQuest.copy(locationId = 2)
        val summary =
            buildEntitySummary(
                EntityRef.Location(2),
                lookup(quests = listOf(questAtForge)),
            ) as EntitySummaryItem.LocationSummary

        assertEquals(2, summary.groups.size)
    }

    @Test
    fun `buildEntitySummary for a location with nothing there has no groups`() {
        val emptyLocation = Location(id = 9, name = "Пустошь", type = "Дикая земля", description = "", region = "Юг")
        val summary =
            buildEntitySummary(
                EntityRef.Location(9),
                lookup(locations = listOf(emptyLocation)),
            ) as EntitySummaryItem.LocationSummary

        assertTrue(summary.groups.isEmpty())
    }

    @Test
    fun `buildEntitySummary finds only the session note that actually mentions the entity`() {
        val summary = buildEntitySummary(EntityRef.Npc(1), lookup()) as EntitySummaryItem.NpcSummary

        assertEquals(1, summary.relatedNotes.size)
        val related = summary.relatedNotes.single()
        assertEquals(100L, related.sessionNoteId)
        assertEquals(1000L, related.entryId)
        assertEquals(1, related.matchCount)
    }

    @Test
    fun `buildEntitySummary labels related notes with the chosen session numbering`() {
        val roman = buildEntitySummary(EntityRef.Npc(1), lookup()) as EntitySummaryItem.NpcSummary
        val arabic =
            buildEntitySummary(
                EntityRef.Npc(1),
                lookup(numbering = SessionNumbering.ARABIC),
            ) as EntitySummaryItem.NpcSummary

        assertEquals("I", roman.relatedNotes.single().numberLabel)
        assertEquals("1", arabic.relatedNotes.single().numberLabel)
    }

    @Test
    fun `buildEntitySummary for a dead npc keeps the relation and marks the condition`() {
        val dead = ragnar.copy(lifeState = NpcLifeState.DEAD)
        val summary = buildEntitySummary(EntityRef.Npc(1), lookup(npcs = listOf(dead))) as EntitySummaryItem.NpcSummary

        assertTrue(summary.isDead)
        assertEquals("Друг · Мёртв", summary.statusLabel)
    }

    @Test
    fun `buildEntitySummary for a party member shows its class and the you flag`() {
        val summary = buildEntitySummary(EntityRef.Party(7), lookup()) as EntitySummaryItem.PartySummary

        assertEquals("Волшебница · Вы", summary.overlineValue)
        assertEquals("Л", summary.emblem.text)
        assertEquals("Вы", (summary.facts.first().value as FactValue.Text).text)
    }

    @Test
    fun `buildEntitySummary returns null for an unknown party member`() {
        assertNull(buildEntitySummary(EntityRef.Party(99), lookup()))
    }
}
