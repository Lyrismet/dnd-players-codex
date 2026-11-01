package com.lyrismet.incadent.feature.codex.presentation.party

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppNoticeBar
import com.lyrismet.incadent.core.designsystem.component.AppRemoveButton
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.icons.entity.EntityIcons
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_pick_banner_action
import dndplayerscodex.shared.generated.resources.me_pick_banner_text
import dndplayerscodex.shared.generated.resources.me_pick_banner_title
import org.jetbrains.compose.resources.stringResource

private val BannerBlue = AppPalette.TextSecondary

/** the notice over the party that asks the player to pick their own character, it can be hidden with the cross */
@Composable
fun PlayerCharacterBanner(
    onPick: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppNoticeBar(BannerBlue, modifier, endPadding = 6.dp) {
        Icon(EntityIcons.Me, contentDescription = null, tint = BannerBlue, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.me_pick_banner_title),
                color = AppPalette.TextHeading,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = AppFontSize.Medium),
            )
            Text(
                text = stringResource(Res.string.me_pick_banner_text),
                color = AppPalette.TextSecondary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Small),
            )
        }
        AppButton(
            text = stringResource(Res.string.me_pick_banner_action),
            onClick = onPick,
            variant = ButtonVariant.Solid,
        )
        AppRemoveButton(onClick = onHide)
    }
}
