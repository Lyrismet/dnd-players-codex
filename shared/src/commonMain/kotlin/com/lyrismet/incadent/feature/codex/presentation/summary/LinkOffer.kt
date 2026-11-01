package com.lyrismet.incadent.feature.codex.presentation.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.codex.domain.link.LinkField
import com.lyrismet.incadent.feature.codex.domain.link.LinkSuggestion
import com.lyrismet.incadent.feature.codex.domain.link.linkSuggestionsFor
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.repository.LinkSuggestionDismissalRepository
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.link_suggestion_apply
import dndplayerscodex.shared.generated.resources.link_suggestion_dismiss
import dndplayerscodex.shared.generated.resources.link_suggestion_dismissed_subtitle
import dndplayerscodex.shared.generated.resources.link_suggestion_dismissed_title
import dndplayerscodex.shared.generated.resources.link_suggestion_done_giver
import dndplayerscodex.shared.generated.resources.link_suggestion_done_meet_at
import dndplayerscodex.shared.generated.resources.link_suggestion_done_title
import dndplayerscodex.shared.generated.resources.link_suggestion_done_who_here
import dndplayerscodex.shared.generated.resources.link_suggestion_giver
import dndplayerscodex.shared.generated.resources.link_suggestion_meet_at
import dndplayerscodex.shared.generated.resources.link_suggestion_who_here
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** a suggestion with the words the card and the undo bar show for it, resolved where the resources are */
data class LinkOffer(
    val suggestion: LinkSuggestion,
    val question: String,
    val doneTitle: String,
    val doneSubtitle: String,
    val dismissTitle: String,
    val dismissSubtitle: String,
)

/** the offers for the open card, none for a record kind that has no field to fill or in a read-only campaign */
@Composable
fun rememberLinkOffers(
    ref: EntityRef?,
    records: CodexRecords,
    dismissals: LinkSuggestionDismissalRepository,
    enabled: Boolean,
): List<LinkOffer> {
    val owner = ref?.takeIf { enabled && it !is EntityRef.Party && it !is EntityRef.Species }?.toMentionRef()
    // the set is paired with its owner, so another card never starts from the refusals of the last one
    val loaded by produceState(DismissedOf(null, emptySet()), owner) {
        owner?.let { dismissals.observeDismissed(it).collect { refs -> value = DismissedOf(owner, refs) } }
    }
    val doneTitle = stringResource(Res.string.link_suggestion_done_title)
    val dismissTitle = stringResource(Res.string.link_suggestion_dismissed_title)
    if (owner == null || loaded.owner != owner) return emptyList()
    val suggestions = remember(owner, records, loaded) { records.linkSuggestionsFor(owner, loaded.refs) }
    return suggestions.map { suggestion ->
        val texts = suggestion.field.texts()
        val name = suggestion.target.name
        LinkOffer(
            suggestion,
            stringResource(texts.question, name),
            doneTitle,
            stringResource(texts.done, name),
            dismissTitle,
            stringResource(Res.string.link_suggestion_dismissed_subtitle, name),
        )
    }
}

private data class DismissedOf(
    val owner: MentionRef?,
    val refs: Set<MentionRef>,
)

private enum class LinkTexts(
    val question: StringResource,
    val done: StringResource,
) {
    MEET_AT(Res.string.link_suggestion_meet_at, Res.string.link_suggestion_done_meet_at),
    WHO_HERE(Res.string.link_suggestion_who_here, Res.string.link_suggestion_done_who_here),
    GIVER(Res.string.link_suggestion_giver, Res.string.link_suggestion_done_giver),
}

private fun LinkField.texts(): LinkTexts =
    when (this) {
        LinkField.MEET_AT -> LinkTexts.MEET_AT
        LinkField.WHO_HERE -> LinkTexts.WHO_HERE
        LinkField.GIVER -> LinkTexts.GIVER
    }

/** the offers under a card's notes - "Link" fills the field, "No thanks" is remembered and never asked again */
@Composable
internal fun LinkOfferStrip(
    offers: List<LinkOffer>,
    actions: EntitySummarySheetActions,
) {
    val onApply = actions.onLinkApplied
    val onDismiss = actions.onLinkDismissed
    if (onApply == null || onDismiss == null) return
    offers.forEach { offer ->
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .appCard(
                        shape = AppShapes.Field,
                        background = AppPalette.Gold.copy(alpha = 0.08f),
                        border = AppPalette.Gold.copy(alpha = 0.4f),
                    ).padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                offer.question,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.SmallPlus),
                color = AppPalette.TextHeading,
                modifier = Modifier.weight(1f),
            )
            AppButton(
                text = stringResource(Res.string.link_suggestion_dismiss),
                onClick = { onDismiss(offer) },
                tone = ButtonTone.Neutral,
                variant = ButtonVariant.Ghost,
            )
            AppButton(
                text = stringResource(Res.string.link_suggestion_apply),
                onClick = { onApply(offer) },
                variant = ButtonVariant.Solid,
            )
        }
    }
}
