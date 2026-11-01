package com.lyrismet.incadent.feature.codex.presentation.party

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.component.AppSelectSheet
import com.lyrismet.incadent.core.designsystem.component.AppSelectTexts
import com.lyrismet.incadent.core.designsystem.component.icons.entity.icon
import com.lyrismet.incadent.core.selectlist.SelectOption
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.campaign_player_level_format
import dndplayerscodex.shared.generated.resources.me_pick_empty
import dndplayerscodex.shared.generated.resources.me_pick_search
import dndplayerscodex.shared.generated.resources.me_pick_title
import org.jetbrains.compose.resources.stringResource

/** the characters to pick the player's own from, the fallen are not offered */
data class PlayerCharacterPickState(
    val options: List<SelectOption>,
)

/** the picker's rows, the class and the level under each name */
@Composable
fun playerCharacterPickState(party: List<PartyMember>): PlayerCharacterPickState {
    val levelFormat = Res.string.campaign_player_level_format
    return PlayerCharacterPickState(
        party.filter { !it.fallen }.map { member ->
            val level = member.level?.let { stringResource(levelFormat, it) }
            SelectOption(
                id = member.id,
                title = member.name,
                subtitle =
                    listOfNotNull(
                        member.characterClass.takeIf { it.isNotBlank() },
                        level,
                    ).joinToString(" · ").ifBlank { null },
            )
        },
    )
}

/** the single-pick sheet of the player's own character */
@Composable
fun PlayerCharacterPickSheet(
    state: PlayerCharacterPickState,
    onPicked: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AppSelectSheet(
        texts =
            AppSelectTexts(
                title = stringResource(Res.string.me_pick_title),
                searchPlaceholder = stringResource(Res.string.me_pick_search),
                emptyText = stringResource(Res.string.me_pick_empty),
                multiHint = "",
                clearLabel = null,
                doneLabel = { "" },
                createLabel = null,
            ),
        options = state.options,
        selectedIds = emptySet(),
        multi = false,
        onConfirm = { ids -> ids.firstOrNull()?.let(onPicked) },
        onDismiss = onDismiss,
        leadingIcon = EntityKind.PARTY.icon(),
    )
}
