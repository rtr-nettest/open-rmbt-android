package at.rtr.rmbt.android.di

import android.content.Context
import at.specure.di.NotificationProvider
import at.specure.location.LocationInfoWatcherFusedImpl
import at.specure.location.LocationWatcher
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DependencyModule {

    @Provides
    @Singleton
    fun provideLocationInfo(context: Context): LocationWatcher =
        LocationInfoWatcherFusedImpl(context)

    @Provides
    fun provideNotificationProvider(context: Context): NotificationProvider = NotificationProviderImpl(context)
}
