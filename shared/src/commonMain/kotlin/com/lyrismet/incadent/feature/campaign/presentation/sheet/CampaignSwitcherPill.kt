package com.lyrismet.incadent.feature.campaign.presentation.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppNoticeBar
import com.lyrismet.incadent.core.designsystem.component.AppTriangle
import com.lyrismet.incadent.core.designsystem.component.appCard
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.campaign_archive_banner_text
import dndplayerscodex.shared.generated.resources.campaign_archive_banner_title
import dndplayerscodex.shared.generated.resources.campaign_resume
import org.jetbrains.compose.resources.stringResource

private val PillShape = AppShapes.Control
private val TriangleSize = 8.dp
private val TriangleHeight = 5.dp

/** the campaign name as a button with the up/down triangles of a switcher, it opens the campaign library */
@Composable
fun CampaignSwitcherPill(
    text: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    muted: Boolean = false,
) {
    val tint = if (muted) AppPalette.Parchment else AppPalette.Gold
    Row(
        modifier =
            modifier
                .height(34.dp)
                .appCard(
                    shape = PillShape,
                    background = tint.copy(alpha = 0.1f),
                    border = tint.copy(alpha = 0.4f),
                    onClick = onClick,
                ).padding(horizontal = 10.dp)
                .semantics { this.contentDescription = contentDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.weight(1f, fill = false),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AppTriangle(true, tint, Modifier.width(TriangleSize).height(TriangleHeight))
            AppTriangle(false, tint, Modifier.width(TriangleSize).height(TriangleHeight))
        }
    }
}

/** the read-only notice under the session list header of a finished campaign, with the way back to a live one */
@Composable
fun AppArchiveBanner(
    onResume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppNoticeBar(AppPalette.Parchment, modifier, fillAlpha = 0.07f, borderAlpha = 0.32f) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.campaign_archive_banner_title),
                color = AppPalette.Parchment,
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = AppFontSize.TinyPlus,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.14.em,
                    ),
            )
            Text(
                text = stringResource(Res.string.campaign_archive_banner_text),
                modifier = Modifier.padding(top = 2.dp),
                color = AppPalette.TextMuted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.SmallPlus, lineHeight = 18.sp),
            )
        }
        AppButton(text = stringResource(Res.string.campaign_resume), onClick = onResume)
    }
}
