package com.lyrismet.incadent.core.appinfo

/** where the latest published version comes from, null when it cannot be known */
interface LatestVersionSource {
    suspend fun latest(): VersionNumber?
}
