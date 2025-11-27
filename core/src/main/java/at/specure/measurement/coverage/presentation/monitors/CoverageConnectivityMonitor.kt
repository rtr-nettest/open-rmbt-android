package at.specure.measurement.coverage.presentation.monitors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import at.specure.measurement.coverage.domain.monitors.ConnectivityMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RtrConnectivityMonitor @Inject constructor(
    private val context: Context,
    private val telephonyManager: TelephonyManager,
): ConnectivityMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    // -------------------- AIRPLANE MODE --------------------

    private fun airplaneModeFlow(): Flow<Boolean> = callbackFlow {

        fun isEnabled(): Boolean =
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AIRPLANE_MODE_ON,
                0
            ) == 1

        trySend(isEnabled())

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                trySend(isEnabled())
            }
        }

        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED))

        awaitClose { context.unregisterReceiver(receiver) }

    }.distinctUntilChanged()


    override fun isAirplaneModeCurrentlyEnabled(): Boolean {
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) == 1
    }

    // -------------------- MOBILE DATA ENABLED --------------------

    private fun mobileDataEnabledFlow(): Flow<Boolean> =
        flow {
            while (true) {
                emit(isMobileDataEnabled())
                delay(2000)
            }
        }.distinctUntilChanged()

    override fun isMobileDataEnabled(): Boolean =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager.isDataEnabled
            } else true
        } catch (e: Exception) {
            false
        }

    // -------------------- PUBLIC START / STOP --------------------

    override fun start(
        onAirplaneEnabled: () -> Unit,
        onAirplaneDisabled: () -> Unit,
        onMobileDataEnabled: () -> Unit,
        onMobileDataDisabled: () -> Unit
    ) {
        if (monitorJob != null) return

        monitorJob = scope.launch {

            merge(
                airplaneModeFlow()
                    .drop(1)
                    .onEach {
                    if (it) onAirplaneEnabled() else onAirplaneDisabled()
                },

                mobileDataEnabledFlow()
                    .drop(1)
                    .onEach {
                    if (it) onMobileDataEnabled() else onMobileDataDisabled()
                }

            ).collect()
        }
    }

    override fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }
}
