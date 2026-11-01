package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LinkSuggestionDismissalRepositoryTest {
    private val db = TestDatabase()
    private val repository = LinkSuggestionDismissalRepositoryImpl(db.database, db.current)
    private val owner = MentionRef(EntityKind.NPC, 1)
    private val place = MentionRef(EntityKind.LOCATION, 2)

    @Test
    fun `a refusal is remembered for its record only`() =
        runTest {
            repository.dismiss(owner, place)
            repository.dismiss(owner, place)

            assertEquals(setOf(place), repository.observeDismissed(owner).first())
            assertEquals(emptySet(), repository.observeDismissed(MentionRef(EntityKind.NPC, 7)).first())
        }

    @Test
    fun `a refusal belongs to its campaign`() =
        runTest {
            repository.dismiss(owner, place)
            db.addCampaign(TestDatabase.SECOND_CAMPAIGN)
            db.current.open(TestDatabase.SECOND_CAMPAIGN)

            assertEquals(emptySet(), repository.observeDismissed(owner).first())
        }

    @Test
    fun `restoring a refusal asks again`() =
        runTest {
            repository.dismiss(owner, place)
            repository.restore(owner, place)

            assertEquals(emptySet(), repository.observeDismissed(owner).first())
        }
}
