package com.lyrismet.incadent.core.appinfo

import platform.Foundation.NSBundle

actual class AppVersionReader {
    actual fun read(): AppVersion {
        val info = NSBundle.mainBundle
        return AppVersion.of(
            info.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String,
            info.objectForInfoDictionaryKey("CFBundleVersion") as? String,
        )
    }
}
