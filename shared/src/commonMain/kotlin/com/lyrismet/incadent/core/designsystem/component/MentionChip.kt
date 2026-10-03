package com.lyrismet.incadent.core.designsystem.component

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.LocationMentionColor
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.designsystem.toStatusColor
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.mention.MentionEntity
import com.lyrismet.incadent.core.mention.dedupeKey
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.MentionStyle
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.Quest

/** how inline mention chips are drawn - set once at the app root from Settings */
val LocalMentionStyle = compositionLocalOf { MentionStyle.FILLED }

enum class MentionGlyph(
    val symbol: String,
) {
    NPC("●"),
    LOCATION("▲"),
    QUEST("◆"),
}

/** pre-formatted for direct rendering - built once per presenter from a [MentionEntity], never touched by the Ui */
data class MentionChipItem(
    val stableId: String,
    val glyph: MentionGlyph,
    val label: String,
    val color: StatusColor,
    val strikeThrough: Boolean = false,
    val entityRef: EntityRef? = null,
)

fun MentionEntity.toChipItem(): MentionChipItem =
    when (this) {
        is MentionEntity.NpcMention ->
            MentionChipItem(
                stableId = dedupeKey(),
                glyph = MentionGlyph.NPC,
                label = name,
                color = status.toStatusColor(),
                strikeThrough = status == NpcStatus.DEAD,
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
    }

fun Npc.toMentionChip(): MentionChipItem = MentionEntity.NpcMention(id, name, status).toChipItem()

fun Location.toMentionChip(): MentionChipItem = MentionEntity.LocationMention(id, name).toChipItem()

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
    val underlined = LocalMentionStyle.current == MentionStyle.UNDERLINE
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier =
            modifier
                .then(
                    if (underlined) {
                        Modifier.drawUnderline(item.color.foreground)
                    } else {
                        Modifier.clip(shape).background(item.color.background).border(1.dp, item.color.border, shape)
                    },
                ).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = if (underlined) 1.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(glyphGap),
    ) {
        Text(item.glyph.symbol, fontSize = glyphSize, color = item.color.foreground)
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

private fun Modifier.drawUnderline(color: Color): Modifier =
    drawBehind {
        val strokePx = 1.dp.toPx()
        val y = size.height - strokePx / 2
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = strokePx)
    }
