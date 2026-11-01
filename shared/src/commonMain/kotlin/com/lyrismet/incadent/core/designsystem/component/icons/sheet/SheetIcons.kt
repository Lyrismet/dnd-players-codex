package com.lyrismet.incadent.core.designsystem.component.icons.sheet

import androidx.compose.ui.graphics.vector.ImageVector
import com.lyrismet.incadent.core.designsystem.component.icons.silhouetteIcon

/** the interface glyphs of the v9 design system for the character sheet, same grid and style as the entity icons */
object SheetIcons {
    val Heart: ImageVector =
        silhouetteIcon(
            name = "Heart",
            body =
                "M12 21.2C6 16.8 2.2 13.2 2.2 8.8 2.2 5.6 4.6 3.2 7.6 3.2" +
                    "c1.8 0 3.4.9 4.4 2.4 1-1.5 2.6-2.4 4.4-2.4 3 0 5.4 2.4 5.4 5.6 0 4.4-3.8 8-9.8 12.4Z",
        )
    val Shield: ImageVector =
        silhouetteIcon(
            name = "Shield",
            body =
                "M12 2.2c2.6 1.4 5.4 2 8.2 2v6.4c0 5.6-3.4 9.6-8.2 11.8C7.2 20.2 3.8 16.2 3.8 10.6V4.2" +
                    "c2.8 0 5.6-.6 8.2-2Z",
        )
    val Bolt: ImageVector =
        silhouetteIcon(
            name = "Bolt",
            body =
                "M13.6 1.8 4.6 13.4c-.4.5 0 1.2.6 1.2H11l-1.4 7.4c-.1.6.7.9 1.1.4l8.8-11.6" +
                    "c.4-.5 0-1.2-.6-1.2H13l1.7-7.2c.2-.6-.7-1-1.1-.6Z",
        )
    val Boot: ImageVector =
        silhouetteIcon(
            name = "Boot",
            body =
                "M7.2 2.2H13.4C13.9 2.2 14.2 2.5 14.2 3V11.2L18.9 13.6C20.4 14.4 21.4 15.9 21.4 17.6" +
                    "V20.8C21.4 21.3 21 21.7 20.5 21.7H3.5C3 21.7 2.6 21.3 2.6 20.8V19" +
                    "C2.6 18.5 2.8 18 3.2 17.6L6.4 14.2V3C6.4 2.5 6.7 2.2 7.2 2.2ZM2.6 18.1H21.4V19.7H2.6Z",
        )
    val Medal: ImageVector =
        silhouetteIcon(
            name = "Medal",
            body =
                "M5.4 15.2a6.6 6.6 0 1 0 13.2 0a6.6 6.6 0 1 0 -13.2 0ZM8.6 15.2a3.4 3.4 0 1 0 6.8 0" +
                    "a3.4 3.4 0 1 0 -6.8 0Z",
            extra =
                "M6.4 1.8H10.6L13.2 7.9 10.1 9.5ZM17.6 1.8H13.4L10.8 7.9 13.9 9.5ZM10.3 15.2" +
                    "a1.7 1.7 0 1 0 3.4 0a1.7 1.7 0 1 0 -3.4 0Z",
        )
    val Eye: ImageVector =
        silhouetteIcon(
            name = "Eye",
            body =
                "M1.8 12C4.4 7.4 8 5 12 5s7.6 2.4 10.2 7c-2.6 4.6-6.2 7-10.2 7S4.4 16.6 1.8 12ZM8.2 12" +
                    "a3.8 3.8 0 1 0 7.6 0a3.8 3.8 0 1 0 -7.6 0Z",
            extra = "M10.2 12a1.8 1.8 0 1 0 3.6 0a1.8 1.8 0 1 0 -3.6 0Z",
        )
    val Campfire: ImageVector =
        silhouetteIcon(
            name = "Campfire",
            body =
                "M12 1.8C13 4.6 17 6.6 17 11C17 14 14.8 16 12 16S7 14 7 11C7 8.6 8.6 7.4 9.4 5.6" +
                    "C10.2 7 10.6 8 11.4 8.4C11.2 6 11.4 3.8 12 1.8ZM12 9.4C12.7 10.8 13.8 11.5 13.8 12.6" +
                    "C13.8 13.6 13 14.2 12 14.2S10.2 13.6 10.2 12.6C10.2 11.5 11.3 10.8 12 9.4Z",
            extra = "M5.8 20.2 18.6 17.1 19 18.6 6.2 21.7ZM5.4 17.1 18.2 20.2 17.8 21.7 5 18.6Z",
        )
    val Coin: ImageVector =
        silhouetteIcon(
            name = "Coin",
            body =
                "M2.6 12a9.4 9.4 0 1 0 18.8 0a9.4 9.4 0 1 0 -18.8 0ZM5.4 12a6.6 6.6 0 1 0 13.2 0" +
                    "a6.6 6.6 0 1 0 -13.2 0Z",
            extra = "M8.4 12a3.6 3.6 0 1 0 7.2 0a3.6 3.6 0 1 0 -7.2 0Z",
        )
}
