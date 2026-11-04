package com.lyrismet.incadent.core.designsystem.component

/** one button of an [EmptyState] */
data class EmptyStateAction(
    val label: String,
    val onClick: () -> Unit,
)
