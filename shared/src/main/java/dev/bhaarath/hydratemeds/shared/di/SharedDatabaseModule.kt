package dev.bhaarath.hydratemeds.shared.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.bhaarath.hydratemeds.shared.database.AcknowledgmentLogDao
import dev.bhaarath.hydratemeds.shared.database.ActiveReminderDao
import dev.bhaarath.hydratemeds.shared.database.DailyReminderPauseDao
import dev.bhaarath.hydratemeds.shared.database.HydrateMedsDatabase
import dev.bhaarath.hydratemeds.shared.repository.ReminderRepository
import dev.bhaarath.hydratemeds.shared.repository.RoomReminderRepository
import dev.bhaarath.hydratemeds.shared.runtime.ReminderRuntime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SharedDatabaseProviders {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): HydrateMedsDatabase =
        ReminderRuntime.database(context)

    @Provides
    fun provideAcknowledgmentLogDao(database: HydrateMedsDatabase): AcknowledgmentLogDao =
        database.acknowledgmentLogDao()

    @Provides
    fun provideActiveReminderDao(database: HydrateMedsDatabase): ActiveReminderDao =
        database.activeReminderDao()

    @Provides
    fun provideDailyReminderPauseDao(database: HydrateMedsDatabase): DailyReminderPauseDao =
        database.dailyReminderPauseDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SharedRepositoryBindings {
    @Binds
    @Singleton
    abstract fun bindReminderRepository(
        implementation: RoomReminderRepository,
    ): ReminderRepository
}
