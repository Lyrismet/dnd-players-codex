package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppDisclosure
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_more
import dndplayerscodex.shared.generated.resources.codex_entry_more_close
import dndplayerscodex.shared.generated.resources.codex_entry_more_hint_npc
import dndplayerscodex.shared.generated.resources.codex_entry_summary_portrait
import org.jetbrains.compose.resources.stringResource

/** the "more" block of a form, a plain flag is the only state it keeps */
@Composable
internal fun FormDetails(
    summary: String,
    startsOpen: Boolean,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(startsOpen) }
    AppDisclosure(
        title = stringResource(if (expanded) Res.string.codex_entry_more_close else Res.string.codex_entry_more),
        summary = summary,
        expanded = expanded,
        onToggle = { expanded = !expanded },
        content = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { content() } },
    )
}

// what is filled in while the details are closed, the hint of what hides there when nothing is
@Composable
internal fun npcDetailsSummary(form: CodexEntryFormState): String {
    val filled =
        listOfNotNull(
            stringResource(Res.string.codex_entry_summary_portrait).takeIf { form.portrait != null },
            form.race.trim().takeIf { it.isNotEmpty() },
            form.npcLifeOptions
                .firstOrNull { it.selected && it.value != NpcLifeState.ALIVE }
                ?.label
                ?.lowercase(),
            form.npcPlaces.takeIf { it.isNotEmpty() }?.joinToString(prefix = "▲ ") { it.label },
        )
    return filled.joinToString(" · ").ifEmpty { stringResource(Res.string.codex_entry_more_hint_npc) }
}
