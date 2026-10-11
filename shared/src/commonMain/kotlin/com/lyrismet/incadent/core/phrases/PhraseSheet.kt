package com.lyrismet.incadent.core.phrases

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import com.lyrismet.incadent.domain.model.SessionEntry
import com.lyrismet.incadent.domain.repository.PhraseSnippetRepository
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.phrase_meta_format
import dndplayerscodex.shared.generated.resources.phrase_meta_sessions
import dndplayerscodex.shared.generated.resources.phrase_meta_times
import dndplayerscodex.shared.generated.resources.session_detail_quest_mention_prefix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** the "Заготовки" bottom sheet's content, pre-formatted for rendering - absent while the sheet is closed */
data class PhraseSheetState(
    val pinned: List<PhrasePinnedRow>,
    val frequent: List<PhraseFrequentRow>,
    val draft: String,
)

data class PhrasePinnedRow(
    val text: String,
    val label: String,
)

data class PhraseFrequentRow(
    val text: String,
    val label: String,
    val meta: String,
)

sealed interface PhraseSheetEvent {
    data object Dismissed : PhraseSheetEvent

    data class DraftChanged(
        val value: String,
    ) : PhraseSheetEvent

    data object DraftSubmitted : PhraseSheetEvent

    data class Removed(
        val text: String,
    ) : PhraseSheetEvent

    data class Pinned(
        val text: String,
    ) : PhraseSheetEvent
}

/** open flag, draft field and writes of the "Заготовки" sheet - shared by every presenter that hosts it */
class PhraseSheetController(
    private val repository: PhraseSnippetRepository,
) {
    val isOpen: MutableState<Boolean> = mutableStateOf(false)
    val draft: MutableState<String> = mutableStateOf("")

    fun open() {
        isOpen.value = true
    }

    fun onEvent(
        event: PhraseSheetEvent,
        scope: CoroutineScope,
    ) {
        when (event) {
            PhraseSheetEvent.Dismissed -> isOpen.value = false
            is PhraseSheetEvent.DraftChanged -> draft.value = event.value
            PhraseSheetEvent.DraftSubmitted -> {
                val text = draft.value.trim()
                if (text.isEmpty()) return
                draft.value = ""
                scope.launch { repository.add(text) }
            }
            is PhraseSheetEvent.Removed -> scope.launch { repository.remove(event.text) }
            is PhraseSheetEvent.Pinned -> scope.launch { repository.add(event.text) }
        }
    }
}

/** frequent phrases of every note on the device - searched off the main thread and redone when the notes change */
@Composable
fun rememberFrequentPhrases(entries: List<SessionEntry>): List<FrequentPhrase> {
    val phrases by produceState(emptyList<FrequentPhrase>(), entries) {
        value = withContext(Dispatchers.Default) { findFrequentPhrases(entries) }
    }
    return phrases
}

/** the sheet's rows for the current pinned snippets and not yet pinned frequent phrases, null while it is closed */
@Composable
fun PhraseSheetController.sheetState(
    pinned: List<String>,
    frequent: List<FrequentPhrase>,
): PhraseSheetState? {
    if (!isOpen.value) return null
    val questPrefix = stringResource(Res.string.session_detail_quest_mention_prefix)
    return PhraseSheetState(
        pinned = pinned.map { PhrasePinnedRow(it, phraseLabel(it, questPrefix)) },
        frequent =
            frequent.filter { phrase -> pinned.none { it == phrase.text } }.map { phrase ->
                val times = pluralStringResource(Res.plurals.phrase_meta_times, phrase.entryCount, phrase.entryCount)
                val sessions =
                    pluralStringResource(Res.plurals.phrase_meta_sessions, phrase.sessionCount, phrase.sessionCount)
                PhraseFrequentRow(
                    text = phrase.text,
                    label = phraseLabel(phrase.text, questPrefix),
                    meta = stringResource(Res.string.phrase_meta_format, times, sessions),
                )
            },
        draft = draft.value,
    )
}
