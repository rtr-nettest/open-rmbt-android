package at.rtr.rmbt.android.di

import android.content.Context
import at.rmbt.client.control.ControlServerModule
import at.rtr.rmbt.android.App
import at.rtr.rmbt.android.config.ConfigModule
import at.rtr.rmbt.android.location.LocationModule
import at.rtr.rmbt.android.ui.dialog.HistoryFiltersDialog
import at.rtr.rmbt.android.ui.dialog.NetworkInfoDialog
import at.specure.di.CoreComponent
import at.specure.di.CoreModule
import at.specure.di.DatabaseModule
import at.specure.di.SystemModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

/**
 * Main Application component that wires all application modules together
 */
@Singleton
@Component(
    modules = [ViewModelModule::class,
        SystemModule::class,
        DependencyModule::class,
        DatabaseModule::class,
        CoreModule::class,
        ControlServerModule::class,
        ConfigModule::class,
        LocationModule::class]
)
interface AppComponent : CoreComponent {

    fun viewModelFactory(): ViewModelFactory

    fun inject(dialog: HistoryFiltersDialog)

    fun inject(dialog: NetworkInfoDialog)

    fun inject(app: App)

    @Component.Builder
    abstract class Builder {

        fun context(context: Context): Builder {
            seedContext(context)
            return this
        }

        @BindsInstance
        abstract fun seedContext(context: Context)

        abstract fun build(): AppComponent
    }
}