package at.rtr.rmbt.android.location

import android.content.Context
import at.specure.location.GPSLocationSource
import at.specure.location.LocationProducer
import at.specure.location.NetworkLocationSource
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class LocationModule {

    @Provides
    @Singleton
    fun provideLocationProvider(context: Context): LocationProducer = LocationProducer.Builder(context)
        .addSource(GPSLocationSource(context))
        .addSource(NetworkLocationSource(context))
        .build()

}