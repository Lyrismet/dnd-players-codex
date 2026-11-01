package com.lyrismet.incadent.feature.campaign.data

import com.lyrismet.incadent.core.preferences.AppPreferencesRepositoryImpl
import com.lyrismet.incadent.feature.phrases.data.PhraseSnippetRepositoryImpl
import com.lyrismet.incadent.persistence.TestDatabase
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CampaignStarterPhrasesTest {
    private val db = TestDatabase()
    private val campaigns = CampaignRepositoryImpl(db.database, db.current, AppPreferencesRepositoryImpl(MapSettings()))
    private val phrases = PhraseSnippetRepositoryImpl(db.database, db.current)

    @Test
    fun `a new campaign opens with its starter phrases pinned`() =
        runTest {
            campaigns.create("Новая", null, listOf("Бросьте инициативу", "Что вы делаете"))

            assertEquals(listOf("Бросьте инициативу", "Что вы делаете"), phrases.observeAll().first())
        }

    @Test
    fun `a campaign made without starter phrases has none`() =
        runTest {
            campaigns.create("Новая", null)

            assertEquals(emptyList(), phrases.observeAll().first())
        }
}
