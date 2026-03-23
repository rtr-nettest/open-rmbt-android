package at.specure.measurement.coverage.presentation.monitors

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import at.specure.measurement.coverage.domain.monitors.ConnectivityMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RtrConnectivityMonitor @Inject constructor(
    private val context: Context,
    private val telephonyManager: TelephonyManager,
) : ConnectivityMonitor {

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

//     // Not working on eSIM devices (e.g. DJ's pixel 6 with dual sim (1 eSIM, 1 physical SIM))
//    override fun isMobileDataEnabled(): Boolean =
//        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                telephonyManager.isDataEnabled
//            } else true
//        } catch (e: Exception) {
//            if (e is CancellationException) {
//                throw e
//            }
//            false
//        }

//    /**
//     * Now it checks ability to connect to a network with some capabilities and if there is an airplane mode enabled then there is no such a possibility,
//     * so it returns null, also it uses deprecated  method so it can stop to work with new versions. when there is no network in reach it will return false.
//     */
//    override fun isMobileDataEnabled(): Boolean =
//        try {
//            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//
//            val networks = cm.allNetworks
//
//            val hasCellular = networks.any { network ->
//                val caps = cm.getNetworkCapabilities(network)
//                val isCellularAvailable = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
//                val isDataCapable = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ?: false
//                isCellularAvailable && isDataCapable
//            }
//            val oldWay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                telephonyManager.isDataEnabled
//            } else true
//            Timber.d("Mobile data enabled: $hasCellular vs $oldWay")
//            hasCellular
//
//        } catch (e: Exception) {
//            if (e is CancellationException) {
//                throw e
//            }
//            false
//        }

    override fun isMobileDataEnabled(): Boolean {
        val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId()
        try{
            return if (defaultDataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                val subSpecificTelephonyManager = telephonyManager.createForSubscriptionId(defaultDataSubId)
                val isDataEnabled = subSpecificTelephonyManager.isDataEnabled
                Timber.d("Mobile data enabled: $isDataEnabled")
                isDataEnabled
            } else {
                Timber.e("Mobile data not enabled - invalid subscription ID")
                false
            }
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            Timber.e("Mobile data not enabled - ${e.message}")
            false
        }
        Timber.e("Mobile data not enabled - fallback")
        return false
    }


    // -------------------- IP ADDRESS CHANGED --------------------

    private fun ipAddressFlow(context: Context) = callbackFlow<String?> {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var lastIp: String? = null

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(checkIp())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(checkIp())
            }

            private fun checkIp(): String? {
                val ip = getCurrentIpAddress()
                if (ip != lastIp) {
                    lastIp = ip
                    return ip
                }
                return null
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        awaitClose { connectivityManager.unregisterNetworkCallback(networkCallback) }
    }.distinctUntilChanged()

    override fun getCurrentIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()
                .toList()
                .asSequence()
                .flatMap { it.inetAddresses.toList().asSequence() }
                .filter { !it.isLoopbackAddress }
                .mapNotNull { address ->
                    when (address) {
                        is Inet4Address -> address.hostAddress
                        is Inet6Address -> address.hostAddress
                            ?.substringBefore('%')

                        else -> null
                    }
                }
                .firstOrNull()
        } catch (e: Exception) {
            if (e is CancellationException) {
                throw e
            }
            null
        }
    }

    // -------------------- PUBLIC START / STOP --------------------

    override fun start(
        onAirplaneEnabled: () -> Unit,
        onAirplaneDisabled: () -> Unit,
        onMobileDataEnabled: () -> Unit,
        onMobileDataDisabled: () -> Unit,
        onIpAddressChanged: (ipAddress: String?) -> Unit
    ) {
        if (monitorJob != null) return

        monitorJob = scope.launch {

            merge(
                airplaneModeFlow()
                    .onEach { Timber.d("Airplane mode changed to: $it") }
                    .drop(1)
                    .debounce { 1_000 }
                    .onEach {
                        if (it) onAirplaneEnabled() else onAirplaneDisabled()
                    },

                mobileDataEnabledFlow()
                    .onEach { Timber.d("mobile data enabled changed to: $it") }
                    .drop(1)
                    .debounce { 1_000 }
                    .onEach {
                        if (it) onMobileDataEnabled() else onMobileDataDisabled()
                    },

                ipAddressFlow(context)
                    .onEach { Timber.d("IP changed to: $it") }
                    .drop(1)
                    .debounce { 1_000 }
                    .onEach {
                        onIpAddressChanged(it)
                    }

            ).collect()
        }
    }

    override fun stop() {
        monitorJob?.cancel()
        monitorJob = null
    }
}
