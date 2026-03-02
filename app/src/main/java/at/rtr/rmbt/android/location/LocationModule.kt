package at.rtr.rmbt.android.location

import android.content.Context
import at.specure.location.FusedLocationSource
import at.specure.location.GPSLocationSource
import at.specure.location.LocationWatcher
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Location module that should provide instance of [LocationWatcher] for concrete flavor
 */
@Module
class LocationModule {

    @Provides
    @Singleton
    fun provideLocationProviderNoNetwork(context: Context): LocationWatcher = LocationWatcher.Builder(context)
        .addSource(GPSLocationSource(context))
        .build()
}