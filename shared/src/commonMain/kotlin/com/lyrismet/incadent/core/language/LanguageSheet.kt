package com.lyrismet.incadent.core.language

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppSearchField
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.domain.model.AppLanguage
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.settings_language_empty
import dndplayerscodex.shared.generated.resources.settings_language_en
import dndplayerscodex.shared.generated.resources.settings_language_en_sub
import dndplayerscodex.shared.generated.resources.settings_language_footnote
import dndplayerscodex.shared.generated.resources.settings_language_ru
import dndplayerscodex.shared.generated.resources.settings_language_ru_sub
import dndplayerscodex.shared.generated.resources.settings_language_search
import dndplayerscodex.shared.generated.resources.settings_language_title
import dndplayerscodex.shared.generated.resources.settings_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LanguageRow(
    title: String,
    description: String,
    current: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 56.dp)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(
            size = 32.dp,
            shape = RoundedCornerShape(9.dp),
            background = AppPalette.SurfacePopover,
            border = AppPalette.Border,
        ) {
            Image(imageVector = GlobeIcon, contentDescription = null)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                color = AppPalette.TextPrimary,
            )
            Text(
                text = description,
                modifier = Modifier.padding(top = 2.dp),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = AppPalette.TextSecondary,
            )
        }
        Text(text = current, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.GoldBright)
        Text(text = "›", fontSize = 20.sp, color = AppPalette.TextTertiary)
    }
}

// the mockup's globe - a circle, the equator and a meridian lens, 1.6 strokes on a 24 grid
internal val GlobeIcon: ImageVector =
    ImageVector
        .Builder(
            name = "Globe",
            defaultWidth = 17.dp,
            defaultHeight = 17.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            listOf(
                "M21 12a9 9 0 1 1-18 0a9 9 0 1 1 18 0z",
                "M3 12h18",
                "M12 3c2.6 2.6 3.8 5.6 3.8 9s-1.2 6.4-3.8 9c-2.6-2.6-3.8-5.6-3.8-9S9.4 5.6 12 3z",
            ).forEach { path ->
                addPath(
                    pathData = addPathNodes(path),
                    stroke = SolidColor(AppPalette.Parchment),
                    strokeLineWidth = 1.6f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

/** the app's languages in a searchable sheet - the list is [AppLanguage.entries], so a new one shows up alone */
@Composable
internal fun LanguageSheet(
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val rows =
        AppLanguage.entries
            .map { LanguageOption(it, it.label(), it.subLabel()) }
            .filter { query.isBlank() || "${it.name} ${it.sub}".contains(query.trim(), ignoreCase = true) }
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 44.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppSheetHeader(
                overline = stringResource(Res.string.settings_title),
                title = stringResource(Res.string.settings_language_title),
                onClose = onDismiss,
            )
            AppSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(Res.string.settings_language_search),
                height = 44.dp,
                textSize = 14.sp,
                background = AppPalette.Background,
            )
            if (rows.isEmpty()) {
                Text(
                    stringResource(Res.string.settings_language_empty),
                    modifier = Modifier.padding(2.dp),
                    fontSize = 13.sp,
                    color = AppPalette.TextTertiary,
                )
            } else {
                LanguageList(rows, selected, onSelected)
            }
            Text(
                stringResource(Res.string.settings_language_footnote),
                fontSize = 12.sp,
                lineHeight = FootnoteLineHeight,
                color = AppPalette.TextTertiary,
            )
        }
    }
}

private val FootnoteLineHeight = 18.6.sp
private val ListShape = RoundedCornerShape(14.dp)

private class LanguageOption(
    val language: AppLanguage,
    val name: String,
    val sub: String,
)

@Composable
private fun LanguageList(
    rows: List<LanguageOption>,
    selected: AppLanguage,
    onSelected: (AppLanguage) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(shape = ListShape, background = AppPalette.Background)
                .padding(horizontal = 14.dp, vertical = 2.dp),
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) AppDivider(color = AppPalette.SurfacePopover)
            LanguageOptionRow(row, row.language == selected) { onSelected(row.language) }
        }
    }
}

@Composable
private fun LanguageOptionRow(
    row: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 54.dp)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                row.name,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = AppPalette.TextHeading,
            )
            Text(
                row.sub,
                modifier = Modifier.padding(top = 1.dp),
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = AppPalette.TextSecondary,
            )
        }
        RadioMark(isSelected)
    }
}

// the mockup's 22px ring with a 10px dot - gold while it is the current language
@Composable
private fun RadioMark(isSelected: Boolean) {
    val ring = if (isSelected) AppPalette.Gold else AppPalette.RadioIdle
    Box(
        modifier = Modifier.size(22.dp).border(1.5.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) Box(Modifier.size(10.dp).background(AppPalette.Gold, CircleShape))
    }
}

// the other script's name, so a reader of that language still recognises the row
@Composable
private fun AppLanguage.subLabel(): String =
    when (this) {
        AppLanguage.RUSSIAN -> stringResource(Res.string.settings_language_ru_sub)
        AppLanguage.ENGLISH -> stringResource(Res.string.settings_language_en_sub)
    }

@Composable
internal fun AppLanguage.label(): String =
    when (this) {
        AppLanguage.RUSSIAN -> stringResource(Res.string.settings_language_ru)
        AppLanguage.ENGLISH -> stringResource(Res.string.settings_language_en)
    }
