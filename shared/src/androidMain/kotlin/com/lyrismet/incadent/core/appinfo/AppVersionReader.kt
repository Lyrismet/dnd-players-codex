package com.lyrismet.incadent.core.appinfo

import android.content.Context
import android.content.pm.PackageManager

actual class AppVersionReader(
    private val context: Context,
) {
    @Suppress("DEPRECATION")
    actual fun read(): AppVersion =
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            AppVersion.of(info.versionName, info.versionCode.toString())
        } catch (_: PackageManager.NameNotFoundException) {
            AppVersion.of(null, null)
        }
}
