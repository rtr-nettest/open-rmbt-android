package at.rtr.rmbt.android.location

import android.content.Context
//import at.specure.location.FusedLocationSource
import at.specure.location.GPSLocationSource
import at.specure.location.LocationWatcher
import at.specure.location.NetworkLocationSource
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
    fun provideLocationProvider(context: Context): LocationWatcher = LocationWatcher.Builder(context)
        .addSource(GPSLocationSource(context))
        .addSource(NetworkLocationSource(context))
        //.addSource(FusedLocationSource(context))
        .build()
}