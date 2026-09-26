package com.lyrismet.dndcodex.data

import com.lyrismet.dndcodex.data.db.DatabaseDriverFactory
import com.lyrismet.dndcodex.data.db.createAppDatabase
import com.lyrismet.dndcodex.data.repository.NpcRepositoryImpl
import com.lyrismet.dndcodex.data.repository.SessionEntryRepositoryImpl
import com.lyrismet.dndcodex.data.repository.SessionNoteRepositoryImpl
import com.lyrismet.dndcodex.domain.repository.NpcRepository
import com.lyrismet.dndcodex.domain.repository.SessionEntryRepository
import com.lyrismet.dndcodex.domain.repository.SessionNoteRepository
import com.lyrismet.dndcodex.presentation.npcdetail.addNpcDetailUi
import com.lyrismet.dndcodex.presentation.npcdetail.npcDetailScreenRegistration
import com.lyrismet.dndcodex.presentation.npclist.addNpcListUi
import com.lyrismet.dndcodex.presentation.npclist.npcListScreenRegistration
import com.lyrismet.dndcodex.presentation.sessiondetail.addSessionDetailUi
import com.lyrismet.dndcodex.presentation.sessiondetail.sessionDetailScreenRegistration
import com.lyrismet.dndcodex.presentation.sessionlist.addSessionListUi
import com.lyrismet.dndcodex.presentation.sessionlist.sessionListScreenRegistration
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.serialization.SerializableCircuitSaver

class AppContainer(
    databaseDriverFactory: DatabaseDriverFactory,
) {
    private val database = createAppDatabase(databaseDriverFactory.createDriver())

    val npcRepository: NpcRepository = NpcRepositoryImpl(database)
    val sessionNoteRepository: SessionNoteRepository = SessionNoteRepositoryImpl(database)
    val sessionEntryRepository: SessionEntryRepository = SessionEntryRepositoryImpl(database)

    val circuit: Circuit =
        Circuit
            .Builder()
            .addSessionListUi(sessionNoteRepository)
            .addSessionDetailUi(sessionNoteRepository, sessionEntryRepository)
            .addNpcListUi(npcRepository)
            .addNpcDetailUi(npcRepository)
            .setCircuitSaver(
                SerializableCircuitSaver(
                    listOf(
                        sessionListScreenRegistration,
                        sessionDetailScreenRegistration,
                        npcListScreenRegistration,
                        npcDetailScreenRegistration,
                    ),
                ),
            ).build()
}
