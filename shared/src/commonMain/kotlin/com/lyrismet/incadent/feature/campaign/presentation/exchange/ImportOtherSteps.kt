package com.lyrismet.incadent.feature.campaign.presentation.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ImportError
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.exchange_back
import incadent.shared.generated.resources.exchange_done
import incadent.shared.generated.resources.exchange_error_broken_body
import incadent.shared.generated.resources.exchange_error_broken_title
import incadent.shared.generated.resources.exchange_error_large_body
import incadent.shared.generated.resources.exchange_error_large_title
import incadent.shared.generated.resources.exchange_error_not_incadent_body
import incadent.shared.generated.resources.exchange_error_not_incadent_title
import incadent.shared.generated.resources.exchange_error_other_file
import incadent.shared.generated.resources.exchange_error_read_body
import incadent.shared.generated.resources.exchange_error_read_title
import incadent.shared.generated.resources.exchange_import_done_toast
import incadent.shared.generated.resources.exchange_kind_item
import incadent.shared.generated.resources.exchange_kind_npc
import incadent.shared.generated.resources.exchange_kind_party
import incadent.shared.generated.resources.exchange_kind_place
import incadent.shared.generated.resources.exchange_kind_quest
import incadent.shared.generated.resources.exchange_kind_species
import incadent.shared.generated.resources.exchange_tab_sessions
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun Note(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        color = AppPalette.TextSecondary,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
internal fun FailedStep(
    failure: ImportFailure,
    onOtherFile: () -> Unit,
) {
    val (title, body) = failureTexts(failure)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Maroon.copy(alpha = 0.1f), AppPalette.Maroon.copy(alpha = 0.5f))
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = stringResource(title), color = AppPalette.MaroonBright, style = MaterialTheme.typography.titleSmall)
        Text(
            text = stringResource(body),
            color = AppPalette.TextDescription,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    FormPrimaryButton(text = stringResource(Res.string.exchange_error_other_file), onClick = onOtherFile)
}

private fun failureTexts(failure: ImportFailure): Pair<StringResource, StringResource> =
    when (failure) {
        ImportFailure.Unreadable -> Res.string.exchange_error_read_title to Res.string.exchange_error_read_body
        is ImportFailure.Rejected ->
            when (failure.error) {
                ImportError.NotIncadent ->
                    Res.string.exchange_error_not_incadent_title to Res.string.exchange_error_not_incadent_body
                ImportError.TooLarge -> Res.string.exchange_error_large_title to Res.string.exchange_error_large_body
                ImportError.Malformed -> Res.string.exchange_error_broken_title to Res.string.exchange_error_broken_body
            }
    }

internal fun ExchangeKind.label(): StringResource =
    when (this) {
        ExchangeKind.PLACE -> Res.string.exchange_kind_place
        ExchangeKind.NPC -> Res.string.exchange_kind_npc
        ExchangeKind.QUEST -> Res.string.exchange_kind_quest
        ExchangeKind.SPECIES -> Res.string.exchange_kind_species
        ExchangeKind.ITEM -> Res.string.exchange_kind_item
        ExchangeKind.PARTY -> Res.string.exchange_kind_party
        ExchangeKind.SESSION -> Res.string.exchange_tab_sessions
    }

@Composable
internal fun DoneStep(
    records: Int,
    onDone: () -> Unit,
) {
    Text(
        text = pluralStringResource(Res.plurals.exchange_import_done_toast, records, records),
        color = AppPalette.EmeraldBright,
        style = MaterialTheme.typography.titleMedium,
    )
    Row(modifier = Modifier.fillMaxWidth()) {
        AppButton(
            text = stringResource(Res.string.exchange_done),
            onClick = onDone,
            modifier = Modifier.weight(1f),
            tone = ButtonTone.Neutral,
            variant = ButtonVariant.Tinted,
            size = ButtonSize.Large,
        )
    }
}

@Composable
internal fun BackLink(onClick: () -> Unit) {
    Text(
        text = stringResource(Res.string.exchange_back),
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(AppShapes.Control, Color.Transparent, null, onClick = onClick)
                .padding(vertical = 10.dp),
        color = AppPalette.TextMuted,
        style = MaterialTheme.typography.bodyMedium,
    )
}
