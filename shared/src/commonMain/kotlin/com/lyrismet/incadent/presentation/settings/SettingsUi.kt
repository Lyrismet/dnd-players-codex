package com.lyrismet.incadent.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.campaign.RenameSheetState
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.CampaignRenameSheet
import com.lyrismet.incadent.core.designsystem.component.ScreenHeader
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.SegmentedControl
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.domain.model.AppLanguage
import com.lyrismet.incadent.domain.model.MentionStyle
import com.lyrismet.incadent.domain.model.SessionNumbering
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.settings_campaign_name_title
import dndplayerscodex.shared.generated.resources.settings_hints_desc
import dndplayerscodex.shared.generated.resources.settings_hints_show
import dndplayerscodex.shared.generated.resources.settings_hints_title
import dndplayerscodex.shared.generated.resources.settings_language_desc
import dndplayerscodex.shared.generated.resources.settings_language_en
import dndplayerscodex.shared.generated.resources.settings_language_ru
import dndplayerscodex.shared.generated.resources.settings_language_title
import dndplayerscodex.shared.generated.resources.settings_mention_desc
import dndplayerscodex.shared.generated.resources.settings_mention_filled
import dndplayerscodex.shared.generated.resources.settings_mention_title
import dndplayerscodex.shared.generated.resources.settings_mention_underline
import dndplayerscodex.shared.generated.resources.settings_numbering_arabic
import dndplayerscodex.shared.generated.resources.settings_numbering_desc
import dndplayerscodex.shared.generated.resources.settings_numbering_roman
import dndplayerscodex.shared.generated.resources.settings_numbering_title
import dndplayerscodex.shared.generated.resources.settings_section_app
import dndplayerscodex.shared.generated.resources.settings_section_campaign
import dndplayerscodex.shared.generated.resources.settings_section_diary
import dndplayerscodex.shared.generated.resources.settings_storage_title
import dndplayerscodex.shared.generated.resources.settings_storage_value
import dndplayerscodex.shared.generated.resources.settings_title
import org.jetbrains.compose.resources.stringResource

private val ChoiceLabelSerif = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
internal val ChoiceLabelSans = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

/** renders the settings screen - every tap goes out through the state's eventSink */
@Composable
fun SettingsUi(
    state: SettingsState,
    modifier: Modifier = Modifier,
) {
    val sink = state.eventSink
    Scaffold(modifier = modifier) { contentPadding ->
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
                AppSection(language = state.language, onLanguageSelected = { sink(SettingsEvent.LanguageSelected(it)) })
                DiarySection(
                    numbering = state.numbering,
                    mentionStyle = state.mentionStyle,
                    onNumberingSelected = { sink(SettingsEvent.NumberingSelected(it)) },
                    onMentionStyleSelected = { sink(SettingsEvent.MentionStyleSelected(it)) },
                    onShowHintsClick = { sink(SettingsEvent.HoldHintShowRequested) },
                )
                CodexSection(
                    editMode = state.editMode,
                    onEditModeSelected = { sink(SettingsEvent.EditModeSelected(it)) },
                )
                CampaignSection(name = state.campaignName, onRenameClick = { sink(SettingsEvent.RenameOpened) })
            }
        }
    }

    val renameSheet = state.renameSheet
    if (renameSheet is RenameSheetState.Editing) {
        CampaignRenameSheet(
            draft = renameSheet.draft,
            onDraftChanged = { sink(SettingsEvent.RenameDraftChanged(it)) },
            onSave = { sink(SettingsEvent.RenameSaved) },
            onDismiss = { sink(SettingsEvent.RenameDismissed) },
        )
    }
}

@Composable
private fun AppSection(
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    SettingsGroup(overline = stringResource(Res.string.settings_section_app)) {
        ChoiceRow(
            title = stringResource(Res.string.settings_language_title),
            description = stringResource(Res.string.settings_language_desc),
            choices = AppLanguage.entries.map { SettingsChoice(it, it.label()) },
            selected = language,
            onSelected = onLanguageSelected,
            labelStyle = ChoiceLabelSans,
            width = 170.dp,
        )
    }
}

@Composable
private fun DiarySection(
    numbering: SessionNumbering,
    mentionStyle: MentionStyle,
    onNumberingSelected: (SessionNumbering) -> Unit,
    onMentionStyleSelected: (MentionStyle) -> Unit,
    onShowHintsClick: () -> Unit,
) {
    SettingsGroup(overline = stringResource(Res.string.settings_section_diary), topSpacing = 12.dp) {
        ChoiceRow(
            title = stringResource(Res.string.settings_numbering_title),
            description = stringResource(Res.string.settings_numbering_desc),
            choices =
                listOf(
                    SettingsChoice(SessionNumbering.ROMAN, stringResource(Res.string.settings_numbering_roman)),
                    SettingsChoice(SessionNumbering.ARABIC, stringResource(Res.string.settings_numbering_arabic)),
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
                    SettingsChoice(MentionStyle.FILLED, stringResource(Res.string.settings_mention_filled)),
                    SettingsChoice(MentionStyle.UNDERLINE, stringResource(Res.string.settings_mention_underline)),
                ),
            selected = mentionStyle,
            onSelected = onMentionStyleSelected,
            labelStyle = ChoiceLabelSans,
            width = 150.dp,
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
) {
    SettingsGroup(overline = stringResource(Res.string.settings_section_campaign), topSpacing = 12.dp) {
        CampaignNameRow(name = name, onClick = onRenameClick)
        AppDivider()
        InfoRow(
            title = stringResource(Res.string.settings_storage_title),
            value = stringResource(Res.string.settings_storage_value),
        )
    }
}

@Composable
private fun AppLanguage.label(): String =
    when (this) {
        AppLanguage.RUSSIAN -> stringResource(Res.string.settings_language_ru)
        AppLanguage.ENGLISH -> stringResource(Res.string.settings_language_en)
    }

@Composable
internal fun SettingsGroup(
    overline: String,
    topSpacing: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = topSpacing),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionOverline(
            text = overline,
            modifier = Modifier.padding(horizontal = 4.dp),
            color = AppPalette.TextSecondary,
            letterSpacing = 0.16.em,
            fontSize = 11.sp,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .appCard(background = AppPalette.Surface)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun <T> ChoiceRow(
    title: String,
    description: String,
    choices: List<SettingsChoice<T>>,
    selected: T,
    onSelected: (T) -> Unit,
    labelStyle: TextStyle,
    width: Dp = 140.dp,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = AppPalette.TextPrimary)
            Text(
                text = description,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 12.sp,
                color = AppPalette.TextSecondary,
            )
        }
        SegmentedControl(
            items = choices,
            onSelected = { onSelected(it.value) },
            itemBackground = { choice ->
                if (choice.value ==
                    selected
                ) {
                    AppPalette.SurfaceElevated
                } else {
                    Color.Transparent
                }
            },
            modifier = Modifier.width(width),
            containerShape = RoundedCornerShape(10.dp),
            containerBackground = AppPalette.Background,
            itemShape = RoundedCornerShape(8.dp),
            itemHeight = 30.dp,
        ) { choice ->
            val tint = if (choice.value == selected) AppPalette.GoldBright else AppPalette.TextSecondary
            Text(text = choice.label, style = labelStyle, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CampaignNameRow(
    name: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_campaign_name_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AppPalette.TextPrimary,
        )
        Text(
            text = name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
            color = AppPalette.GoldBright,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AppPalette.SurfacePopover)
                    .border(1.dp, AppPalette.Border, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = AppPalette.TextPrimary,
        )
        Text(text = value, fontSize = 13.sp, color = AppPalette.TextSecondary)
    }
}
