package com.lyrismet.incadent.feature.combat.presentation.prep

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppTitleSubtitle
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.DiamondOutline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_prep_intro_add
import dndplayerscodex.shared.generated.resources.combat_prep_intro_foe
import dndplayerscodex.shared.generated.resources.combat_prep_intro_party
import dndplayerscodex.shared.generated.resources.combat_prep_intro_text
import dndplayerscodex.shared.generated.resources.combat_prep_intro_title
import org.jetbrains.compose.resources.stringResource

/** the preparation before the codex has a party member and an enemy: what is missing and where to add it */
@Composable
internal fun PrepIntroBody(
    intro: PrepIntro,
    onEvent: (CombatPrepEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AppDivider(
                color = AppPalette.BorderHover,
                modifier = Modifier.widthIn(max = 28.dp).size(width = 28.dp, height = 1.dp),
            )
            DiamondOutline(size = 8.dp, color = AppPalette.MaroonBright)
            AppDivider(
                color = AppPalette.BorderHover,
                modifier = Modifier.widthIn(max = 28.dp).size(width = 28.dp, height = 1.dp),
            )
        }
        Text(
            text = stringResource(Res.string.combat_prep_intro_title),
            style = MaterialTheme.typography.titleLarge,
            color = AppPalette.TextHeading,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.combat_prep_intro_text),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = AppFontSize.Medium, lineHeight = 22.sp),
            color = AppPalette.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 290.dp),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .appCard(shape = AppShapes.Card, background = AppPalette.SurfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 2.dp),
        ) {
            IntroStep(1, stringResource(Res.string.combat_prep_intro_party), intro.partySub, intro.partyDone) {
                onEvent(CombatPrepEvent.CreateOpened(CombatSide.PARTY))
            }
            AppDivider(color = AppPalette.BorderSubtle)
            IntroStep(2, stringResource(Res.string.combat_prep_intro_foe), intro.foeSub, intro.foeDone) {
                onEvent(CombatPrepEvent.CreateOpened(CombatSide.ENEMY))
            }
        }
    }
}

@Composable
private fun IntroStep(
    number: Int,
    title: String,
    sub: String,
    done: Boolean,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 64.dp).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ring = if (done) AppPalette.Emerald else AppPalette.RadioIdle
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .then(
                        if (done) {
                            Modifier.appCard(CircleShape, AppPalette.Emerald, AppPalette.Emerald, 1.5.dp)
                        } else {
                            Modifier.border(1.5.dp, ring, CircleShape)
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (done) "✓" else number.toString(),
                color = if (done) AppPalette.Background else AppPalette.TextMuted,
                fontSize = AppFontSize.Body,
                fontWeight = FontWeight.Bold,
            )
        }
        AppTitleSubtitle(title = title, subtitle = sub, modifier = Modifier.weight(1f))
        if (!done) {
            AppButton(
                text = stringResource(Res.string.combat_prep_intro_add),
                onClick = onAdd,
                variant = ButtonVariant.Solid,
                size = ButtonSize.Medium,
                height = 40.dp,
            )
        }
    }
}
