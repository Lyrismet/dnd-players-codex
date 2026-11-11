package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

private const val DISMISS_PULL_PX = 160f

/** a pull down that the scrolled content did not take (it sits at its top) drags the panel, enough of it closes it */
internal class SwipeDownToDismiss(
    private val onPull: (Float) -> Unit,
    private val onDismiss: () -> Unit,
) : NestedScrollConnection {
    private var pulled = 0f

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (pulled <= 0f || available.y >= 0f) return Offset.Zero
        val back = maxOf(available.y, -pulled)
        move(pulled + back)
        return Offset(0f, back)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
        move(pulled + available.y)
        return Offset(0f, available.y)
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        val close = pulled >= DISMISS_PULL_PX
        move(0f)
        if (close) onDismiss()
        return Velocity.Zero
    }

    private fun move(value: Float) {
        pulled = value
        onPull(value)
    }
}
