package at.rtr.rmbt.android.di

import android.content.Context
import at.specure.di.NotificationProvider
import dagger.Module
import dagger.Provides

@Module
class DependencyModule {

    @Provides
    fun provideNotificationProvider(context: Context): NotificationProvider = NotificationProviderImpl(context)
}
