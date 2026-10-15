package com.lyrismet.incadent.core.preferences

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun deviceLanguageTag(): String = NSLocale.currentLocale.languageCode
