package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.numeral
import com.lyrismet.incadent.core.format.orDash
import com.lyrismet.incadent.core.format.signed
import com.lyrismet.incadent.feature.codex.domain.model.Proficiency
import com.lyrismet.incadent.feature.me.domain.AbilityStrip
import com.lyrismet.incadent.feature.me.domain.SheetRow
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_strip_save
import org.jetbrains.compose.resources.stringResource

/** an ability tile with the lines that use it: the saving throw first, then the skills of the ability */
@Composable
internal fun StripCard(
    strip: AbilityStrip,
    onEvent: (MeEvent) -> Unit,
) {
    val big = LocalDensity.current.fontScale > LARGE_FONT_SCALE
    val tapAbility = { onEvent(MeEvent.Sheet(SheetEvent.AbilityTapped(strip.ability))) }
    if (big) {
        Column(Modifier.fillMaxWidth().appCard().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AbilityBand(strip, tapAbility)
            StripRows(strip, onEvent)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().appCard().padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AbilityTile(strip, tapAbility)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) { StripRows(strip, onEvent) }
        }
    }
}

@Composable
private fun AbilityTile(
    strip: AbilityStrip,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .width(84.dp)
                .heightIn(min = 72.dp)
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.Background,
                    border = AppPalette.Border,
                    onClick = onClick,
                ).padding(horizontal = 2.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Text(
            stringResource(strip.ability.nameRes()),
            style = AppText.caption.copy(fontSize = 10.sp),
            color = AppPalette.TextSecondary,
            maxLines = 1,
        )
        Text(
            strip.bonus.signed(),
            style = AppText.numeral(28.sp),
            color =
                if (strip.bonus ==
                    null
                ) {
                    AppPalette.TextTertiary
                } else {
                    AppPalette.TextHeading
                },
        )
        Text(strip.score.orDash(), style = AppText.caption, color = AppPalette.TextTertiary)
    }
}

// with a big system font the tile becomes the head of the strip: name on the left, bonus big on the right
@Composable
private fun AbilityBand(
    strip: AbilityStrip,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.Background,
                    border = AppPalette.Border,
                    onClick = onClick,
                ).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(strip.ability.nameRes()),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            color = AppPalette.TextPrimary,
        )
        Text(strip.bonus.signed(), style = AppText.numeral(24.sp), color = AppPalette.TextHeading)
        Text(strip.score.orDash(), style = AppText.caption, color = AppPalette.TextTertiary)
    }
}

@Composable
private fun StripRows(
    strip: AbilityStrip,
    onEvent: (MeEvent) -> Unit,
) {
    strip.rows.forEachIndexed { index, row ->
        val skill = row.skill
        val onMark =
            if (skill == null) {
                { onEvent(MeEvent.Sheet(SheetEvent.SaveToggled(strip.ability))) }
            } else {
                { onEvent(MeEvent.Sheet(SheetEvent.SkillMarkTapped(skill))) }
            }
        val onName =
            if (skill == null) {
                { onEvent(MeEvent.Sheet(SheetEvent.AbilityTapped(strip.ability))) }
            } else {
                { onEvent(MeEvent.Sheet(SheetEvent.SkillTapped(skill))) }
            }
        if (index > 0) AppDivider(color = AppPalette.RowDivider)
        SheetLine(
            name = stringResource(skill?.nameRes() ?: Res.string.me_strip_save),
            alt = skill?.altRes()?.let { stringResource(it) },
            row = row,
            onMark = onMark,
            onName = onName,
        )
    }
}

@Composable
private fun SheetLine(
    name: String,
    alt: String?,
    row: SheetRow,
    onMark: () -> Unit,
    onName: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clickable(onClick = onMark), contentAlignment = Alignment.Center) {
            ProficiencyMark(row.proficiency)
        }
        Column(Modifier.weight(1f).clickable(onClick = onName).padding(vertical = 4.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, color = AppPalette.TextPrimary)
            if (alt != null) Text(alt, style = AppText.caption, color = AppPalette.TextTertiary)
        }
        Text(
            row.bonus.signed(),
            modifier = Modifier.clickable(onClick = onName).padding(horizontal = 4.dp),
            style = AppText.numeral(16.sp),
            color = if (row.proficiency == Proficiency.NONE) AppPalette.TextMuted else AppPalette.TextHeading,
            textAlign = TextAlign.End,
        )
    }
}

/** empty ring, filled circle for proficiency and a circle in a ring for expertise */
@Composable
internal fun ProficiencyMark(proficiency: Proficiency) {
    val filled = proficiency != Proficiency.NONE
    Box(
        modifier =
            Modifier
                .size(if (proficiency == Proficiency.EXPERTISE) 18.dp else 14.dp)
                .then(
                    if (proficiency == Proficiency.EXPERTISE) {
                        Modifier.border(1.5.dp, AppPalette.GoldBright, CircleShape).padding(3.dp)
                    } else {
                        Modifier
                    },
                ).clip(CircleShape)
                .background(if (filled) AppPalette.GoldBright else Color.Transparent)
                .then(if (!filled) Modifier.border(1.5.dp, AppPalette.RadioIdle, CircleShape) else Modifier),
    )
}
