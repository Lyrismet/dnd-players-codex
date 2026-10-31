package com.lyrismet.incadent.core.appinfo

import android.content.Context

actual class AppVersionReader(
    private val context: Context,
) {
    @Suppress("DEPRECATION")
    actual fun read(): AppVersion {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return AppVersion.of(info.versionName, info.versionCode.toString())
    }
}
