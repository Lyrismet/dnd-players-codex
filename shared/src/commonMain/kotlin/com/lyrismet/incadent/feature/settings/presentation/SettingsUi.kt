package com.lyrismet.incadent.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.attribution.SrdAttribution
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppListCard
import com.lyrismet.incadent.core.designsystem.component.AppListRow
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.AppToast
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.ListSectionHeader
import com.lyrismet.incadent.core.designsystem.component.ScreenHeader
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.floatingBarAnchor
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.core.language.LanguageRow
import com.lyrismet.incadent.core.language.LanguageSheet
import com.lyrismet.incadent.core.language.LanguageSheetEvent
import com.lyrismet.incadent.core.language.label
import com.lyrismet.incadent.core.preferences.AppLanguage
import com.lyrismet.incadent.core.preferences.MentionStyle
import com.lyrismet.incadent.core.preferences.SessionNumbering
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetEvent
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheets
import com.lyrismet.incadent.feature.campaign.presentation.sheet.rememberImportPicker
import com.lyrismet.incadent.feature.phrases.presentation.PhraseSnippetsSheet
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.settings_about_version_title
import dndplayerscodex.shared.generated.resources.settings_all_campaigns_title
import dndplayerscodex.shared.generated.resources.settings_campaign_name_title
import dndplayerscodex.shared.generated.resources.settings_hints_desc
import dndplayerscodex.shared.generated.resources.settings_hints_show
import dndplayerscodex.shared.generated.resources.settings_hints_title
import dndplayerscodex.shared.generated.resources.settings_language_desc
import dndplayerscodex.shared.generated.resources.settings_language_title
import dndplayerscodex.shared.generated.resources.settings_mention_desc
import dndplayerscodex.shared.generated.resources.settings_mention_filled
import dndplayerscodex.shared.generated.resources.settings_mention_title
import dndplayerscodex.shared.generated.resources.settings_mention_underline
import dndplayerscodex.shared.generated.resources.settings_numbering_arabic
import dndplayerscodex.shared.generated.resources.settings_numbering_desc
import dndplayerscodex.shared.generated.resources.settings_numbering_roman
import dndplayerscodex.shared.generated.resources.settings_numbering_title
import dndplayerscodex.shared.generated.resources.settings_phrases_summary_format
import dndplayerscodex.shared.generated.resources.settings_phrases_title
import dndplayerscodex.shared.generated.resources.settings_section_about
import dndplayerscodex.shared.generated.resources.settings_section_app
import dndplayerscodex.shared.generated.resources.settings_section_campaign
import dndplayerscodex.shared.generated.resources.settings_section_diary
import dndplayerscodex.shared.generated.resources.settings_section_general
import dndplayerscodex.shared.generated.resources.settings_storage_title
import dndplayerscodex.shared.generated.resources.settings_storage_value
import dndplayerscodex.shared.generated.resources.settings_title
import org.jetbrains.compose.resources.stringResource

private val ChoiceLabelSerif = TextStyle(fontSize = AppFontSize.Lead, fontWeight = FontWeight.SemiBold)
internal val ChoiceLabelSans = TextStyle(fontSize = AppFontSize.Small, fontWeight = FontWeight.SemiBold)

/** renders the settings screen - every tap goes out through the state's eventSink */
@Composable
fun SettingsUi(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    val sink = state.eventSink
    val pickFile = rememberImportPicker { sink(SettingsEvent.CampaignSheet(it)) }
    Box(modifier = modifier.fillMaxSize()) {
        // the tab bar already sits above the navigation bar, so this scaffold must not reserve it again
        val contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        Scaffold(modifier = Modifier.fillMaxSize(), contentWindowInsets = contentWindowInsets) { contentPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .verticalScroll(rememberScrollState()),
            ) {
                ScreenHeader(
                    overline = stringResource(Res.string.settings_section_app),
                    title = stringResource(Res.string.settings_title),
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppSection(language = state.language, onLanguageClick = {
                        sink(SettingsEvent.LanguageSheet(LanguageSheetEvent.Opened))
                    })
                    DataSection(
                        onExportClick = { sink(SettingsEvent.CampaignSheet(CampaignSheetEvent.ExportCurrentClicked)) },
                        onImportClick = pickFile,
                    )
                    DiarySection(
                        numbering = state.numbering,
                        mentionStyle = state.mentionStyle,
                        onNumberingSelected = { sink(SettingsEvent.NumberingSelected(it)) },
                        onMentionStyleSelected = { sink(SettingsEvent.MentionStyleSelected(it)) },
                        phrasesSummary =
                            stringResource(
                                Res.string.settings_phrases_summary_format,
                                state.pinnedPhraseCount,
                                state.frequentPhraseCount,
                            ),
                        onPhrasesClick = { sink(SettingsEvent.PhrasesOpened) },
                        onShowHintsClick = { sink(SettingsEvent.SwipeHintsShowRequested) },
                    )
                    CodexSection(state.editMode, state.followPartyLevel, sink)
                    CampaignSection(
                        name = state.campaignName,
                        onRenameClick = { sink(SettingsEvent.CampaignSheet(CampaignSheetEvent.RenameClicked)) },
                        onAllCampaignsClick = { sink(SettingsEvent.CampaignSheet(CampaignSheetEvent.LibraryOpened)) },
                    )
                    AboutSection(version = state.version)
                }
            }
        }
        AppToast(text = state.toast, modifier = floatingBarAnchor())
    }

    SettingsSheets(state)
}

