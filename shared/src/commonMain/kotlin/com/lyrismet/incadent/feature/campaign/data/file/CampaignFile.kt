package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val FILE_FORMAT = "incadent"
const val FILE_VERSION = 1

/** the first line of a file, what the preview shows is read from here before anything else */
@Serializable
data class FileHeader(
    @SerialName("format") val mark: String,
    val version: Int,
    val name: String = "",
    val withPhotos: Boolean = false,
    val records: Int = 0,
    val sessions: Int = 0,
)

/**
 * one line of a file after the header - ids are the file's own numbers, one range per kind, never database ids,
 * and a record only points at ones written above it
 */
@Serializable
sealed interface FileRecord {
    @Serializable
    @SerialName("species")
    data class Species(
        val id: Long,
        val name: String,
        val creatureType: CreatureType,
        val damage: String = "",
        val notes: String = "",
        val nicknames: String = "",
        val hitPoints: Int? = null,
    ) : FileRecord

    @Serializable
    @SerialName("place")
    data class Place(
        val id: Long,
        val name: String,
        val description: String = "",
        val parentId: Long? = null,
    ) : FileRecord

    @Serializable
    @SerialName("npc")
    data class Npc(
        val id: Long,
        val name: String,
        val status: NpcStatus,
        val description: String = "",
        val race: String = "",
        val lifeState: NpcLifeState = NpcLifeState.ALIVE,
        val nicknames: String = "",
        val speciesId: Long? = null,
        val placeIds: List<Long> = emptyList(),
    ) : FileRecord

    @Serializable
    @SerialName("quest")
    data class Quest(
        val id: Long,
        val title: String,
        val status: QuestStatus,
        val reward: String = "",
        val giverNpcId: Long? = null,
        val placeId: Long? = null,
        val description: String = "",
    ) : FileRecord

    @Serializable
    @SerialName("party")
    data class Party(
        val id: Long,
        val name: String,
        val characterClass: String = "",
        val race: String = "",
        val level: Int? = null,
        val playerName: String = "",
        val isPlayerCharacter: Boolean = false,
        val presence: PartyPresence = PartyPresence.IN,
        val hpMax: Int? = null,
        val hpCurrent: Int? = null,
        val description: String = "",
    ) : FileRecord

    @Serializable
    @SerialName("tag")
    data class Tag(
        val name: String,
    ) : FileRecord

    @Serializable
    @SerialName("phrase")
    data class Phrase(
        val text: String,
    ) : FileRecord

    @Serializable
    @SerialName("session")
    data class Session(
        val id: Long,
        val title: String,
        val date: LocalDateTime,
        val endedAt: LocalDateTime? = null,
        val createdAt: Long = 0,
        val tags: List<String> = emptyList(),
    ) : FileRecord

    @Serializable
    @SerialName("entry")
    data class Entry(
        val sessionId: Long,
        val createdAt: LocalDateTime,
        val body: String,
        val edited: Boolean = false,
        val mentionCheck: Boolean = false,
    ) : FileRecord

    /** the main picture of one record, base64 of the stored jpeg, the thumbnail is made again on import */
    @Serializable
    @SerialName("photo")
    data class Photo(
        val owner: PhotoOwner,
        val ownerId: Long,
        val data: String,
    ) : FileRecord
}

@Serializable
enum class PhotoOwner { SPECIES, PLACE, NPC, PARTY }
