package com.lyrismet.incadent.core.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// BCP 47 tag (eg "ru", "en"), null follows the OS locale - see AppLanguage for the app's actual supported values
var customAppLocale: String? by mutableStateOf(null)

/** platform hook to force stringResource() onto customAppLocale instead of the OS locale - see actuals per platform */
expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

@Composable
fun AppEnvironment(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppLocale provides customAppLocale) {
        key(customAppLocale) {
            content()
        }
    }
}
