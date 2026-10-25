package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.plainMentionText
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import com.lyrismet.incadent.feature.session.domain.repository.TagRepository
import com.lyrismet.incadent.feature.session.domain.tags.TagReasonLabels
import com.lyrismet.incadent.feature.session.domain.tags.suggestSessionTags
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_tag_button_label_empty
import dndplayerscodex.shared.generated.resources.session_detail_tag_button_label_has_tags
import dndplayerscodex.shared.generated.resources.session_detail_tag_sheet_overline_format
import dndplayerscodex.shared.generated.resources.session_tag_reason_quest
import dndplayerscodex.shared.generated.resources.session_tag_reason_quests
import dndplayerscodex.shared.generated.resources.session_tag_reason_words
import org.jetbrains.compose.resources.stringResource

/** what the tag button, the tag sheet and the event handler read, derived from the session's notes and tags */
internal class SessionTagModel(
    val assigned: List<String>,
    val suggestions: List<SessionTagSuggestionItem>,
    val buttonLabel: String,
    val sheet: SessionTagSheetState?,
)

@Composable
@Suppress("LongParameterList")
internal fun rememberSessionTagModel(
    tagRepository: TagRepository,
    sessionNoteId: Long,
    entries: List<SessionEntry>,
    entryMentions: List<MentionEntity>,
    numberLabel: String?,
    titleText: String,
    controller: SessionTagsController,
): SessionTagModel {
    val assignedTags by tagRepository.observeTagsFor(sessionNoteId).collectAsState(initial = emptyList())
    val tagCatalog by tagRepository.observeCatalog().collectAsState(initial = emptyList())
    val tagReasons = tagReasonLabels()
    val suggestions =
        remember(entries, entryMentions, assignedTags, tagReasons) {
            val mentionedQuestNames = entryMentions.filterIsInstance<MentionEntity.QuestMention>().map { it.name }
            suggestSessionTags(
                entries.map { plainMentionText(it.body) },
                mentionedQuestNames,
                assignedTags,
                tagReasons,
            ).map { SessionTagSuggestionItem(it.tag, it.reason) }
        }
    val buttonLabel =
        if (assignedTags.isEmpty()) {
            stringResource(Res.string.session_detail_tag_button_label_empty)
        } else {
            stringResource(Res.string.session_detail_tag_button_label_has_tags)
        }
    val sheet =
        if (controller.isOpen.value) {
            SessionTagSheetState(
                overline =
                    numberLabel
                        ?.let { stringResource(Res.string.session_detail_tag_sheet_overline_format, it) }
                        .orEmpty(),
                sessionTitle = titleText,
                suggestions = suggestions,
                catalog =
                    tagCatalog.map { name ->
                        SessionTagCatalogItem(name, assignedTags.any { it.equals(name, ignoreCase = true) })
                    },
                draft = controller.draft.value,
            )
        } else {
            null
        }
    return SessionTagModel(assignedTags, suggestions, buttonLabel, sheet)
}

@Composable
private fun tagReasonLabels() =
    TagReasonLabels(
        quest = stringResource(Res.string.session_tag_reason_quest),
        quests = stringResource(Res.string.session_tag_reason_quests),
        words = stringResource(Res.string.session_tag_reason_words),
    )
