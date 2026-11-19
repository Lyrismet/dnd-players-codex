package com.lyrismet.incadent.feature.me.presentation.spells

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppDamageGlyph
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.appCard
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_spells_edit_overline
import incadent.shared.generated.resources.me_spells_school_none
import incadent.shared.generated.resources.me_spells_school_title
import org.jetbrains.compose.resources.stringResource

private const val TILES_PER_ROW = 2
private val TileHeight = 48.dp

/** the schools as tiles in two columns with their icons, a wide tile on top clears the choice */
@Composable
internal fun SchoolSheet(
    spellName: String,
    chosen: String,
    onClose: () -> Unit,
    onPick: (String) -> Unit,
) {
    val none = stringResource(Res.string.me_spells_school_none)
    AppBottomSheet(onDismissRequest = onClose) {
        AppSheetColumn(
            header = {
                AppSheetHeader(
                    title = stringResource(Res.string.me_spells_school_title),
                    onClose = onClose,
                    overline =
                        listOf(stringResource(Res.string.me_spells_edit_overline), spellName)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                            .uppercase(),
                )
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = TileHeight)
                        .appCard(
                            AppShapes.Field,
                            AppPalette.Background,
                            if (chosen.isBlank()) AppPalette.Gold else AppPalette.BorderSubtle,
                            onClick = { onPick("") },
                        ).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) { TileText(none.lowercase(), AppPalette.TextSecondary) }
                Schools.chunked(TILES_PER_ROW).forEach { line ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        line.forEach { school ->
                            val name = stringResource(school.name)
                            SchoolTile(school, name, name == chosen, Modifier.weight(1f)) { onPick(name) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SchoolTile(
    school: School,
    name: String,
    on: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .heightIn(min = TileHeight)
            .appCard(
                AppShapes.Field,
                if (on) AppPalette.SurfaceElevated else AppPalette.Background,
                if (on) school.color else AppPalette.BorderSubtle,
                onClick = onClick,
            ).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppDamageGlyph(school.path, school.color, size = 18.dp)
        TileText(name.lowercase(), AppPalette.TextPrimary)
    }
}

@Composable
private fun TileText(
    text: String,
    color: Color,
) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = color,
        maxLines = 1,
    )
}
