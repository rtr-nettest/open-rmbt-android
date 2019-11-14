package at.rtr.rmbt.android.di

import android.content.Context
import at.specure.location.LocationWatcher
import at.specure.location.LocationInfoWatcherFusedImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class LocationModule {

    @Provides
    @Singleton
    fun provideLocationInfo(context: Context): LocationWatcher =
        LocationInfoWatcherFusedImpl(context)
}
