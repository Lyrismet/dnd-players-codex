package com.lyrismet.incadent.di

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import com.slack.circuit.serialization.CircuitSerializerRegistration
import com.slack.circuit.serialization.SerializableCircuitSaver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/** assembles the one Circuit instance from every factory and screen registration the features contribute */
@BindingContainer
@ContributesTo(AppScope::class)
object CircuitBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideCircuit(
        presenterFactories: Set<Presenter.Factory>,
        uiFactories: Set<Ui.Factory>,
        screenRegistrations: Set<CircuitSerializerRegistration>,
    ): Circuit =
        Circuit
            .Builder()
            .addPresenterFactories(presenterFactories)
            .addUiFactories(uiFactories)
            .setCircuitSaver(SerializableCircuitSaver(screenRegistrations.toList()))
            .build()
}
