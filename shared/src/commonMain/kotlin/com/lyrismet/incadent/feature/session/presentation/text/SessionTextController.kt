package com.lyrismet.incadent.feature.session.presentation.text

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.feature.session.domain.SessionTextParts
import com.lyrismet.incadent.feature.session.domain.buildSessionText
import com.lyrismet.incadent.feature.session.domain.introText
import com.lyrismet.incadent.feature.session.domain.nextIntroIndex
import kotlin.random.Random

/** the open sheet, [introIndex] is the joke chosen when it opened so it stays while the sheet is open */
data class SessionTextOpen(
    val introIndex: Int,
    val introCount: Int,
    val withIntro: Boolean = true,
    val withActors: Boolean = false,
    val sending: Boolean = false,
)

/** what the text is made of that the controller cannot know, [parts] is only called once the text is needed */
class SessionTextSource(
    val overline: String,
    val character: String?,
    val namedIntros: List<String>,
    val plainIntros: List<String>,
    val parts: () -> SessionTextParts,
) {
    val intros: List<String> get() = if (character == null) plainIntros else namedIntros
}

/** owns the switches of the "send as text" sheet and builds what it previews and sends */
class SessionTextController(
    private val open: MutableState<SessionTextOpen?>,
    private val preferences: AppPreferencesRepository,
    private val random: Random = Random.Default,
) {
    fun open(source: SessionTextSource) {
        open.value =
            SessionTextOpen(
                introIndex = nextIntroIndex(source.intros.size, preferences.lastSessionTextIntro(), random),
                introCount = source.intros.size,
            )
    }

    fun onEvent(event: SessionTextEvent) {
        val current = open.value ?: return
        when (event) {
            SessionTextEvent.IntroToggled -> open.value = current.copy(withIntro = !current.withIntro)
            SessionTextEvent.IntroChanged ->
                open.value = current.copy(introIndex = nextIntroIndex(current.introCount, current.introIndex, random))
            SessionTextEvent.ActorsToggled -> open.value = current.copy(withActors = !current.withActors)
            SessionTextEvent.SendClicked -> {
                if (current.withIntro) preferences.setLastSessionTextIntro(current.introIndex)
                open.value = current.copy(sending = true)
            }
            SessionTextEvent.Shared -> open.value = current.copy(sending = false)
            SessionTextEvent.Dismissed -> open.value = null
        }
    }

    fun state(source: SessionTextSource): SessionTextSheetState? {
        val current = open.value ?: return null
        val parts = source.parts()
        val intro =
            source.intros
                .getOrNull(current.introIndex)
                ?.let { introText(it, source.character) }
                .takeIf { current.withIntro }
        val text = buildSessionText(parts.copy(intro = intro, actors = parts.actors.takeIf { current.withActors }))
        return SessionTextSheetState(
            overline = source.overline,
            withIntro = current.withIntro,
            withActors = current.withActors,
            hasCharacter = source.character != null,
            canChangeIntro = current.withIntro && current.introCount > 1,
            preview = text,
            shareNow = text.takeIf { current.sending },
        )
    }
}
