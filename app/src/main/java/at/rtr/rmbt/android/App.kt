package at.rtr.rmbt.android

import at.rtr.rmbt.android.di.DaggerAppComponent
import at.rtr.rmbt.android.di.Injector
import at.specure.di.CoreApp
import at.specure.di.CoreComponent
import at.specure.di.CoreInjector
import at.specure.worker.WorkLauncher

class App : CoreApp() {

    override val coreComponent: CoreComponent
        get() = Injector.component

    override fun onCreate() {
        super.onCreate()

        setupAnalyticsEnvironment(this)
        setupBuildEnvironment(this)

        Injector.component = DaggerAppComponent.builder()
            .context(this)
            .build()

        CoreInjector.component = Injector.component

        WorkLauncher.enqueueSettingsRequest(this)
    }
}