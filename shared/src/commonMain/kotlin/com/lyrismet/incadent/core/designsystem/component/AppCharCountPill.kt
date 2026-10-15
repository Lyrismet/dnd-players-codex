package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette

private const val NEAR_LIMIT_SHARE = 0.9f
private val CounterShape = RoundedCornerShape(10.dp)

/** the "3/24" pill - turns gold when the field is within 10% of its cap */
@Composable
fun AppCharCountPill(
    count: Int,
    max: Int,
    modifier: Modifier = Modifier,
) {
    val nearLimit = count >= max * NEAR_LIMIT_SHARE
    Text(
        text = "$count/$max",
        fontSize = 10.5.sp,
        color = if (nearLimit) AppPalette.GoldBright else AppPalette.TextTertiary,
        modifier =
            modifier
                .border(1.dp, if (nearLimit) AppPalette.Gold.copy(alpha = 0.5f) else AppPalette.Border, CounterShape)
                .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}
