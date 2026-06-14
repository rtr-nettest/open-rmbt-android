package at.rtr.rmbt.android

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.work.Configuration
import at.rtr.rmbt.android.di.DaggerAppComponent
import at.rtr.rmbt.android.di.Injector
import at.rtr.rmbt.android.util.LocaleHelper
import at.specure.config.Config
import at.specure.di.CoreComponent
import at.specure.di.CoreApp
import at.specure.di.CoreInjector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import at.specure.worker.WorkLauncher
import java.io.File
import javax.inject.Inject
import androidx.core.content.edit

class App : CoreApp(), Configuration.Provider {

    @Inject
    lateinit var config: Config

    override val coreComponent: CoreComponent
        get() = Injector.component


    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()

        setupAnalyticsEnvironment(this)
        setupBuildEnvironment(this)

        Injector.component = DaggerAppComponent.builder()
            .context(this)
            .build()

        CoreInjector.component = Injector.component


        WorkLauncher.enqueueSettingsRequest(this)
        WorkLauncher.enqueueCoverageSyncRequest(this)

        // Trim historic measurement data at app start and on every foreground resume so the DB
        // cannot accumulate submitted/unsendable measurements for long.
        registerActivityLifecycleCallbacks(retentionLifecycleCallback)
        runDatabaseRetentionDebounced()

        // https://issuetracker.google.com/issues/154855417#comment367 Workaround
        try {
            val sharedPreferences = getSharedPreferences("google_bug_154855417", Context.MODE_PRIVATE)
            if (!sharedPreferences.contains("fixed")) {
                val corruptedZoomTables = File(filesDir, "ZoomTables.data")
                val corruptedSavedClientParameters = File(filesDir, "SavedClientParameters.data.cs")
                val corruptedClientParametersData = File(filesDir, "DATA_ServerControlledParametersManager.data.$packageName")
                val corruptedClientParametersDataV1 = File(filesDir, "DATA_ServerControlledParametersManager.data.v1.$packageName")
                corruptedZoomTables.delete()
                corruptedSavedClientParameters.delete()
                corruptedClientParametersData.delete()
                corruptedClientParametersDataV1.delete()
                sharedPreferences.edit() { putBoolean("fixed", true) }
            }
        } catch (exception: Exception) {
        }
    }


    private val retentionScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastRetentionRunMs = 0L

    private fun runDatabaseRetentionDebounced() {
        val now = System.currentTimeMillis()
        if (now - lastRetentionRunMs < RETENTION_MIN_INTERVAL_MS) return
        lastRetentionRunMs = now
        retentionScope.launch {
            runCatching { CoreApp.component.signalMeasurementRepository().runDatabaseRetention() }
                .onFailure { Log.e("App", "Database retention failed", it) }
        }
    }

    private val retentionLifecycleCallback = object : ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) = runDatabaseRetentionDebounced()
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }

    companion object {
        // Don't re-run retention more often than this (it also fires on every activity resume).
        private const val RETENTION_MIN_INTERVAL_MS = 10L * 60 * 1000 // 10 minutes
    }
}
