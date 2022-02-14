package at.rtr.rmbt.android

import android.content.Context
import at.rtr.rmbt.android.di.DaggerAppComponent
import at.rtr.rmbt.android.di.Injector
import at.specure.config.Config
import at.specure.di.CoreApp
import at.specure.di.CoreComponent
import at.specure.di.CoreInjector
import at.specure.worker.WorkLauncher
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.hms.maps.MapsInitializer
import java.io.File
import javax.inject.Inject

class App : CoreApp() {

    @Inject
    lateinit var config: Config

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
                sharedPreferences.edit().putBoolean("fixed", true).apply()
            }
        } catch (exception: Exception) {
        }

        val config = AGConnectServicesConfig.fromContext(this)
        MapsInitializer.setApiKey(config.getString("client/api_key"))
    }
}