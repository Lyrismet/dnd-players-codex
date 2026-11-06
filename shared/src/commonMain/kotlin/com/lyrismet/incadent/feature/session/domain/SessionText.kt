package com.lyrismet.incadent.feature.session.domain

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.displayMentionText
import com.lyrismet.incadent.feature.mention.domain.tokenQuantities
import kotlin.random.Random

/** where a joke of the introduction puts the name of the player's character */
const val INTRO_NAME_SLOT = "{name}"

/** one note of the text, [time] is already written the way the app shows it */
data class SessionTextNote(
    val time: String,
    val body: String,
)

/** one line of the list of who took part, [label] is the kind of record */
data class SessionTextActors(
    val label: String,
    val names: List<String>,
)

/** what the text of a session is put together from, every word already in the person's language */
data class SessionTextParts(
    val heading: String,
    val intro: String?,
    val notes: List<SessionTextNote>,
    val actorsTitle: String,
    val actors: List<SessionTextActors>?,
)

/** the heading, the joke, the notes in order and who took part, an empty part leaves no blank line */
fun buildSessionText(parts: SessionTextParts): String {
    val lines = mutableListOf(parts.heading)
    parts.intro?.takeIf { it.isNotBlank() }?.let { lines += listOf("", it) }
    val notes =
        parts.notes.mapNotNull { note ->
            val text = displayMentionText(note.body).trim()
            if (text.isEmpty()) {
                null
            } else if (note.time.isBlank()) {
                text
            } else {
                "${note.time} - $text"
            }
        }
    if (notes.isNotEmpty()) lines += listOf("") + notes
    val actors = parts.actors.orEmpty().filter { it.names.isNotEmpty() }
    if (actors.isNotEmpty()) {
        lines += listOf("", parts.actorsTitle)
        lines += actors.map { "${it.label}: ${it.names.joinToString(", ")}" }
    }
    return lines.joinToString("\n")
}

/** who the notes mention, one line per kind in the order of [labels], a species met several times reads Goblin ×3 */
fun sessionActors(
    mentioned: List<MentionEntity>,
    bodies: List<String>,
    labels: List<Pair<EntityKind, String>>,
): List<SessionTextActors> =
    labels.map { (kind, label) ->
        val names =
            mentioned.filter { it.ref.type == kind && it !is MentionEntity.DeletedMention }.map { entity ->
                val count =
                    if (kind ==
                        EntityKind.SPECIES
                    ) {
                        bodies.sumOf { tokenQuantities(it, entity.ref).sum() }
                    } else {
                        1
                    }
                if (count > 1) "${entity.name} ×$count" else entity.name
            }
        SessionTextActors(label, names)
    }

/** the joke of [template] with the character's name put in */
fun introText(
    template: String,
    name: String?,
): String = if (name.isNullOrBlank()) template else template.replace(INTRO_NAME_SLOT, name)

/** the joke that comes next, never the one told last time */
fun nextIntroIndex(
    count: Int,
    last: Int,
    random: Random,
): Int =
    when {
        count <= 1 -> 0
        last !in 0 until count -> random.nextInt(count)
        else -> (last + 1 + random.nextInt(count - 1)) % count
    }
