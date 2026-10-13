package com.lyrismet.incadent.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.portrait.rememberPhotoBitmap
import com.lyrismet.incadent.domain.model.Photo
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.photo_cover_expand
import org.jetbrains.compose.resources.stringResource

private val CardCoverShape = RoundedCornerShape(16.dp)
private val AddStripShape = RoundedCornerShape(14.dp)
private val FieldShape = RoundedCornerShape(14.dp)
private val FieldInnerShape = RoundedCornerShape(10.dp)
private val ExpandPillShape = RoundedCornerShape(15.dp)

// mockup - rgba(10,11,14,0.72) behind the "Развернуть" pill
private val ExpandPillBackground = Color(0xB80A0B0E)

/** the place's cover on top of its card, a tap opens it full screen */
@Composable
fun AppCoverFrame(
    cover: Photo,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val bitmap = rememberPhotoBitmap(cover.display)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(CardCoverShape)
                .background(AppPalette.Background)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().border(1.dp, AppPalette.Parchment.copy(alpha = 0.3f), CardCoverShape))
        if (onClick != null) {
            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .height(30.dp)
                        .clip(ExpandPillShape)
                        .background(ExpandPillBackground)
                        .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(Res.string.photo_cover_expand),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppPalette.TextHeading,
                )
            }
        }
    }
}

/** the "add a cover" strip of a card without one (quick mode) - 64dp, dashed, "+" and the label in parchment */
@Composable
fun AppCoverAddStrip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(AddStripShape)
                .dashedBorder(AppPalette.BorderHover, width = 1.5.dp, cornerRadius = 14.dp)
                .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("+", fontSize = 18.sp, color = AppPalette.Parchment)
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.Parchment)
    }
}

/** the cover field of the place form, a 150dp box with the picture or a dashed "+" with [placeholder] and [hint] */
@Composable
fun AppCoverField(
    cover: Photo?,
    placeholder: String,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(FieldShape)
                .background(AppPalette.Background)
                .border(1.dp, AppPalette.Border, FieldShape)
                .clickable(onClick = onClick),
    ) {
        if (cover != null) {
            val bitmap = rememberPhotoBitmap(cover.display)
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(FieldInnerShape)
                        .dashedBorder(AppPalette.BorderHover, width = 1.5.dp, cornerRadius = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
            ) {
                Text("+", fontSize = 22.sp, color = AppPalette.Parchment)
                Text(placeholder, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AppPalette.Parchment)
                Text(hint, fontSize = 11.5.sp, color = AppPalette.TextSecondary, textAlign = TextAlign.Center)
            }
        }
    }
}
