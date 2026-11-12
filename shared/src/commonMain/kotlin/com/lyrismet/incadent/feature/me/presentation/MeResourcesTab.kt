package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppCells
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.CellSize
import com.lyrismet.incadent.core.designsystem.component.CellTone
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.me.domain.isDown
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_help_resources_body
import incadent.shared.generated.resources.me_help_resources_title
import incadent.shared.generated.resources.me_resource_uses
import incadent.shared.generated.resources.me_resources_add
import incadent.shared.generated.resources.me_resources_empty_body
import incadent.shared.generated.resources.me_resources_empty_title
import incadent.shared.generated.resources.me_resources_rest_note
import incadent.shared.generated.resources.me_resources_template
import incadent.shared.generated.resources.me_resources_title
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val PIPS_WEIGHT = 1.6f

/** draws the counters of the character as lines with pips, nothing about how they are stored */
@Composable
internal fun MeResourcesTab(
    character: MeCharacter,
    listState: LazyListState,
    onEvent: (MeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val send = { event: CounterEvent -> onEvent(MeEvent.Counters(event)) }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (character.vitals.isDown) item { MeDeathSaves(character, onEvent) }
        if (character.resources.isEmpty()) {
            item { EmptyResources() }
        } else {
            item { ResourcesHeader() }
            item { ResourceLines(character.resources, send) }
        }
        if (!character.readOnly) {
            item { AddButtons(send) }
        }
        item {
            Text(
                stringResource(Res.string.me_resources_rest_note),
                style = AppText.footnote,
                color = AppPalette.TextSecondary,
            )
        }
    }
}

@Composable
private fun ResourcesHeader() {
    MeSectionTitle(
        stringResource(Res.string.me_resources_title),
        helpTitle = stringResource(Res.string.me_help_resources_title),
        helpBody = stringResource(Res.string.me_help_resources_body),
    )
}

@Composable
private fun EmptyResources() {
    MeEmptyState(
        stringResource(Res.string.me_resources_empty_title),
        stringResource(Res.string.me_resources_empty_body),
        Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun ResourceLines(
    resources: List<CharacterResource>,
    send: (CounterEvent) -> Unit,
) {
    Column(Modifier.fillMaxWidth().appCard().padding(start = 14.dp, end = 8.dp)) {
        resources.forEachIndexed { index, resource ->
            if (index > 0) AppDivider()
            ResourceLine(resource, send)
        }
    }
}

@Composable
private fun ResourceLine(
    resource: CharacterResource,
    send: (CounterEvent) -> Unit,
) {
    val hold = { send(CounterEvent.PipHeld(resource.id)) }
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).clickable(onClick = hold).padding(vertical = 8.dp)) {
            Text(resource.name, style = MaterialTheme.typography.bodyLarge, color = AppPalette.TextPrimary)
            Text(
                pluralStringResource(Res.plurals.me_resource_uses, resource.max, resource.current, resource.max),
                style = AppText.caption,
                color = AppPalette.TextSecondary,
            )
        }
        AppCells(
            total = resource.max,
            filled = resource.current,
            size = CellSize.Medium,
            tone = CellTone.Gold,
            // a press spends or returns the same way as on a spell level
            onPress = { send(CounterEvent.CellPressed(resource.id, it)) },
            onHold = hold,
            modifier = Modifier.weight(PIPS_WEIGHT),
        )
    }
}

@Composable
private fun AddButtons(send: (CounterEvent) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppButton(
            text = stringResource(Res.string.me_resources_add),
            onClick = { send(CounterEvent.ResourceAdded) },
            modifier = Modifier.weight(1f),
            tone = ButtonTone.Neutral,
            height = 48.dp,
        )
        AppButton(
            text = stringResource(Res.string.me_resources_template),
            onClick = { send(CounterEvent.TemplatesRequested) },
            modifier = Modifier.weight(1f),
            tone = ButtonTone.Neutral,
            height = 48.dp,
        )
    }
}
