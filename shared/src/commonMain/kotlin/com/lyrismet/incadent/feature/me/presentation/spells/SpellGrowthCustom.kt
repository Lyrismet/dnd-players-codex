package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.component.FormInlineField
import com.lyrismet.incadent.feature.codex.domain.model.CIRCLE_PLACEHOLDER
import com.lyrismet.incadent.feature.codex.domain.model.MAX_CUSTOM_TEXT
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_growth_custom_circle_hint
import incadent.shared.generated.resources.me_spells_growth_custom_text_hint
import org.jetbrains.compose.resources.stringResource

/** the one text of a custom rule, the placeholder is where the circle of the slot goes */
@Composable
internal fun CustomText(
    value: String,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FormInlineField(
            value = value,
            onChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.me_spells_growth_custom_text_hint),
            maxLength = MAX_CUSTOM_TEXT,
        )
        Text(
            stringResource(Res.string.me_spells_growth_custom_circle_hint, CIRCLE_PLACEHOLDER),
            style = AppText.footnote,
            color = AppPalette.TextTertiary,
        )
    }
}
