package com.lyrismet.incadent.core.help

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lyrismet.incadent.core.designsystem.component.AppHelpHint
import com.lyrismet.incadent.core.designsystem.component.HelpMarker
import com.lyrismet.incadent.core.designsystem.component.HelpRow
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.help_ac_body
import dndplayerscodex.shared.generated.resources.help_ac_title
import dndplayerscodex.shared.generated.resources.help_ds_body
import dndplayerscodex.shared.generated.resources.help_ds_fail_sub
import dndplayerscodex.shared.generated.resources.help_ds_fail_title
import dndplayerscodex.shared.generated.resources.help_ds_ok_sub
import dndplayerscodex.shared.generated.resources.help_ds_ok_title
import dndplayerscodex.shared.generated.resources.help_ds_title
import dndplayerscodex.shared.generated.resources.help_friend_body
import dndplayerscodex.shared.generated.resources.help_friend_row_ac
import dndplayerscodex.shared.generated.resources.help_friend_row_hp
import dndplayerscodex.shared.generated.resources.help_friend_row_items
import dndplayerscodex.shared.generated.resources.help_friend_row_me
import dndplayerscodex.shared.generated.resources.help_friend_row_notes
import dndplayerscodex.shared.generated.resources.help_friend_row_notes_sub
import dndplayerscodex.shared.generated.resources.help_friend_row_sheet
import dndplayerscodex.shared.generated.resources.help_friend_title
import dndplayerscodex.shared.generated.resources.help_per_body
import dndplayerscodex.shared.generated.resources.help_per_title
import dndplayerscodex.shared.generated.resources.help_pip_off_title
import dndplayerscodex.shared.generated.resources.help_pip_on_title
import dndplayerscodex.shared.generated.resources.help_res_body
import dndplayerscodex.shared.generated.resources.help_res_off_sub
import dndplayerscodex.shared.generated.resources.help_res_on_sub
import dndplayerscodex.shared.generated.resources.help_res_title
import dndplayerscodex.shared.generated.resources.help_skills_body
import dndplayerscodex.shared.generated.resources.help_skills_expert_sub
import dndplayerscodex.shared.generated.resources.help_skills_expert_title
import dndplayerscodex.shared.generated.resources.help_skills_none_sub
import dndplayerscodex.shared.generated.resources.help_skills_none_title
import dndplayerscodex.shared.generated.resources.help_skills_proficient_sub
import dndplayerscodex.shared.generated.resources.help_skills_proficient_title
import dndplayerscodex.shared.generated.resources.help_skills_title
import dndplayerscodex.shared.generated.resources.help_slots_body
import dndplayerscodex.shared.generated.resources.help_slots_off_sub
import dndplayerscodex.shared.generated.resources.help_slots_off_title
import dndplayerscodex.shared.generated.resources.help_slots_on_sub
import dndplayerscodex.shared.generated.resources.help_slots_on_title
import dndplayerscodex.shared.generated.resources.help_slots_title
import dndplayerscodex.shared.generated.resources.help_temp_body
import dndplayerscodex.shared.generated.resources.help_temp_title
import dndplayerscodex.shared.generated.resources.help_wallet_body
import dndplayerscodex.shared.generated.resources.help_wallet_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private class HelpRowText(
    val marker: HelpMarker,
    val title: StringResource,
    val subtitle: StringResource? = null,
)

/** the nine places that explain themselves with a "?" - the texts are in `help_*` strings */
enum class HelpTopic(
    internal val title: StringResource,
    internal val body: StringResource,
    private val legend: List<HelpRowText> = emptyList(),
) {
    SKILLS(
        Res.string.help_skills_title,
        Res.string.help_skills_body,
        listOf(
            HelpRowText(HelpMarker.Empty, Res.string.help_skills_none_title, Res.string.help_skills_none_sub),
            HelpRowText(
                HelpMarker.Filled,
                Res.string.help_skills_proficient_title,
                Res.string.help_skills_proficient_sub,
            ),
            HelpRowText(HelpMarker.Double, Res.string.help_skills_expert_title, Res.string.help_skills_expert_sub),
        ),
    ),
    SLOTS(
        Res.string.help_slots_title,
        Res.string.help_slots_body,
        listOf(
            HelpRowText(HelpMarker.Filled, Res.string.help_slots_on_title, Res.string.help_slots_on_sub),
            HelpRowText(HelpMarker.Empty, Res.string.help_slots_off_title, Res.string.help_slots_off_sub),
        ),
    ),
    RESOURCES(
        Res.string.help_res_title,
        Res.string.help_res_body,
        listOf(
            HelpRowText(HelpMarker.Filled, Res.string.help_pip_on_title, Res.string.help_res_on_sub),
            HelpRowText(HelpMarker.Empty, Res.string.help_pip_off_title, Res.string.help_res_off_sub),
        ),
    ),
    DEATH_SAVES(
        Res.string.help_ds_title,
        Res.string.help_ds_body,
        listOf(
            HelpRowText(HelpMarker.Success, Res.string.help_ds_ok_title, Res.string.help_ds_ok_sub),
            HelpRowText(HelpMarker.Danger, Res.string.help_ds_fail_title, Res.string.help_ds_fail_sub),
        ),
    ),
    ARMOR_CLASS(Res.string.help_ac_title, Res.string.help_ac_body),
    PASSIVE_PERCEPTION(Res.string.help_per_title, Res.string.help_per_body),
    WALLET(Res.string.help_wallet_title, Res.string.help_wallet_body),
    TEMP_HIT_POINTS(Res.string.help_temp_title, Res.string.help_temp_body),
    FRIEND_EXPORT(
        Res.string.help_friend_title,
        Res.string.help_friend_body,
        listOf(
            HelpRowText(HelpMarker.Danger, Res.string.help_friend_row_hp),
            HelpRowText(HelpMarker.Danger, Res.string.help_friend_row_ac),
            HelpRowText(HelpMarker.Danger, Res.string.help_friend_row_notes, Res.string.help_friend_row_notes_sub),
            HelpRowText(HelpMarker.Danger, Res.string.help_friend_row_sheet),
            HelpRowText(HelpMarker.Danger, Res.string.help_friend_row_items),
            HelpRowText(HelpMarker.Danger, Res.string.help_friend_row_me),
        ),
    ),
    ;

    @Composable
    internal fun rows(): List<HelpRow> =
        legend.map { HelpRow(it.marker, stringResource(it.title), it.subtitle?.let { sub -> stringResource(sub) }) }
}

/** the "?" of one [topic], placed next to the section title it explains */
@Composable
fun HelpHint(
    topic: HelpTopic,
    modifier: Modifier = Modifier,
) {
    AppHelpHint(
        title = stringResource(topic.title),
        body = stringResource(topic.body),
        modifier = modifier,
        rows = topic.rows(),
    )
}
