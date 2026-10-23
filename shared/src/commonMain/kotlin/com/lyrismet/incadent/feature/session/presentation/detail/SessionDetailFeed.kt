package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.format.toDisplayTime
import com.lyrismet.incadent.core.format.toShortDayMonthUpper
import com.lyrismet.incadent.core.localization.currentAppLanguage
import com.lyrismet.incadent.feature.combat.presentation.summary.CombatCardItem
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.MentionSegment
import com.lyrismet.incadent.feature.mention.domain.autoMatchedEntities
import com.lyrismet.incadent.feature.mention.domain.dedupeKey
import com.lyrismet.incadent.feature.mention.domain.parseMentions
import com.lyrismet.incadent.feature.mention.presentation.toChipItem
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_meeting_separator_format
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource

/** a note or a fight on the line of time, whichever the feed puts next */
private data class FeedSlot(
    val at: LocalDateTime,
    val note: SessionEntryItem?,
    val card: CombatCardItem?,
)

/** notes and [cards] under a "ВСТРЕЧА N" separator per calendar day, only when the session spans several */
@Composable
internal fun buildFeed(
    entries: List<SessionEntry>,
    cards: List<CombatCardItem>,
    sessionStartDay: LocalDate?,
    noteItems: Map<Long, SessionEntryItem>,
): List<SessionFeedItem> {
    val slots =
        (
            entries.map { FeedSlot(it.createdAt, noteItems[it.id], null) } +
                cards.map { FeedSlot(it.finishedAt, null, it) }
        ).sortedBy { it.at }
    if (slots.isEmpty()) return emptyList()
    val language = currentAppLanguage()
    val distinctDays = (listOfNotNull(sessionStartDay) + slots.map { it.at.date }).distinct().sorted()
    val isMultiDay = distinctDays.size > 1

    val feed = mutableListOf<SessionFeedItem>()
    var lastDay: LocalDate? = null
    for (slot in slots) {
        val day = slot.at.date
        if (isMultiDay && day != lastDay) {
            val meetingNumber = distinctDays.indexOf(day) + 1
            val label =
                stringResource(
                    Res.string.session_detail_meeting_separator_format,
                    meetingNumber,
                    day.toShortDayMonthUpper(language),
                )
            feed += SessionFeedItem.DaySeparator(key = "sep-$day", label = label)
            lastDay = day
        }
        slot.note?.let { feed += SessionFeedItem.Note(it) }
        slot.card?.let { feed += SessionFeedItem.Combat(it) }
    }
    return feed
}

internal fun SessionEntry.toItem(candidates: MentionIndex): SessionEntryItem {
    // only a note that was sent or edited and not decided on yet is searched for words without "@"
    val parsed = parseMentions(body, candidates, autoMatch = mentionCheck)
    return SessionEntryItem(
        id = id,
        timeLabel = createdAt.toDisplayTime(),
        edited = edited,
        segments = parsed.toSegments(),
        autoMatchCount = autoMatchedEntities(parsed).size,
    )
}

// a chip knows which of its record's tokens it is, so a card opened from it can change just that count
internal fun List<MentionSegment>.toSegments(): List<SessionEntrySegment> {
    val seen = mutableMapOf<MentionRef, Int>()
    return map { segment ->
        when (segment) {
            is MentionSegment.Text -> SessionEntrySegment.Text(segment.text)
            is MentionSegment.Mention -> {
                val occurrence = seen[segment.entity.ref] ?: 0
                seen[segment.entity.ref] = occurrence + 1
                SessionEntrySegment.Mention(segment.entity.toChipItem().copy(quantity = segment.quantity), occurrence)
            }
            is MentionSegment.AutoMatch -> SessionEntrySegment.AutoMatch(segment.text, segment.entity.dedupeKey())
        }
    }
}
