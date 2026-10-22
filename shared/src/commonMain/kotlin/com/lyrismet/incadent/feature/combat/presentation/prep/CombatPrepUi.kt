package com.lyrismet.incadent.feature.combat.presentation.prep

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppStepper
import com.lyrismet.incadent.core.designsystem.component.AppToast
import com.lyrismet.incadent.core.designsystem.component.DiscardChangesDialog
import com.lyrismet.incadent.core.designsystem.component.Dot
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.OverlineStyle
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.dashedBorder
import com.lyrismet.incadent.core.designsystem.component.floatingBarAnchor
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.combat_prep_add_ally
import dndplayerscodex.shared.generated.resources.combat_prep_add_enemy
import dndplayerscodex.shared.generated.resources.combat_prep_add_party
import dndplayerscodex.shared.generated.resources.combat_prep_enemies_header
import dndplayerscodex.shared.generated.resources.combat_prep_exit_text
import dndplayerscodex.shared.generated.resources.combat_prep_less
import dndplayerscodex.shared.generated.resources.combat_prep_more
import dndplayerscodex.shared.generated.resources.combat_prep_overline
import dndplayerscodex.shared.generated.resources.combat_prep_party_count
import dndplayerscodex.shared.generated.resources.combat_prep_party_header
import dndplayerscodex.shared.generated.resources.combat_prep_title
import org.jetbrains.compose.resources.stringResource

private const val UNCHECKED_ALPHA = 0.55f
private val TitleLineHeight = 20.sp
private val PartyRowBorder = Color(0x736EA0E6)
private val AllyDot = AppPalette.Emerald
private val EnemyDot = Color(0xFFC0443F)

@Composable
fun CombatPrepUi(
    state: CombatPrepState,
    modifier: Modifier = Modifier,
) {
    val contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    Scaffold(modifier = modifier, contentWindowInsets = contentWindowInsets) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                PrepHeader(state)
                val intro = state.intro
                if (intro != null) {
                    PrepIntroBody(intro, state.eventSink, Modifier.weight(1f))
                } else {
                    PrepBody(state, Modifier.weight(1f))
                    state.start?.let { PrepStartBar(it) { state.eventSink(CombatPrepEvent.StartClicked) } }
                }
            }
            AppToast(text = state.toast?.text, modifier = floatingBarAnchor())
        }
    }
    state.search?.let { FoeSearchSheet(it, state.eventSink) }
    state.quickCreate?.let { QuickCreateSheet(it, state.eventSink) }
    state.hpPad?.let { HpPadSheet(it, state.eventSink) }
    if (state.exitConfirmVisible) {
        DiscardChangesDialog(
            text = stringResource(Res.string.combat_prep_exit_text),
            onStay = { state.eventSink(CombatPrepEvent.ExitDismissed) },
            onLeave = { state.eventSink(CombatPrepEvent.ExitConfirmed) },
        )
    }
}

@Composable
private fun PrepHeader(state: CombatPrepState) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionOverline(text = stringResource(Res.string.combat_prep_overline), color = AppPalette.MaroonBright)
            IconBadge(
                size = 44.dp,
                background = Color.Transparent,
                onClick = { state.eventSink(CombatPrepEvent.CloseClicked) },
                modifier = Modifier.offset(x = 10.dp),
            ) {
                Text("✕", color = AppPalette.TextMuted, fontSize = AppFontSize.Body)
            }
        }
        Text(
            text = stringResource(Res.string.combat_prep_title),
            style = MaterialTheme.typography.displayLarge,
            color = AppPalette.TextHeading,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (state.intro == null) {
            Text(
                text =
                    buildAnnotatedString {
                        append(state.targetPrefix + " ")
                        withStyle(SpanStyle(color = AppPalette.GoldBright)) { append(state.target) }
                    },
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        fontSize = AppFontSize.Body,
                        lineHeight = TitleLineHeight,
                    ),
                color = AppPalette.TextSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun PrepBody(
    state: CombatPrepState,
    modifier: Modifier = Modifier,
) {
    val send = state.eventSink
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionLabel(
            text = stringResource(Res.string.combat_prep_party_header),
            count = stringResource(Res.string.combat_prep_party_count, state.partyIncluded, state.party.size),
            countColor = AppPalette.Azure,
        )
        state.party.forEach { row ->
            PartyRow(row, onToggle = { send(CombatPrepEvent.PartyToggled(row.partyMemberId)) }) {
                send(CombatPrepEvent.HpPadOpened(row.partyMemberId))
            }
        }
        state.allies.forEach { row -> MemberRow(row, AllyDot, PrepSide.ALLY, send) }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AddButton(
                text = stringResource(Res.string.combat_prep_add_party),
                color = AppPalette.GoldBright,
                modifier = Modifier.weight(1f),
            ) { send(CombatPrepEvent.QuickOpened(QuickKind.PARTY)) }
            AddButton(
                text = stringResource(Res.string.combat_prep_add_ally),
                color = AppPalette.EmeraldBright,
                modifier = Modifier.weight(1f),
            ) { send(CombatPrepEvent.SearchOpened(PrepSide.ALLY)) }
        }
        SectionLabel(
            text = stringResource(Res.string.combat_prep_enemies_header),
            count = state.enemies.sumOf { it.count }.toString(),
            countColor = AppPalette.MaroonBright,
            modifier = Modifier.padding(top = 12.dp),
        )
        state.enemies.forEach { row -> MemberRow(row, EnemyDot, PrepSide.ENEMY, send) }
        AddEnemyButton(
            stringResource(Res.string.combat_prep_add_enemy),
        ) { send(CombatPrepEvent.SearchOpened(PrepSide.ENEMY)) }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    count: String,
    countColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SectionOverline(text = text, color = AppPalette.TextTertiary, style = OverlineStyle.Field)
        Text(text = count, color = countColor, fontSize = AppFontSize.Small)
    }
}

