package com.lyrismet.incadent.core.preferences

import android.content.res.Resources

// the system resources are untouched by the app's own locale override
actual fun deviceLanguageTag(): String =
    Resources
        .getSystem()
        .configuration.locales[0]
        .language
