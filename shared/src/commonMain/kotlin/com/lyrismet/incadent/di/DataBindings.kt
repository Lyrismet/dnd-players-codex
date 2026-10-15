package com.lyrismet.incadent.di

import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.data.db.DatabaseDriverFactory
import com.lyrismet.incadent.data.db.createAppDatabase
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.repository.CodexRepositories
import com.lyrismet.incadent.domain.repository.LocationRepository
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.QuestRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** bindings for objects that cannot carry @Inject themselves - the database, expect classes and plain wrappers */
@BindingContainer
@ContributesTo(AppScope::class)
object DataBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabase(driverFactory: DatabaseDriverFactory): AppDatabase =
        createAppDatabase(driverFactory.createDriver())

    @Provides
    fun provideCodexRepositories(
        partyRepository: PartyRepository,
        npcRepository: NpcRepository,
        questRepository: QuestRepository,
        locationRepository: LocationRepository,
    ): CodexRepositories = CodexRepositories(partyRepository, npcRepository, questRepository, locationRepository)

    @Provides
    @SingleIn(AppScope::class)
    fun provideImageCompressor(): ImageCompressor = ImageCompressor()

    @Provides
    @SingleIn(AppScope::class)
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