@Composable
private fun PartyRow(
    row: PrepPartyRow,
    onToggle: () -> Unit,
    onHp: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .alpha(if (row.included) 1f else UNCHECKED_ALPHA)
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.SurfaceVariant,
                    border = if (row.included) PartyRowBorder else AppPalette.BorderSubtle,
                    onClick = onToggle,
                ).defaultMinSize(minHeight = 60.dp)
                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(row.included)
        RowTitle(row.name, row.sub, Modifier.weight(1f))
        row.hp?.let { HpButton(it, onHp) }
    }
}

@Composable
private fun Checkbox(checked: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier =
            Modifier
                .size(22.dp)
                .then(
                    if (checked) {
                        Modifier.appCard(shape, AppPalette.Azure, AppPalette.Azure, 1.5.dp)
                    } else {
                        Modifier.border(1.5.dp, AppPalette.RadioIdle, shape)
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Text("✓", color = AppPalette.Background, fontSize = AppFontSize.Body, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HpButton(
    hp: PrepHp,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .defaultMinSize(minWidth = 78.dp, minHeight = 46.dp)
                .appCard(
                    shape = AppShapes.Control,
                    background = AppPalette.Background,
                    border = AppPalette.Border,
                    onClick = onClick,
                ).padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(hp.value, color = AppPalette.TextPrimary, fontSize = AppFontSize.Medium, fontWeight = FontWeight.Bold)
        Text(hp.caption, color = AppPalette.Azure, fontSize = AppFontSize.Tiny, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MemberRow(
    row: PrepMemberRow,
    dot: Color,
    side: PrepSide,
    onEvent: (CombatPrepEvent) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.SurfaceVariant,
                    border = dot.copy(alpha = 0.25f),
                ).defaultMinSize(minHeight = 60.dp)
                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { Dot(size = 8.dp, color = dot) }
        RowTitle(row.name, row.sub, Modifier.weight(1f))
        AppStepper(
            count = row.count,
            onDecrease = { onEvent(CombatPrepEvent.MemberDecreased(side, row.key)) },
            onIncrease = { onEvent(CombatPrepEvent.MemberIncreased(side, row.key)) },
            decreaseDescription = stringResource(Res.string.combat_prep_less),
            increaseDescription = stringResource(Res.string.combat_prep_more),
            canIncrease = row.canIncrease,
        )
    }
}

@Composable
internal fun RowTitle(
    name: String,
    sub: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = name,
            color = AppPalette.TextPrimary,
            fontSize = AppFontSize.MediumPlus,
            lineHeight = TitleLineHeight,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (sub.isNotBlank()) {
            Text(
                text = sub,
                color = AppPalette.TextSecondary,
                fontSize = AppFontSize.Small,
                lineHeight = TitleLineHeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun AddButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .defaultMinSize(minHeight = 48.dp)
                .dashedBorder(AppPalette.BorderHover, cornerRadius = 12.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("+", color = color, fontSize = AppFontSize.Heading)
        Text(text, color = color, fontSize = AppFontSize.BodyPlus, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AddEnemyButton(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .dashedBorder(AppPalette.BorderHover, cornerRadius = 12.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(24.dp).appCard(CircleShape, AppPalette.Gold.copy(alpha = 0.14f), null),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = AppPalette.GoldBright, fontSize = AppFontSize.Heading)
        }
        Text(text, color = AppPalette.GoldBright, fontSize = AppFontSize.Medium, fontWeight = FontWeight.SemiBold)
    }
}
