package com.lyrismet.incadent.data.repository

import com.lyrismet.incadent.domain.model.MentionStyle
import com.lyrismet.incadent.domain.model.SessionNumbering
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppPreferencesRepositoryImplTest {
    @Test
    fun `campaign name is null until it is set and then comes back trimmed`() =
        runTest {
            val repository = AppPreferencesRepositoryImpl(MapSettings())

            assertNull(repository.observeCampaignName().first())

            repository.setCampaignName("  Тени Блэквуда  ")

            assertEquals("Тени Блэквуда", repository.observeCampaignName().first())
        }

    @Test
    fun `blank campaign name clears the override back to the default`() =
        runTest {
            val repository = AppPreferencesRepositoryImpl(MapSettings())
            repository.setCampaignName("Тени")

            repository.setCampaignName("   ")

            assertNull(repository.observeCampaignName().first())
        }

    @Test
    fun `session numbering defaults to roman and round-trips the chosen style`() =
        runTest {
            val repository = AppPreferencesRepositoryImpl(MapSettings())
            assertEquals(SessionNumbering.ROMAN, repository.observeSessionNumbering().first())

            repository.setSessionNumbering(SessionNumbering.ARABIC)

            assertEquals(SessionNumbering.ARABIC, repository.observeSessionNumbering().first())
        }

    @Test
    fun `unknown stored session numbering falls back to roman`() =
        runTest {
            val settings = MapSettings("session_numbering" to "bogus")

            assertEquals(
                SessionNumbering.ROMAN,
                AppPreferencesRepositoryImpl(settings).observeSessionNumbering().first(),
            )
        }

    @Test
    fun `mention style defaults to filled and round-trips the chosen style`() =
        runTest {
            val repository = AppPreferencesRepositoryImpl(MapSettings())
            assertEquals(MentionStyle.FILLED, repository.observeMentionStyle().first())

            repository.setMentionStyle(MentionStyle.UNDERLINE)

            assertEquals(MentionStyle.UNDERLINE, repository.observeMentionStyle().first())
        }

    @Test
    fun `unknown stored mention style falls back to filled`() =
        runTest {
            val settings = MapSettings("mention_style" to "bogus")

            assertEquals(MentionStyle.FILLED, AppPreferencesRepositoryImpl(settings).observeMentionStyle().first())
        }
}
