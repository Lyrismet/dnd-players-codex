package com.lyrismet.incadent.feature.mention.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.preferences.MentionStyle
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.presentation.style.LocationMentionColor
import com.lyrismet.incadent.feature.codex.presentation.style.PartyMemberColor
import com.lyrismet.incadent.feature.codex.presentation.style.toStatusColor
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.mention.domain.DeletedMention
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.dedupeKey

// a "?" tag has no status, so it is plain grey text with a dashed underline instead of a tinted pill
private val DeletedMentionColor =
    StatusColor(foreground = AppPalette.TextMuted, background = Color.Transparent, border = Color.Transparent)

private const val DASH_ON_DP = 3
private const val DASH_OFF_DP = 2

/** how inline mention chips are drawn - set once at the app root from Settings */
val LocalMentionStyle = compositionLocalOf { MentionStyle.FILLED }

enum class MentionGlyph(
    val symbol: String,
) {
    NPC("●"),
    LOCATION("▲"),
    QUEST("◆"),
    PARTY("■"),

    /** the "?" of a mention whose record was deleted from the codex */
    DELETED("?"),
}

/** pre-formatted for direct rendering - built once per presenter from a [MentionEntity], never touched by the Ui */
data class MentionChipItem(
    val stableId: String,
    val glyph: MentionGlyph,
    val label: String,
    val color: StatusColor,
    val strikeThrough: Boolean = false,
    val entityRef: EntityRef? = null,
    // set only on the "?" tag of a deleted record - the snapshot that tag opens the "not in the codex" sheet with
    val deleted: DeletedMention? = null,
)

fun MentionEntity.toChipItem(): MentionChipItem =
    when (this) {
        is MentionEntity.NpcMention ->
            MentionChipItem(
                stableId = dedupeKey(),
                glyph = MentionGlyph.NPC,
                label = name,
                color = status.toStatusColor(),
                strikeThrough = lifeState == NpcLifeState.DEAD,
                entityRef = EntityRef.Npc(id),
            )

        is MentionEntity.LocationMention ->
            MentionChipItem(
                stableId = dedupeKey(),
                glyph = MentionGlyph.LOCATION,
                label = name,
                color = LocationMentionColor,
                entityRef = EntityRef.Location(id),
            )

        is MentionEntity.QuestMention ->
            MentionChipItem(
                stableId = dedupeKey(),
                glyph = MentionGlyph.QUEST,
                label = name,
                color = status.toStatusColor(),
                entityRef = EntityRef.Quest(id),
            )

        is MentionEntity.PartyMention ->
            MentionChipItem(
                stableId = dedupeKey(),
                glyph = MentionGlyph.PARTY,
                label = name,
                color = PartyMemberColor,
                entityRef = EntityRef.Party(id),
            )

        is MentionEntity.DeletedMention ->
            MentionChipItem(
                stableId = dedupeKey(),
                glyph = MentionGlyph.DELETED,
                label = name,
                color = DeletedMentionColor,
                deleted = DeletedMention(id, name, type),
            )
    }

fun Npc.toMentionChip(): MentionChipItem = MentionEntity.NpcMention(id, name, status, lifeState).toChipItem()

fun Location.toMentionChip(): MentionChipItem = MentionEntity.LocationMention(id, name).toChipItem()

fun PartyMember.toMentionChip(): MentionChipItem = MentionEntity.PartyMention(id, name).toChipItem()

fun Quest.toMentionChip(): MentionChipItem = MentionEntity.QuestMention(id, title, status).toChipItem()

/** the colored "●NPC name"/"▲Location"/"◆Quest" mention pill - pass 14sp for [fontSize] inline in a paragraph */
@Composable
fun MentionChip(
    item: MentionChipItem,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp,
    glyphSize: TextUnit = 8.sp,
    glyphGap: Dp = 5.dp,
    lineHeightFactor: Float = 1.6f,
) {
    val isDeleted = item.glyph == MentionGlyph.DELETED
    // a deleted record has no status colors to fill or line, so it always reads as a dashed underline
    val underlined = isDeleted || LocalMentionStyle.current == MentionStyle.UNDERLINE
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier =
            modifier
                .then(
                    if (underlined) {
                        Modifier.drawUnderline(item.color.foreground, dashed = isDeleted)
                    } else {
                        Modifier.clip(shape).background(item.color.background).border(1.dp, item.color.border, shape)
                    },
                ).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = if (underlined) 1.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(glyphGap),
    ) {
        Text(item.glyph.symbol, fontSize = if (isDeleted) fontSize else glyphSize, color = item.color.foreground)
        Text(
            text = item.label,
            fontSize = fontSize,
            lineHeight = fontSize * lineHeightFactor,
            fontWeight = FontWeight.Medium,
            fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
            color = item.color.foreground,
            textDecoration = if (item.strikeThrough) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}

private fun Modifier.drawUnderline(
    color: Color,
    dashed: Boolean = false,
): Modifier =
    drawBehind {
        val strokePx = 1.dp.toPx()
        val y = size.height - strokePx / 2
        val dashes =
            if (dashed) {
                PathEffect.dashPathEffect(
                    floatArrayOf(DASH_ON_DP.dp.toPx(), DASH_OFF_DP.dp.toPx()),
                )
            } else {
                null
            }
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = strokePx, pathEffect = dashes)
    }
