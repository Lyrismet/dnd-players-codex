package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.domain.repository.createNamed
import com.lyrismet.incadent.feature.codex.domain.repository.deleteRecord
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class QuickRecordsTest {
    private val db = TestDatabase()
    private val repositories =
        CodexRepositories(
            PartyRepositoryImpl(db.database, db.current),
            NpcRepositoryImpl(db.database, db.current),
            QuestRepositoryImpl(db.database, db.current),
            LocationRepositoryImpl(db.database, db.current),
            SpeciesRepositoryImpl(db.database, db.current),
        )

    @Test
    fun `a named record of each kind is created and deleted again`() =
        runTest {
            val ids = EntityKind.entries.associateWith { repositories.createNamed(it, "Новичок") }

            assertEquals(
                listOf("Новичок"),
                repositories.npcRepository
                    .observeAll()
                    .first()
                    .map { it.name },
            )
            assertEquals(
                listOf("Новичок"),
                repositories.questRepository
                    .observeAll()
                    .first()
                    .map { it.title },
            )
            assertEquals(
                listOf("Новичок"),
                repositories.locationRepository
                    .observeAll()
                    .first()
                    .map { it.name },
            )
            assertEquals(
                listOf("Новичок"),
                repositories.partyRepository
                    .observeAll()
                    .first()
                    .map { it.name },
            )

            assertEquals(
                listOf("Новичок"),
                repositories.speciesRepository
                    .observeAll()
                    .first()
                    .map { it.name },
            )

            ids.forEach { (kind, id) -> repositories.deleteRecord(kind, id) }

            assertEquals(emptyList(), repositories.npcRepository.observeAll().first())
            assertEquals(emptyList(), repositories.questRepository.observeAll().first())
            assertEquals(emptyList(), repositories.locationRepository.observeAll().first())
            assertEquals(emptyList(), repositories.partyRepository.observeAll().first())
            assertEquals(emptyList(), repositories.speciesRepository.observeAll().first())
        }
}
