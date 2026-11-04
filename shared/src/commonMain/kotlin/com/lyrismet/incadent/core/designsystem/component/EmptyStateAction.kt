package com.lyrismet.incadent.core.designsystem.component

/** one button of an [EmptyState], [tinted] is a quiet dark button with gold text instead of the solid gold one */
data class EmptyStateAction(
    val label: String,
    val tinted: Boolean = false,
    val onClick: () -> Unit,
)
