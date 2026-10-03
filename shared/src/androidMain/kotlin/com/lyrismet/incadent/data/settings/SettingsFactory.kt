package com.lyrismet.incadent.data.settings

import android.content.Context
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings

actual class SettingsFactory(
    private val context: Context,
) {
    actual fun createSettings(): ObservableSettings =
        SharedPreferencesSettings(context.getSharedPreferences("dnd_players_codex_settings", Context.MODE_PRIVATE))
}
