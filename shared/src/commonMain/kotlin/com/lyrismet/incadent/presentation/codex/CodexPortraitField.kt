package com.lyrismet.incadent.presentation.codex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.HeaderActionButton
import com.lyrismet.incadent.core.designsystem.component.PortraitFrame
import com.lyrismet.incadent.core.designsystem.component.PortraitSourceMenu
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.portrait.portraitSizeKb
import com.lyrismet.incadent.core.portrait.rememberImagePickerLauncher
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_label_portrait
import dndplayerscodex.shared.generated.resources.codex_entry_portrait_hint_empty
import dndplayerscodex.shared.generated.resources.codex_entry_portrait_hint_filled
import dndplayerscodex.shared.generated.resources.codex_entry_portrait_remove
import org.jetbrains.compose.resources.stringResource

private val PortraitCardModifier =
    Modifier.fillMaxWidth().appCard(RoundedCornerShape(14.dp), AppPalette.Background, AppPalette.Border).padding(12.dp)

/** the arch portrait slot shared by npc and party forms, see Players Codex v6.dc.html's form `photo()` */
@Composable
internal fun PortraitField(
    form: CodexEntryFormState,
    eventSink: (CodexEvent) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val launcher = rememberImagePickerLauncher(onPicked = { eventSink(CodexEvent.EntryPortraitPicked(it)) })
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionOverline(
            text = stringResource(Res.string.codex_entry_label_portrait),
            color = AppPalette.TextTertiary,
            fontSize = 10.sp,
            letterSpacing = 0.14.em,
        )
        Row(
            modifier = PortraitCardModifier,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PortraitFrame(portraitBase64 = form.portraitBase64, onClick = { menuOpen = true })
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val portrait = form.portraitBase64
                val hint =
                    if (portrait != null) {
                        stringResource(Res.string.codex_entry_portrait_hint_filled, portraitSizeKb(portrait))
                    } else {
                        stringResource(Res.string.codex_entry_portrait_hint_empty)
                    }
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                    color = AppPalette.TextMuted,
                )
                if (portrait != null) {
                    HeaderActionButton(
                        text = stringResource(Res.string.codex_entry_portrait_remove),
                        onClick = { eventSink(CodexEvent.EntryPortraitRemoved) },
                        modifier = Modifier.height(36.dp),
                        foreground = AppPalette.TextMuted,
                        background = Color.Transparent,
                        border = AppPalette.BorderHover,
                    )
                }
            }
        }
    }
    PortraitSourceMenu(
        expanded = menuOpen,
        onCamera = {
            menuOpen = false
            launcher.launchCamera()
        },
        onGallery = {
            menuOpen = false
            launcher.launchGallery()
        },
        onDismiss = { menuOpen = false },
    )
}
