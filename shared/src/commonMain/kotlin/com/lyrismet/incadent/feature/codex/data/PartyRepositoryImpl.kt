package com.lyrismet.incadent.feature.codex.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.campaign.observe
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.joinNicknames
import com.lyrismet.incadent.feature.codex.domain.model.parseNicknames
import com.lyrismet.incadent.feature.codex.domain.repository.PartyRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class PartyRepositoryImpl(
    private val database: AppDatabase,
    private val current: CurrentCampaign,
) : PartyRepository {
    private val queries = database.partyMemberQueries

    override fun observeAll(): Flow<List<PartyMember>> =
        current.observe { campaignId ->
            queries.selectAll(campaignId, ::toDomain).asFlow().mapToList(Dispatchers.Default)
        }

    override suspend fun getById(id: Long): PartyMember? =
        withContext(Dispatchers.Default) {
            queries.selectById(id, ::toDomainFull).executeAsOneOrNull()
        }

    // the "вы" flag is unique per campaign - saving a player character clears it from everyone else
    override suspend fun upsert(entity: PartyMember): Long =
        withContext(Dispatchers.Default) {
            database.transactionWithResult {
                val stored =
                    if (entity.id == 0L) null else queries.selectById(entity.id, ::toDomainFull).executeAsOneOrNull()
                val id =
                    if (stored != null) {
                        queries.update(
                            name = entity.name,
                            character_class = entity.characterClass,
                            race = entity.race,
                            level = entity.level?.toLong(),
                            player_name = entity.playerName,
                            is_player_character = entity.isPlayerCharacter,
                            presence = entity.presence,
                            hp_max = entity.hpMax?.toLong(),
                            hp_current = entity.hpCurrent?.toLong(),
                            description = entity.description,
                            nicknames = entity.nicknames.joinNicknames(),
                            id = entity.id,
                        )
                        writePortrait(entity.id, entity.portrait, stored.portrait)
                        entity.id
                    } else {
                        insert(entity)
                    }
                if (entity.isPlayerCharacter) queries.clearOtherPlayerCharacters(id, current.requireId())
                id
            }
        }

    // a record of a known id that is gone comes back under that id, so the "@" tokens in notes find it again
    private fun insert(member: PartyMember): Long {
        if (member.id != 0L) {
            queries.insertWithId(
                id = member.id,
                name = member.name,
                character_class = member.characterClass,
                race = member.race,
                level = member.level?.toLong(),
                player_name = member.playerName,
                is_player_character = member.isPlayerCharacter,
                presence = member.presence,
                hp_max = member.hpMax?.toLong(),
                hp_current = member.hpCurrent?.toLong(),
                description = member.description,
                nicknames = member.nicknames.joinNicknames(),
                portrait = member.portrait?.full,
                portrait_thumb = member.portrait?.thumb,
                campaign_id = current.requireId(),
            )
            return member.id
        }
        queries.insert(
            name = member.name,
            character_class = member.characterClass,
            race = member.race,
            level = member.level?.toLong(),
            player_name = member.playerName,
            is_player_character = member.isPlayerCharacter,
            presence = member.presence,
            hp_max = member.hpMax?.toLong(),
            hp_current = member.hpCurrent?.toLong(),
            description = member.description,
            nicknames = member.nicknames.joinNicknames(),
            portrait = member.portrait?.full,
            portrait_thumb = member.portrait?.thumb,
            campaign_id = current.requireId(),
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    // null removes the picture, see needsWrite for what leaves it alone
    private fun writePortrait(
        memberId: Long,
        portrait: Photo?,
        stored: Photo?,
    ) {
        when {
            portrait == null -> if (stored != null) queries.setPortrait(null, null, memberId)
            portrait.needsWrite(stored) -> queries.setPortrait(portrait.full, portrait.thumb, memberId)
        }
    }

    override suspend fun delete(id: Long) {
        withContext(Dispatchers.Default) {
            queries.deleteById(id)
        }
    }

    // one param per party_member column - dictated by SQLDelight's generated query mapper shape, not real complexity
    @Suppress("LongParameterList")
    private fun toDomain(
        id: Long,
        name: String,
        characterClass: String,
        race: String,
        level: Long?,
        playerName: String,
        isPlayerCharacter: Boolean,
        presence: PartyPresence,
        hpMax: Long?,
        hpCurrent: Long?,
        description: String,
        nicknames: String,
        portraitThumb: ByteArray?,
    ) = PartyMember(
        id = id,
        name = name,
        characterClass = characterClass,
        race = race,
        level = level?.toInt(),
        playerName = playerName,
        isPlayerCharacter = isPlayerCharacter,
        presence = presence,
        hpMax = hpMax?.toInt(),
        hpCurrent = hpCurrent?.toInt(),
        description = description,
        portrait = portraitThumb?.let { Photo(it) },
        nicknames = parseNicknames(nicknames),
    )

    // the by-id row also carries the full jpeg
    @Suppress("LongParameterList")
    private fun toDomainFull(
        id: Long,
        name: String,
        characterClass: String,
        race: String,
        level: Long?,
        playerName: String,
        isPlayerCharacter: Boolean,
        presence: PartyPresence,
        hpMax: Long?,
        hpCurrent: Long?,
        description: String,
        nicknames: String,
        portraitThumb: ByteArray?,
        portrait: ByteArray?,
    ) = toDomain(
        id,
        name,
        characterClass,
        race,
        level,
        playerName,
        isPlayerCharacter,
        presence,
        hpMax,
        hpCurrent,
        description,
        nicknames,
        portraitThumb,
    ).copy(portrait = portraitThumb?.let { Photo(it, portrait) })
}
