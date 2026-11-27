package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.feature.codex.domain.grouping.kept
import com.lyrismet.incadent.feature.codex.domain.grouping.summary
import com.lyrismet.incadent.feature.codex.domain.model.PartyClass

// the class fields follow the list, the first class and the level sum
internal fun CharacterDraft.withClassList(list: List<PartyClass>): CharacterDraft {
    val summary = list.summary()
    return copy(
        classes = list.kept(),
        characterClass = summary?.characterClass.orEmpty(),
        subclass = summary?.subclass.orEmpty(),
        level = summary?.level ?: level,
    )
}

internal fun FillDraft.withClassList(list: List<PartyClass>): FillDraft {
    val summary = list.summary()
    return copy(
        classes = list.kept(),
        characterClass = summary?.characterClass.orEmpty(),
        subclass = summary?.subclass.orEmpty(),
        level = summary?.level ?: level,
    )
}
