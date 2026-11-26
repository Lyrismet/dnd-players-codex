package com.lyrismet.incadent.feature.codex.data.remote

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent

private const val CONNECT_TIMEOUT_MILLIS = 8_000L
private const val REQUEST_TIMEOUT_MILLIS = 15_000L

/** redirects are followed by hand, so one that leaves the known sites can be stopped */
internal fun HttpClientConfig<*>.configureAppClient() {
    followRedirects = false
    install(UserAgent) { agent = "Incadent" }
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
    }
}

/** the one http client of the app, every remote data source reads through it */
@BindingContainer
@ContributesTo(AppScope::class)
object HttpClientBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideHttpClient(): HttpClient = HttpClient { configureAppClient() }
}
