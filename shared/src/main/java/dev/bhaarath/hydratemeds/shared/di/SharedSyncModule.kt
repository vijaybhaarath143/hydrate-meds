package dev.bhaarath.hydratemeds.shared.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.bhaarath.hydratemeds.shared.sync.ReminderDataLayerSync
import dev.bhaarath.hydratemeds.shared.sync.WearableReminderDataLayerSync
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SharedSyncModule {
    @Binds
    @Singleton
    abstract fun bindReminderDataLayerSync(
        implementation: WearableReminderDataLayerSync,
    ): ReminderDataLayerSync
}

