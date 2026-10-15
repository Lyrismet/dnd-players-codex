package com.lyrismet.incadent.core.preferences

import com.russhwolf.settings.ObservableSettings

expect class SettingsFactory {
    fun createSettings(): ObservableSettings
}
