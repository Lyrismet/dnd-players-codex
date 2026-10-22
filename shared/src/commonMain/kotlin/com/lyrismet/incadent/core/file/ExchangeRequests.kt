package com.lyrismet.incadent.core.file

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** a file flow some screen or the system asked for, the app root shows it over whatever is open */
sealed interface ExchangeRequest {
    data class Export(
        val campaignId: String,
    ) : ExchangeRequest

    data class Import(
        val origin: FileOrigin,
    ) : ExchangeRequest
}

/** where entry points leave an export or import request, a file opened from outside the app arrives here too */
@Inject
@SingleIn(AppScope::class)
class ExchangeRequests {
    private val state = MutableStateFlow<ExchangeRequest?>(null)
    val pending: StateFlow<ExchangeRequest?> = state

    fun request(request: ExchangeRequest) {
        state.value = request
    }

    fun clear() {
        state.value = null
    }
}