@Composable
private fun SettingsSheets(state: SettingsState) {
    val sink = state.eventSink
    state.phraseSheet?.let { sheet ->
        PhraseSnippetsSheet(sheet) { event -> sink(SettingsEvent.PhraseSheet(event)) }
    }
    if (state.languageSheetOpen) {
        LanguageSheet(
            selected = state.language,
            onSelected = { sink(SettingsEvent.LanguageSheet(LanguageSheetEvent.Selected(it))) },
            onDismiss = { sink(SettingsEvent.LanguageSheet(LanguageSheetEvent.Dismissed)) },
        )
    }
    CampaignSheets(state.campaignSheet) { sink(SettingsEvent.CampaignSheet(it)) }
}

@Composable
private fun AppSection(
    language: AppLanguage,
    onLanguageClick: () -> Unit,
) {
    SettingsGroup(overline = stringResource(Res.string.settings_section_general)) {
        LanguageRow(
            title = stringResource(Res.string.settings_language_title),
            description = stringResource(Res.string.settings_language_desc),
            current = language.label(),
            onClick = onLanguageClick,
        )
    }
}

@Composable
private fun DiarySection(
    numbering: SessionNumbering,
    mentionStyle: MentionStyle,
    phrasesSummary: String,
    onNumberingSelected: (SessionNumbering) -> Unit,
    onMentionStyleSelected: (MentionStyle) -> Unit,
    onPhrasesClick: () -> Unit,
    onShowHintsClick: () -> Unit,
) {
    SettingsGroup(overline = stringResource(Res.string.settings_section_diary), topSpacing = 12.dp) {
        ChoiceRow(
            title = stringResource(Res.string.settings_numbering_title),
            description = stringResource(Res.string.settings_numbering_desc),
            choices =
                listOf(
                    SegmentedTab(SessionNumbering.ROMAN, stringResource(Res.string.settings_numbering_roman)),
                    SegmentedTab(SessionNumbering.ARABIC, stringResource(Res.string.settings_numbering_arabic)),
                ),
            selected = numbering,
            onSelected = onNumberingSelected,
            labelStyle = ChoiceLabelSerif,
        )
        AppDivider()
        ChoiceRow(
            title = stringResource(Res.string.settings_mention_title),
            description = stringResource(Res.string.settings_mention_desc),
            choices =
                listOf(
                    SegmentedTab(MentionStyle.FILLED, stringResource(Res.string.settings_mention_filled)),
                    SegmentedTab(MentionStyle.UNDERLINE, stringResource(Res.string.settings_mention_underline)),
                ),
            selected = mentionStyle,
            onSelected = onMentionStyleSelected,
            labelStyle = ChoiceLabelSans,
            width = 150.dp,
        )
        AppDivider()
        PhrasesRow(
            title = stringResource(Res.string.settings_phrases_title),
            summary = phrasesSummary,
            onClick = onPhrasesClick,
        )
        AppDivider()
        HintsRow(
            title = stringResource(Res.string.settings_hints_title),
            description = stringResource(Res.string.settings_hints_desc),
            actionLabel = stringResource(Res.string.settings_hints_show),
            onAction = onShowHintsClick,
        )
    }
}

@Composable
private fun CampaignSection(
    name: String,
    onRenameClick: () -> Unit,
    onAllCampaignsClick: () -> Unit,
) {
    SettingsGroup(overline = stringResource(Res.string.settings_section_campaign), topSpacing = 12.dp) {
        CampaignNameRow(name = name, onClick = onRenameClick)
        AppDivider()
        AllCampaignsRow(onClick = onAllCampaignsClick)
        AppDivider()
        InfoRow(
            title = stringResource(Res.string.settings_storage_title),
            value = stringResource(Res.string.settings_storage_value),
        )
    }
}

@Composable
private fun AboutSection(version: String) {
    SettingsGroup(overline = stringResource(Res.string.settings_section_about), topSpacing = 12.dp) {
        InfoRow(title = stringResource(Res.string.settings_about_version_title), value = version)
        AppDivider()
        SrdAttribution()
    }
}

@Composable
internal fun SettingsGroup(
    overline: String,
    topSpacing: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = topSpacing),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ListSectionHeader(text = overline, fontSize = AppFontSize.Caption)
        AppListCard(content = content)
    }
}

@Composable
internal fun <T> ChoiceRow(
    title: String,
    description: String,
    choices: List<SegmentedTab<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    labelStyle: TextStyle,
    width: Dp = 140.dp,
) {
    AppListRow(title = title, subtitle = description) {
        AppSegmentedTabs(
            tabs = choices,
            selected = selected,
            onSelected = onSelected,
            modifier = Modifier.width(width),
            labelStyle = labelStyle,
            containerShape = AppShapes.Control,
            containerBackground = AppPalette.Background,
            itemShape = AppShapes.Small,
            itemHeight = 30.dp,
        )
    }
}

@Composable
private fun AllCampaignsRow(onClick: () -> Unit) {
    AppListRow(title = stringResource(Res.string.settings_all_campaigns_title), onClick = onClick)
}

@Composable
private fun CampaignNameRow(
    name: String,
    onClick: () -> Unit,
) {
    AppListRow(
        title = stringResource(Res.string.settings_campaign_name_title),
        onClick = onClick,
        chevron = false,
    ) {
        Text(
            text = name,
            modifier = Modifier.widthIn(max = 180.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = AppFontSize.LeadPlus),
            color = AppPalette.GoldBright,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconBadge(
            size = 36.dp,
            shape = AppShapes.Control,
            background = AppPalette.SurfacePopover,
            border = AppPalette.Border,
        ) {
            Icon(
                imageVector = AppIcons.Edit,
                contentDescription = null,
                tint = AppPalette.Gold,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun InfoRow(
    title: String,
    value: String,
) {
    AppListRow(title = title, value = value, valueColor = AppPalette.TextSecondary, valueWeight = FontWeight.Normal)
}
