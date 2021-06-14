/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.specure.info.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import at.rmbt.client.control.getCorrectDataTelephonyManager
import at.rmbt.client.control.getCurrentDataSubscriptionId
import at.specure.info.TransportType
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.connectivity.ConnectivityInfo
import at.specure.info.connectivity.ConnectivityWatcher
import at.specure.info.ip.CaptivePortal
import at.specure.info.wifi.WifiInfoWatcher
import at.specure.location.LocationState
import at.specure.location.LocationStateWatcher
import at.specure.util.*
import cz.mroczis.netmonster.core.INetMonster
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.feature.merge.CellSource
import cz.mroczis.netmonster.core.model.cell.ICell
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

private const val CELL_UPDATE_DELAY = 1000L
/**
 * Active network watcher that is aggregates all network watchers
 * to detect which one is currently active and get its data
 */
class ActiveNetworkWatcher(
    private val context: Context,
    private val netMonster: INetMonster,
    private val subscriptionManager: SubscriptionManager,
    private val telephonyManager: TelephonyManager,
    private val connectivityManager: ConnectivityManager,
    private val connectivityWatcher: ConnectivityWatcher,
    private val wifiInfoWatcher: WifiInfoWatcher,
    private val locationStateWatcher: LocationStateWatcher,
    private val captivePortal: CaptivePortal
) : LocationStateWatcher.Listener {

    private val listeners = Collections.synchronizedSet(mutableSetOf<NetworkChangeListener>())
    private var isCallbacksRegistered = false

    private var lastConnectivityInfo: ConnectivityInfo? = null

    private val handler = Looper.myLooper()?.let { Handler(it) }

    private val signalUpdateRunnable = Runnable {
        updateCellNetworkInfo()
    }

    private var _currentNetworkInfo: NetworkInfo? = null
        set(value) {
            field = value
            listeners.synchronizedForEach {
                it.onActiveNetworkChanged(value)
            }
        }

    init {
        locationStateWatcher.addListener(this)
    }

    /**
     * Returns active network information [NetworkInfo] if it is available
     */
    val currentNetworkInfo: NetworkInfo?
        get() = _currentNetworkInfo

    private val connectivityCallback = object : ConnectivityWatcher.ConnectivityChangeListener {

        override fun onConnectivityChanged(connectivityInfo: ConnectivityInfo?, network: Network?) {
            lastConnectivityInfo = connectivityInfo
            handler?.removeCallbacks(signalUpdateRunnable)
            _currentNetworkInfo = if (connectivityInfo == null) {
                null
            } else {
                when (connectivityInfo.transportType) {
                    TransportType.ETHERNET -> {
                        EthernetNetworkInfo(connectivityInfo.linkDownstreamBandwidthKbps, connectivityInfo.netId, null)
                    }
                    TransportType.WIFI -> wifiInfoWatcher.activeWifiInfo.apply {
                        this?.locationEnabled = locationStateWatcher.state == LocationState.ENABLED
                    }
                    TransportType.CELLULAR -> {
                        updateCellNetworkInfo()
                    }
                    else -> null
                }
            }
            GlobalScope.launch {
                captivePortal.resetCaptivePortalStatus()
                captivePortal.checkForCaptivePortal()
            }
        }
    }

    fun updateCellNetworkInfo(): CellNetworkInfo? {

        if (context.isCoarseLocationPermitted() && context.isReadPhoneStatePermitted()) {
            try {
                var cells: List<ICell>? = null
                var activeCellNetwork: CellNetworkInfo? = null
                cells = netMonster.getCells(CellSource.NEIGHBOURING_CELLS, CellSource.ALL_CELL_INFO, CellSource.CELL_LOCATION)

                val dataSubscriptionId = subscriptionManager.getCurrentDataSubscriptionId()

                val primaryCells = cells?.filterOnlyPrimaryActiveDataCell(dataSubscriptionId)

                if (primaryCells.size == 1) {
                    activeCellNetwork = primaryCells[0].toCellNetworkInfo(
                        connectivityManager.activeNetworkInfo?.extraInfo,
                        telephonyManager.getCorrectDataTelephonyManager(subscriptionManager),
                        NetMonsterFactory.getTelephony(context, primaryCells[0].subscriptionId),
                        netMonster
                    )
                    // more than one primary cell for data subscription
                } else {
                    Timber.e("NM network type unable to detect because of more than 1 primary cells for subscription")
                }

                if (activeCellNetwork == null) {
                    scheduleUpdate()
                }
                return activeCellNetwork
            } catch (e: SecurityException) {
                Timber.e("SecurityException: Not able to read telephonyManager.allCellInfo")
            } catch (e: IllegalStateException) {
                Timber.e("IllegalStateException: Not able to read telephonyManager.allCellInfo")
            } catch (e: NullPointerException) {
                Timber.e("NullPointerException: Not able to read telephonyManager.allCellInfo from other reason")
            }
        }
        scheduleUpdate()
        return null
    }

    private fun scheduleUpdate() {
        handler?.removeCallbacks(signalUpdateRunnable)
        handler?.postDelayed(signalUpdateRunnable, CELL_UPDATE_DELAY)
    }

    /**
     * Add callback to start receiving active network changes
     */
    fun addListener(listener: NetworkChangeListener) {
        listeners.add(listener)
        listener.onActiveNetworkChanged(currentNetworkInfo)
        if (listeners.size == 1) {
            registerCallbacks()
        }
    }

    /**
     * Remove callback from receiving updates of active network changes
     */
    fun removeListener(listener: NetworkChangeListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            unregisterCallbacks()
        }
    }

    private fun registerCallbacks() {
        if (!isCallbacksRegistered) {
            connectivityWatcher.addListener(connectivityCallback)
            isCallbacksRegistered = true
        }
    }

    private fun unregisterCallbacks() {
        if (isCallbacksRegistered) {
            connectivityWatcher.removeListener(connectivityCallback)
            isCallbacksRegistered = false
        }
    }

    /**
     * Callback that is used to observe active network change tracked by [ActiveNetworkWatcher]
     */
    interface NetworkChangeListener {

        /**
         * When active network change is detected this callback will be triggered
         * if no active network is available null will be returned
         */
        fun onActiveNetworkChanged(networkInfo: NetworkInfo?)
    }

    override fun onLocationStateChanged(state: LocationState?) {
        val enabled = state == LocationState.ENABLED
        if (listeners.isNotEmpty()) {
            unregisterCallbacks()
            registerCallbacks()
        }
        if (_currentNetworkInfo is WifiNetworkInfo) {
            listeners.forEach {
                it.onActiveNetworkChanged(
                    wifiInfoWatcher.activeWifiInfo?.apply { locationEnabled = enabled }
                )
            }
        }
    }
}