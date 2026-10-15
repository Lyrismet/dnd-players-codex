package com.lyrismet.incadent.feature.campaign.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppListCard
import com.lyrismet.incadent.core.designsystem.component.AppListRow
import com.lyrismet.incadent.core.designsystem.component.AppToast
import com.lyrismet.incadent.core.designsystem.component.DIAMOND_ROTATION_DEGREES
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.HeaderDivider
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.floatingBarAnchor
import com.lyrismet.incadent.core.language.GlobeIcon
import com.lyrismet.incadent.core.language.LanguageSheet
import com.lyrismet.incadent.core.language.LanguageSheetEvent
import com.lyrismet.incadent.core.language.label
import com.lyrismet.incadent.core.preferences.AppLanguage
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSections
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheetEvent
import com.lyrismet.incadent.feature.campaign.presentation.sheet.CampaignSheets
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.campaign_library_title
import dndplayerscodex.shared.generated.resources.campaign_new
import dndplayerscodex.shared.generated.resources.campaign_onboarding_codex_text
import dndplayerscodex.shared.generated.resources.campaign_onboarding_codex_title
import dndplayerscodex.shared.generated.resources.campaign_onboarding_combat_text
import dndplayerscodex.shared.generated.resources.campaign_onboarding_combat_title
import dndplayerscodex.shared.generated.resources.campaign_onboarding_lead
import dndplayerscodex.shared.generated.resources.campaign_onboarding_notes_text
import dndplayerscodex.shared.generated.resources.campaign_onboarding_notes_title
import dndplayerscodex.shared.generated.resources.campaign_onboarding_title
import dndplayerscodex.shared.generated.resources.settings_language_title
import dndplayerscodex.shared.generated.resources.splash_app_name
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** renders the first-launch intro or the campaign list, with the create sheet over either */
@Composable
fun CampaignLibraryUi(
    state: CampaignLibraryScreenState,
    modifier: Modifier = Modifier,
) {
    val sink = { event: CampaignSheetEvent -> state.eventSink(CampaignLibraryEvent.CampaignSheet(event)) }
    Scaffold(modifier = modifier) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    if (state.isOnboarding) {
                        OnboardingBody()
                    } else {
                        LibraryBody(state, sink)
                    }
                }
                // only the campaign list has the rule above its button, the first launch is a bare page
                if (!state.isOnboarding) AppDivider()
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 34.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (state.isOnboarding) {
                        OnboardingLanguageRow(state.language) {
                            state.eventSink(CampaignLibraryEvent.LanguageSheet(LanguageSheetEvent.Opened))
                        }
                    }
                    FormPrimaryButton(
                        text = stringResource(Res.string.campaign_new),
                        onClick = { sink(CampaignSheetEvent.NewCampaignClicked) },
                    )
                }
            }
            AppToast(
                text = state.toast,
                modifier = floatingBarAnchor(bottomInset = 96.dp),
            )
        }
    }
    CampaignSheets(state.sheet, sink)
    if (state.languageSheetOpen) {
        LanguageSheet(
            selected = state.language,
            onSelected = { state.eventSink(CampaignLibraryEvent.LanguageSheet(LanguageSheetEvent.Selected(it))) },
            onDismiss = { state.eventSink(CampaignLibraryEvent.LanguageSheet(LanguageSheetEvent.Dismissed)) },
        )
    }
}

@Composable
private fun LibraryBody(
    state: CampaignLibraryScreenState,
    sink: (CampaignSheetEvent) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 14.dp)) {
        SectionOverline(text = stringResource(Res.string.splash_app_name), modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(Res.string.campaign_library_title),
            style = MaterialTheme.typography.displayLarge,
            color = AppPalette.TextHeading,
        )
        HeaderDivider(modifier = Modifier.padding(top = 12.dp))
    }
    CampaignSections(
        library = state.library,
        onOpen = { sink(CampaignSheetEvent.CampaignClicked(it)) },
        modifier = Modifier.padding(horizontal = 16.dp),
        showEmptyLiveHint = true,
    )
}

@Composable
private fun OnboardingBody() {
    Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 6.dp)) {
        // the mockup sets the name 4px above the title, with no 32px header slot around it
        SectionOverline(text = stringResource(Res.string.splash_app_name))
        Text(
            text = stringResource(Res.string.campaign_onboarding_title),
            modifier = Modifier.padding(top = 4.dp),
            style = MaterialTheme.typography.displayLarge,
            color = AppPalette.TextHeading,
        )
        Text(
            text = stringResource(Res.string.campaign_onboarding_lead),
            modifier = Modifier.padding(top = 8.dp),
            color = AppPalette.TextMuted,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = AppFontSize.MediumPlus, lineHeight = 22.sp),
        )
    }
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OnboardingCard(Res.string.campaign_onboarding_notes_title, Res.string.campaign_onboarding_notes_text) {
            Box(
                Modifier
                    .size(width = 12.dp, height = 15.dp)
                    .border(1.6.dp, AppPalette.GoldBright, RoundedCornerShape(2.dp)),
            )
        }
        OnboardingCard(Res.string.campaign_onboarding_codex_title, Res.string.campaign_onboarding_codex_text) {
            Box(
                Modifier
                    .size(10.dp)
                    .rotate(DIAMOND_ROTATION_DEGREES)
                    .border(1.6.dp, AppPalette.GoldBright),
            )
        }
        OnboardingCard(Res.string.campaign_onboarding_combat_title, Res.string.campaign_onboarding_combat_text) {
            Box(Modifier.size(14.dp).border(1.6.dp, AppPalette.GoldBright, CircleShape))
        }
    }
}

@Composable
private fun OnboardingCard(
    title: StringResource,
    text: StringResource,
    icon: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Surface, AppPalette.BorderSubtle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(size = 32.dp, shape = AppShapes.Tile, background = AppPalette.SurfacePopover) { icon() }
        Column {
            Text(
                text = stringResource(title),
                color = AppPalette.TextHeading,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = AppFontSize.MediumPlus),
            )
            Text(
                text = stringResource(text),
                modifier = Modifier.padding(top = 2.dp),
                color = AppPalette.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** the "Язык интерфейса" row above the create button of the first launch */
@Composable
private fun OnboardingLanguageRow(
    language: AppLanguage,
    onClick: () -> Unit,
) {
    AppListCard {
        AppListRow(
            title = stringResource(Res.string.settings_language_title),
            value = language.label(),
            onClick = onClick,
            minHeight = 48.dp,
            leading = { Image(imageVector = GlobeIcon, contentDescription = null, modifier = Modifier.size(16.dp)) },
        )
    }
}
