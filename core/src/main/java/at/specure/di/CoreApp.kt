package at.specure.di

import android.app.Application

/**
 * Basic application class that should be extended in module that uses core library
 * This class contains links to the core module that allows to inject it in components.
 */
abstract class CoreApp : Application() {

    abstract val coreComponent: CoreComponent

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {

        private lateinit var instance: CoreApp

        val component: CoreComponent
            get() = instance.coreComponent
    }
}