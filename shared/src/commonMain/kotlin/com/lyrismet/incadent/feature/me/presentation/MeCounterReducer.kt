package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.feature.me.domain.steppedMax

internal fun MeOverlay.Slots.reduceSlots(event: OverlayEvent): MeOverlay =
    if (event is OverlayEvent.SlotStepped) {
        copy(maxima = maxima + (event.level to (maxima[event.level] ?: 0).steppedMax(event.delta)))
    } else {
        this
    }

internal fun MeOverlay.Resource.reduceResource(event: OverlayEvent): MeOverlay =
    when (event) {
        is OverlayEvent.NumberStepped -> copy(max = max.steppedMax(event.delta))
        is OverlayEvent.TextChanged -> if (event.field == OverlayField.RESOURCE_NAME) copy(name = event.value) else this
        else -> this
    }
