package com.lyrismet.incadent.feature.combat.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppLineHeight
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppToast
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.ConfirmationDialog
import com.lyrismet.incadent.core.designsystem.component.EmptyState
import com.lyrismet.incadent.core.designsystem.component.OverlineStyle
import com.lyrismet.incadent.core.designsystem.component.ScreenHeader
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.floatingBarAnchor
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetEvent
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheets
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSwitcherPill
import com.lyrismet.incadent.feature.combat.presentation.summary.OutcomeBadge
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.campaign_switcher_description
import dndplayerscodex.shared.generated.resources.combat_list_archive_text
import dndplayerscodex.shared.generated.resources.combat_list_archive_title
import dndplayerscodex.shared.generated.resources.combat_list_continue
import dndplayerscodex.shared.generated.resources.combat_list_empty
import dndplayerscodex.shared.generated.resources.combat_list_prep_overline
import dndplayerscodex.shared.generated.resources.combat_list_reset
import dndplayerscodex.shared.generated.resources.combat_list_reset_keep
import dndplayerscodex.shared.generated.resources.combat_list_reset_text
import dndplayerscodex.shared.generated.resources.combat_list_reset_title
import dndplayerscodex.shared.generated.resources.combat_list_start
import dndplayerscodex.shared.generated.resources.combat_list_title
import org.jetbrains.compose.resources.stringResource

private val GroupLetterSpacing = 0.12.em
private val RowDivider = Color(0xFF1F2029)

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
                    CombatJournal(state.groups, state.eventSink, modifier = Modifier.weight(1f))
                }
                CombatFooterBar(state.footer, state.eventSink)
            }
            AppToast(text = state.toast?.text, modifier = floatingBarAnchor())
        }
    }
    CampaignSheets(state.campaignSheet) { event -> state.eventSink(CombatListEvent.CampaignSheet(event)) }
    if (state.resetConfirmVisible) {
        ConfirmationDialog(
            title = stringResource(Res.string.combat_list_reset_title),
            text = stringResource(Res.string.combat_list_reset_text),
            onConfirm = { state.eventSink(CombatListEvent.ResetConfirmed) },
            onDismiss = { state.eventSink(CombatListEvent.ResetDismissed) },
            confirmLabel = stringResource(Res.string.combat_list_reset),
            dismissLabel = stringResource(Res.string.combat_list_reset_keep),
        )
    }
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
        if (state.campaignArchived) ArchiveNote(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
    }
}

@Composable
private fun CombatJournal(
    groups: List<CombatGroupItem>,
    onEvent: (CombatListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 20.dp),
    ) {
        groups.forEach { group ->
            item(key = group.label) {
                SectionOverline(
                    text = group.label,
                    color = AppPalette.TextTertiary,
                    letterSpacing = GroupLetterSpacing,
                    fontSize = AppFontSize.TinyPlus,
                    modifier = Modifier.padding(start = 4.dp, top = 10.dp, end = 4.dp, bottom = 4.dp),
                )
            }
            items(
                group.rows,
                key = { it.id },
            ) { row -> CombatRow(row) { onEvent(CombatListEvent.CombatTapped(row.id)) } }
        }
    }
}

@Composable
private fun CombatRow(
    row: CombatRowItem,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        AppDivider(color = RowDivider)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 60.dp)
                    .clickable(onClick = onClick)
                    .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.foes,
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            fontSize = AppFontSize.MediumPlus,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = AppLineHeight.Row,
                        ),
                    color = AppPalette.TextPrimary,
                )
                Text(
                    text = row.meta,
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontSize = AppFontSize.Small,
                            lineHeight = AppLineHeight.Row,
                        ),
                    color = AppPalette.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutcomeBadge(row.outcome)
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
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontSize = AppFontSize.Small,
                                lineHeight = AppLineHeight.Row,
                            ),
                        color = AppPalette.TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    FooterButton(stringResource(Res.string.combat_list_start)) { onEvent(CombatListEvent.StartClicked) }
                }
                is CombatFooter.Prepared -> PreparedFooter(footer.summary, onEvent)
            }
        }
    }
}

/** the abandoned preparation: what is picked so far, a reset and the way back in */
@Composable
private fun PreparedFooter(
    summary: String,
    onEvent: (CombatListEvent) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(Res.string.combat_list_prep_overline),
            style = AppText.fieldOverline.copy(fontSize = AppFontSize.TinyPlus, fontWeight = FontWeight.Bold),
            color = AppPalette.MaroonBright,
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.SmallPlus),
            color = AppPalette.TextDescription,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(Res.string.combat_list_reset),
            color = AppPalette.TextSecondary,
            fontSize = AppFontSize.SmallPlus,
            fontWeight = FontWeight.SemiBold,
            modifier =
                Modifier
                    .defaultMinSize(minHeight = 28.dp)
                    .clickable { onEvent(CombatListEvent.ResetClicked) }
                    .padding(horizontal = 4.dp),
        )
    }
    FooterButton(stringResource(Res.string.combat_list_continue)) { onEvent(CombatListEvent.ContinueClicked) }
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

@Composable
private fun ArchiveNote(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Card,
                    background = AppPalette.Parchment.copy(alpha = 0.07f),
                    border = AppPalette.Parchment.copy(alpha = 0.32f),
                ).padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        SectionOverline(
            text = stringResource(Res.string.combat_list_archive_title),
            color = AppPalette.Parchment,
            style = OverlineStyle.Field,
        )
        Text(
            text = stringResource(Res.string.combat_list_archive_text),
            color = AppPalette.TextMuted,
            fontSize = AppFontSize.SmallPlus,
            lineHeight = AppLineHeight.Row,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
