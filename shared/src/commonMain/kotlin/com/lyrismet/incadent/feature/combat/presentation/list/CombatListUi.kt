package com.lyrismet.incadent.feature.combat.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppToast
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.EmptyState
import com.lyrismet.incadent.core.designsystem.component.ListSectionHeader
import com.lyrismet.incadent.core.designsystem.component.ScreenHeader
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.TagChip
import com.lyrismet.incadent.core.designsystem.component.floatingBarAnchor
import com.lyrismet.incadent.core.designsystem.statusColor
import com.lyrismet.incadent.feature.campaign.presentation.sheet.AppArchiveBanner
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetEvent
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheets
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSwitcherPill
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.campaign_switcher_description
import dndplayerscodex.shared.generated.resources.combat_list_continue
import dndplayerscodex.shared.generated.resources.combat_list_empty
import dndplayerscodex.shared.generated.resources.combat_list_prep_overline
import dndplayerscodex.shared.generated.resources.combat_list_reset
import dndplayerscodex.shared.generated.resources.combat_list_start
import dndplayerscodex.shared.generated.resources.combat_list_title
import dndplayerscodex.shared.generated.resources.combat_outcome_interrupted
import dndplayerscodex.shared.generated.resources.combat_outcome_peace
import dndplayerscodex.shared.generated.resources.combat_outcome_retreat
import dndplayerscodex.shared.generated.resources.combat_outcome_victory
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CombatListUi(
    state: CombatListState,
    modifier: Modifier = Modifier,
) {
    // the tab bar already sits above the navigation bar, so the content must not add its inset again
    val contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    Scaffold(modifier = modifier, contentWindowInsets = contentWindowInsets) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                CombatListHeader(state)
                if (state.groups.isEmpty()) {
                    EmptyState(text = stringResource(Res.string.combat_list_empty), modifier = Modifier.weight(1f))
                } else {
                    CombatJournal(state.groups, modifier = Modifier.weight(1f))
                }
                CombatFooterBar(state.footer, state.eventSink)
            }
            AppToast(text = state.toast?.text, modifier = floatingBarAnchor())
        }
    }
    CampaignSheets(state.campaignSheet) { event -> state.eventSink(CombatListEvent.CampaignSheet(event)) }
}

@Composable
private fun CombatListHeader(state: CombatListState) {
    Column {
        ScreenHeader(
            overline = state.campaignName,
            title = stringResource(Res.string.combat_list_title),
            overlineContent = {
                CampaignSwitcherPill(
                    text = state.campaignName,
                    contentDescription = stringResource(Res.string.campaign_switcher_description),
                    onClick = { state.eventSink(CombatListEvent.CampaignSheet(CampaignSheetEvent.LibraryOpened)) },
                    muted = state.campaignArchived,
                )
            },
        )
        val campaignId = state.campaignId
        if (state.campaignArchived && campaignId != null) {
            AppArchiveBanner(
                onResume = { state.eventSink(CombatListEvent.CampaignSheet(CampaignSheetEvent.Resumed(campaignId))) },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun CombatJournal(
    groups: List<CombatGroupItem>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 20.dp),
    ) {
        groups.forEach { group ->
            item(key = group.label) {
                ListSectionHeader(text = group.label, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            }
            items(group.rows, key = { it.id }) { row -> CombatRow(row) }
        }
    }
}

@Composable
private fun CombatRow(row: CombatRowItem) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        AppDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.foes,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = AppFontSize.Medium),
                    color = AppPalette.TextPrimary,
                )
                Text(
                    text = row.meta,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Small),
                    color = AppPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            val color = row.outcome.toStatusColor()
            TagChip(
                text = stringResource(row.outcome.label()),
                foreground = color.foreground,
                background = color.background,
                border = color.border,
            )
        }
    }
}

@Composable
private fun CombatFooterBar(
    footer: CombatFooter,
    onEvent: (CombatListEvent) -> Unit,
) {
    if (footer == CombatFooter.Hidden) return
    Column(modifier = Modifier.fillMaxWidth().background(AppPalette.SurfaceFooter)) {
        AppDivider(color = AppPalette.BorderSubtle)
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (footer) {
                CombatFooter.Hidden -> Unit
                is CombatFooter.Start -> {
                    Text(
                        text = footer.target,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Small),
                        color = AppPalette.TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    FooterButton(stringResource(Res.string.combat_list_start)) { onEvent(CombatListEvent.StartClicked) }
                }
                is CombatFooter.Prepared -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionOverline(
                            text = stringResource(Res.string.combat_list_prep_overline),
                            color = AppPalette.MaroonBright,
                        )
                        Text(
                            text = footer.summary,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.SmallPlus),
                            color = AppPalette.TextDescription,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        AppButton(
                            text = stringResource(Res.string.combat_list_reset),
                            onClick = { onEvent(CombatListEvent.ResetClicked) },
                            tone = ButtonTone.Neutral,
                            variant = ButtonVariant.Ghost,
                        )
                    }
                    FooterButton(
                        stringResource(Res.string.combat_list_continue),
                    ) { onEvent(CombatListEvent.ContinueClicked) }
                }
            }
        }
    }
}

@Composable
private fun FooterButton(
    text: String,
    onClick: () -> Unit,
) {
    AppButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        variant = ButtonVariant.Solid,
        size = ButtonSize.Primary,
    )
}

private fun CombatOutcome.label(): StringResource =
    when (this) {
        CombatOutcome.VICTORY -> Res.string.combat_outcome_victory
        CombatOutcome.RETREAT -> Res.string.combat_outcome_retreat
        CombatOutcome.PEACE -> Res.string.combat_outcome_peace
        CombatOutcome.INTERRUPTED -> Res.string.combat_outcome_interrupted
    }

private fun CombatOutcome.toStatusColor(): StatusColor =
    when (this) {
        CombatOutcome.VICTORY ->
            statusColor(
                AppPalette.EmeraldBright,
                AppPalette.Emerald.copy(alpha = 0.14f),
                AppPalette.Emerald.copy(alpha = 0.42f),
            )
        CombatOutcome.RETREAT ->
            statusColor(AppPalette.GoldBright, AppPalette.Gold.copy(alpha = 0.13f), AppPalette.Gold.copy(alpha = 0.42f))
        CombatOutcome.PEACE ->
            statusColor(AppPalette.Azure, AppPalette.Azure.copy(alpha = 0.12f), AppPalette.Azure.copy(alpha = 0.4f))
        CombatOutcome.INTERRUPTED ->
            statusColor(
                AppPalette.TextMuted,
                AppPalette.SurfaceElevated,
                AppPalette.BorderHover,
            )
    }
