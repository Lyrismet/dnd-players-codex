package com.lyrismet.incadent.core.designsystem.component.icons.entity

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.icons.silhouetteIcon
import com.lyrismet.incadent.domain.model.EntityKind

/** the seven silhouettes of the v9 set (pc-icons.js), one grid, tinted by the Icon's tint and built on first use */
object EntityIcons {
    val Npc: ImageVector by lazy {
        silhouetteIcon(
            name = "Npc",
            body =
                "M12 2.2C7.9 2.2 5.4 5.6 5.4 9.8c0 1.9.5 3.4 1.4 4.6C4.1 15.6 2.6 18.3 2.6 21.8h18.8" +
                    "c0-3.5-1.5-6.2-4.2-7.4.9-1.2 1.4-2.7 1.4-4.6C18.6 5.6 16.1 2.2 12 2.2ZM12 7.4" +
                    "c-2 0-3.3 1.7-3.3 3.8S10 15 12 15s3.3-1.7 3.3-3.8S14 7.4 12 7.4Z",
        )
    }
    val Location: ImageVector by lazy {
        silhouetteIcon(
            name = "Location",
            body =
                "M2 6.3c0-.4.3-.8.7-.9L7.9 3.6v14.6L3.2 20c-.6.2-1.2-.2-1.2-.9ZM9.1 3.6l5.8 2v14.6" +
                    "l-5.8-2ZM16.1 5.8l4.7-1.8c.6-.2 1.2.2 1.2.9v12.7c0 .4-.3.8-.7.9l-5.2 1.9Z",
        )
    }
    val Quest: ImageVector by lazy {
        silhouetteIcon(
            name = "Quest",
            body = "M5.6 5h12.8v13H5.6ZM8.4 9h7.2v2H8.4ZM8.4 12.7h4.6v2H8.4Z",
            extra =
                "M4.6 2.4h14.8a2.3 2.3 0 0 1 0 4.6H4.6a2.3 2.3 0 0 1 0-4.6ZM4.6 16.8h14.8" +
                    "a2.3 2.3 0 0 1 0 4.6H4.6a2.3 2.3 0 0 1 0-4.6Z",
        )
    }
    val Party: ImageVector by lazy {
        silhouetteIcon(
            name = "Party",
            body =
                "M8.4 7.4a3.6 3.6 0 1 0 7.2 0a3.6 3.6 0 1 0 -7.2 0ZM6.6 21.6C6.6 17 8.8 13.2 12 13.2" +
                    "S17.4 17 17.4 21.6ZM1.800 10.4a2.4 2.4 0 1 0 4.8 0a2.4 2.4 0 1 0 -4.8 0ZM1 21.6V19.4" +
                    "C1 17.4 2.2 15.6 4.2 15.6 4.6 15.6 5 15.7 5.3 15.8 4.9 16.9 4.7 18.1 4.7 19.4V21.6Z" +
                    "M17.400 10.4a2.4 2.4 0 1 0 4.8 0a2.4 2.4 0 1 0 -4.8 0ZM23 21.6V19.4" +
                    "C23 17.4 21.8 15.6 19.8 15.6 19.4 15.6 19 15.7 18.7 15.8 19.1 16.9 19.3 18.1 19.3 19.4" +
                    "V21.6Z",
        )
    }
    val Species: ImageVector by lazy {
        silhouetteIcon(
            name = "Species",
            body =
                "M12 12.5c3.5 0 6 3 6 5.7 0 2.2-1.7 3.3-3.4 3.3-1.1 0-1.8-.5-2.6-.5s-1.5.5-2.6.5" +
                    "C7.7 21.5 6 20.4 6 18.2c0-2.7 2.5-5.7 6-5.7Z",
            extra =
                "M2.3 11.2a2.3 2.3 0 1 0 4.6 0a2.3 2.3 0 1 0 -4.6 0ZM6.4 6.3a2.5 2.5 0 1 0 5 0" +
                    "a2.5 2.5 0 1 0 -5 0ZM12.6 6.3a2.5 2.5 0 1 0 5 0a2.5 2.5 0 1 0 -5 0ZM17.099 11.2" +
                    "a2.3 2.3 0 1 0 4.6 0a2.3 2.3 0 1 0 -4.6 0Z",
        )
    }
    val Item: ImageVector by lazy {
        silhouetteIcon(
            name = "Item",
            body =
                "M8.8 10.8H15.2C18.2 12.6 21 15.4 21 18C21 20.2 19.2 21.5 17 21.5H7" +
                    "C4.8 21.5 3 20.2 3 18C3 15.4 5.8 12.6 8.8 10.8Z",
            extra =
                "M8 2C9.3 2.8 10.7 2.8 12 2S14.7 1.2 16 2L14.6 6.4H9.4ZM9.9 6.4H14.1" +
                    "A1.3 1.3 0 0 1 14.1 9H9.9A1.3 1.3 0 0 1 9.9 6.4Z",
        )
    }
    val Me: ImageVector by lazy {
        silhouetteIcon(
            name = "Me",
            body = "M4 22V10A8 8 0 0 1 20 10V22ZM6 20.2V10A6 6 0 0 1 18 10V20.2Z",
            extra =
                "M9.4 10.4a2.6 2.6 0 1 0 5.2 0a2.6 2.6 0 1 0 -5.2 0ZM7.8 20.2" +
                    "C7.8 17.2 9.7 15.3 12 15.3S16.2 17.2 16.2 20.2Z",
        )
    }
}

fun EntityKind.icon(): ImageVector =
    when (this) {
        EntityKind.NPC -> EntityIcons.Npc
        EntityKind.PARTY -> EntityIcons.Party
        EntityKind.QUEST -> EntityIcons.Quest
        EntityKind.LOCATION -> EntityIcons.Location
        EntityKind.SPECIES -> EntityIcons.Species
    }

/** an entity silhouette in a square of [size] (14 dp in rows and chips, 24 and 48 dp in badges), filled with [tint] */
@Composable
fun AppEntityIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = modifier.size(size))
}
