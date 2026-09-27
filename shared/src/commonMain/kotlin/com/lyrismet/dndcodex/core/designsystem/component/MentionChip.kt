package com.lyrismet.dndcodex.core.designsystem.component

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.dndcodex.core.designsystem.LocationMentionColor
import com.lyrismet.dndcodex.core.designsystem.StatusColor
import com.lyrismet.dndcodex.core.designsystem.toStatusColor
import com.lyrismet.dndcodex.core.mention.MentionEntity
import com.lyrismet.dndcodex.core.mention.dedupeKey
import com.lyrismet.dndcodex.domain.model.NpcStatus

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

/** the colored "●NPC name"/"▲Location"/"◆Quest" mention pill - pass 14sp for [fontSize] inline in a paragraph */
@Composable
fun MentionChip(
    item: MentionChipItem,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp,
) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier =
            modifier
                .clip(shape)
                .background(item.color.background)
                .border(1.dp, item.color.border, shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(item.glyph.symbol, fontSize = 8.sp, color = item.color.foreground)
        Text(
            text = item.label,
            fontSize = fontSize,
            lineHeight = fontSize * 1.6f,
            fontWeight = FontWeight.Medium,
            fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
            color = item.color.foreground,
            textDecoration = if (item.strikeThrough) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}
