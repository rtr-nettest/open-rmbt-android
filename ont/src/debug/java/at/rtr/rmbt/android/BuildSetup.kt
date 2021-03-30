package at.rtr.rmbt.android

import android.app.Application
import com.facebook.stetho.Stetho
import timber.log.Timber

/**
 * Method should be called on [Application.onCreate] method
 * to setup build environment for debug variant
 */
fun setupBuildEnvironment(app: Application) {
    Timber.plant(Timber.DebugTree())
    Stetho.initializeWithDefaults(app)
}