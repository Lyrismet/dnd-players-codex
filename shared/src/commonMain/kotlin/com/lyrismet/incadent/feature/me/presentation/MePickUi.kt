package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.icons.entity.AppEntityIcon
import com.lyrismet.incadent.core.designsystem.component.icons.entity.EntityIcons
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.campaign_player_level_format
import incadent.shared.generated.resources.me_pick_screen_text
import incadent.shared.generated.resources.me_pick_title
import org.jetbrains.compose.resources.stringResource

/** the screen while nobody carries the "me" mark: the party to choose from */
@Composable
internal fun MePickUi(
    content: MeContent.Pick,
    onEvent: (MeEvent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState(),
                ).padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(width = 72.dp, height = 84.dp), contentAlignment = Alignment.Center) {
            AppEntityIcon(EntityIcons.Me, AppPalette.GoldDim, size = 48.dp)
        }
        Text(
            stringResource(Res.string.me_pick_title),
            style = MaterialTheme.typography.headlineMedium,
            color = AppPalette.TextHeading,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.me_pick_screen_text),
            style = MaterialTheme.typography.bodyMedium,
            color = AppPalette.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content.options.forEach { option -> PickRow(option) { onEvent(MeEvent.Picked(option.id)) } }
        }
    }
}

@Composable
private fun PickRow(
    option: MePickOption,
    onClick: () -> Unit,
) {
    val level = option.level?.let { stringResource(Res.string.campaign_player_level_format, it) }
    val sub = listOfNotNull(option.characterClass.takeIf { it.isNotBlank() }, level).joinToString(" · ")
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .appCard(onClick = onClick)
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            option.name,
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = AppPalette.TextPrimary,
        )
        if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextSecondary)
    }
}
