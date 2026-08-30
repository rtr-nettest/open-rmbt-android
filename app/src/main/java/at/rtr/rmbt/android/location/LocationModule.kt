package at.rtr.rmbt.android.location

import android.content.Context
import at.specure.location.CombinedLocationDispatcher
import at.specure.location.FusedLocationSource
import at.specure.location.GPSLocationSource
import at.specure.location.LocationWatcher
import at.specure.location.NetworkLocationSource
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

/**
 * Provides two [LocationWatcher] instances:
 *  - default (unqualified) = "combined-location": gps + fused + network, reporting the best of them
 *    per [CombinedLocationDispatcher]. Used by the start screen and the normal measurement.
 *  - @Named("gps-location") = GNSS only. Used by the signal (coverage) measurement, so it always
 *    reports a real "gps" fix.
 */
@Module
class LocationModule {

    @Provides
    @Singleton
    fun provideCombinedLocationWatcher(context: Context): LocationWatcher = LocationWatcher.Builder(context)
        .addSource(GPSLocationSource(context))
        .addSource(NetworkLocationSource(context))
        .addSource(FusedLocationSource(context))
        .dispatcher(CombinedLocationDispatcher())
        .build()

    @Provides
    @Singleton
    @Named(GPS_LOCATION)
    fun provideGpsLocationWatcher(context: Context): LocationWatcher = LocationWatcher.Builder(context)
        .addSource(GPSLocationSource(context))
        .build()

    companion object {
        const val GPS_LOCATION = "gps-location"
    }
}
