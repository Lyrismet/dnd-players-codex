package com.lyrismet.incadent.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** the corner radii of the design - pick one by what the surface is, not by its number */
object AppShapes {
    /** cards, list blocks, floating bars, the large form button */
    val Card = RoundedCornerShape(14.dp)

    /** inputs, select rows, dialog buttons */
    val Field = RoundedCornerShape(12.dp)

    /** small buttons, header pills, icon slots */
    val Control = RoundedCornerShape(10.dp)

    /** avatars and entity emblems drawn as rounded squares */
    val Tile = RoundedCornerShape(9.dp)

    /** inline chips and the smallest controls */
    val Small = RoundedCornerShape(8.dp)

    /** status and tag badges */
    val Badge = RoundedCornerShape(6.dp)

    /** option pills and chips */
    val Pill = RoundedCornerShape(20.dp)
}
