package com.lyrismet.incadent.core.designsystem.component

/** one button of an [EmptyStateActions] block */
data class EmptyStateAction(
    val label: String,
    val onClick: () -> Unit,
)
