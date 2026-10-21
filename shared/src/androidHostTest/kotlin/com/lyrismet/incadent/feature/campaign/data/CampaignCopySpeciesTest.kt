package com.lyrismet.incadent.feature.campaign.data

import com.lyrismet.incadent.feature.campaign.domain.model.CampaignTransfer
import com.lyrismet.incadent.feature.codex.data.NpcRepositoryImpl
import com.lyrismet.incadent.feature.codex.data.SpeciesRepositoryImpl
import com.lyrismet.incadent.feature.codex.domain.repository.quickNpc
import com.lyrismet.incadent.feature.codex.domain.repository.quickSpecies
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class CampaignCopySpeciesTest {
    private val db = TestDatabase()
    private val campaigns = CampaignRepositoryImpl(db.database, db.current)
    private val npcs = NpcRepositoryImpl(db.database, db.current)
    private val species = SpeciesRepositoryImpl(db.database, db.current)

    @Test
    fun `copied npcs keep their species as one shared copy in the new campaign`() =
        runTest {
            val goblinId = species.upsert(quickSpecies("Гоблин"))
            val first = npcs.upsert(quickNpc("Крик").copy(speciesId = goblinId))
            val second = npcs.upsert(quickNpc("Гнык").copy(speciesId = goblinId))
            val loner = npcs.upsert(quickNpc("Одиночка"))

            campaigns.create(
                "Копия",
                CampaignTransfer(TestDatabase.FIRST_CAMPAIGN, npcIds = setOf(first, second, loner)),
            )

            val copiedSpecies = species.observeAll().first()
            assertEquals(listOf("Гоблин"), copiedSpecies.map { it.name })
            assertNotEquals(goblinId, copiedSpecies.single().id)
            val links = npcs.observeAll().first().associate { it.name to it.speciesId }
            assertEquals(copiedSpecies.single().id, links["Крик"])
            assertEquals(copiedSpecies.single().id, links["Гнык"])
            assertEquals(null, links["Одиночка"])
            assertNotNull(links["Крик"])
        }

    @Test
    fun `species no npc points at are copied too`() =
        runTest {
            species.upsert(quickSpecies("Волк"))
            species.upsert(quickSpecies("Гоблин"))

            campaigns.create("Копия", CampaignTransfer(TestDatabase.FIRST_CAMPAIGN))

            assertEquals(
                listOf("Волк", "Гоблин"),
                species
                    .observeAll()
                    .first()
                    .map { it.name }
                    .sorted(),
            )
        }
}
