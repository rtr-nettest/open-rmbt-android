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
import at.specure.util.filterOnlyPrimaryActiveDataCell
import at.specure.util.isDualSim
import at.specure.util.isFineLocationPermitted
import at.specure.util.isLocationServiceEnabled
import at.specure.util.isReadPhoneStatePermitted
import at.specure.util.mobileNetworkType
import at.specure.util.synchronizedForEach
import at.specure.util.toCellNetworkInfo
import at.specure.util.toSignalStrengthInfo
import cz.mroczis.netmonster.core.INetMonster
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.model.cell.CellLte
import cz.mroczis.netmonster.core.model.cell.CellNr
import cz.mroczis.netmonster.core.model.cell.ICell
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Collections

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
            Timber.d("NIFU: \n\n $connectivityInfo \n\n $network")
            handler?.removeCallbacks(signalUpdateRunnable)
            _currentNetworkInfo = if (connectivityInfo == null) {
                null
            } else {
                when (connectivityInfo.transportType) {
                    TransportType.ETHERNET -> {
                        EthernetNetworkInfo(connectivityInfo.linkDownstreamBandwidthKbps, connectivityInfo.netId, null, connectivityInfo.capabilitiesRaw.toString())
                    }
                    TransportType.WIFI -> wifiInfoWatcher.activeWifiInfo.apply {
                        this?.locationEnabled = locationStateWatcher.state == LocationState.ENABLED
                    }
                    TransportType.CELLULAR -> {
                        updateCellNetworkInfo()
                    }
                    TransportType.BLUETOOTH -> {
                        BluetoothNetworkInfo(connectivityInfo.linkDownstreamBandwidthKbps, connectivityInfo.netId, null, connectivityInfo.capabilitiesRaw.toString())
                    }
                    TransportType.VPN -> {
                        VpnNetworkInfo(connectivityInfo.linkDownstreamBandwidthKbps, connectivityInfo.netId, null, connectivityInfo.capabilitiesRaw.toString())
                    }
                    else -> {
                        Timber.d("NIFU creating OtherNetworkInfo: \n\n $connectivityInfo")
                        OtherNetworkInfo(connectivityInfo.linkDownstreamBandwidthKbps, connectivityInfo.netId, null, connectivityInfo.capabilitiesRaw.toString())
                    }
                }
            }
            GlobalScope.launch {
                captivePortal.resetCaptivePortalStatus()
                captivePortal.checkForCaptivePortal()
            }
        }
    }

    fun updateCellNetworkInfo(): CellNetworkInfo? {

        if (context.isLocationServiceEnabled() &&  context.isFineLocationPermitted() && context.isReadPhoneStatePermitted()) {
            try {
                var cells: List<ICell>? = null
                var activeCellNetwork: CellNetworkInfo? = null
                cells = netMonster.getCells()

                // all of the methods above returns 2 on samsung dual sim no matter of how many sims are activated or inserted so only this is viable but it can return 1 when signal is lost
                Timber.d("subscription manager - active subscriptions count: ${subscriptionManager.activeSubscriptionInfoCount}")

                val simCount = subscriptionManager.activeSubscriptionInfoCount

                val dataSubscriptionId = subscriptionManager.getCurrentDataSubscriptionId()

                val primaryCells = cells?.filterOnlyPrimaryActiveDataCell(dataSubscriptionId)

                // changed to check if it is not empty because some devices report more than one
                // PrimaryConnection cells but with same operator details, only with different
                // technology e.g. in case of 5G NSA there is 5G and 4G cell for some devices/networks, but
                // in some cases for 5G NSA there are 2 primary connections reported - 1. for 4G and 2. for 5G cell,
                // so we need to move that 5G from primary connection to secondary list
                var primaryCellsCorrected = mutableListOf<ICell>()
                when (primaryCells?.size) {
                    2 -> {
                        if (primaryCells[0] is CellNr && (primaryCells[0].mobileNetworkType(
                                netMonster
                            ) == MobileNetworkType.NR_SA)) {
                            primaryCellsCorrected.add(primaryCells[0])
                        } else if (primaryCells[1] is CellNr && (primaryCells[1].mobileNetworkType(
                                netMonster
                            ) == MobileNetworkType.NR_SA)) {
                            primaryCellsCorrected.add(primaryCells[1])
                        } else if (primaryCells[0] is CellNr && primaryCells[0].mobileNetworkType(
                                netMonster
                            ) == MobileNetworkType.NR_NSA && primaryCells[1] is CellLte
                        ) {
                            primaryCellsCorrected.add(primaryCells[1])
                        } else if (primaryCells[1] is CellNr && primaryCells[1].mobileNetworkType(
                                netMonster
                            ) == MobileNetworkType.NR_NSA && primaryCells[0] is CellLte
                        ) {
                            primaryCellsCorrected.add(primaryCells[0])
                        } else {
                            // situation when there are 2 primary cells sent from netmonster library so we must choose one
                                val signalSecondPrimaryCell = primaryCells[1].signal?.toSignalStrengthInfo(0)?.value
                                val signalFirstPrimaryCell = primaryCells[0].signal?.toSignalStrengthInfo(0)?.value
                                if (signalSecondPrimaryCell != null && signalFirstPrimaryCell != null && (signalSecondPrimaryCell > signalFirstPrimaryCell)) {
                                    primaryCellsCorrected.add(primaryCells[1])
                                } else {
                                    primaryCellsCorrected.add(primaryCells[0])
                                }
                        }
                    }
                    else -> {
                        primaryCellsCorrected = primaryCells as MutableList<ICell>
                    }
                }

                if (primaryCellsCorrected.isNotEmpty()) {
                    activeCellNetwork = primaryCellsCorrected[0].toCellNetworkInfo(
                        connectivityManager.activeNetworkInfo?.extraInfo,
                        telephonyManager.getCorrectDataTelephonyManager(subscriptionManager),
                        NetMonsterFactory.getTelephony(
                            context,
                            primaryCellsCorrected[0].subscriptionId
                        ),
                        primaryCellsCorrected[0].mobileNetworkType(netMonster),
                        dataSubscriptionId,
                        simCount
                    )
                    // more than one primary cell for data subscription
                } else {
                    Timber.e("NM network type unable to detect because of ${primaryCellsCorrected.size} primary cells for subscription")
                    activeCellNetwork = CellNetworkInfo("")
                    scheduleUpdate()
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
        // when we are not able to detect more than there is cellular connection (we have
        // no permission granted to read more details or permissions are granted but location is off)
        scheduleUpdate()
        return CellNetworkInfo(cellUUID = "")
    }

    private fun scheduleUpdate() {
        Timber.d("Scheduling mobile cell update")
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