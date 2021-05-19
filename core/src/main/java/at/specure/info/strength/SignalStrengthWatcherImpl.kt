/*
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

package at.specure.info.strength

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import at.rmbt.client.control.getCurrentDataSubscriptionId
import at.rmbt.client.control.getTelephonyManagerForSubscription
import at.rmbt.util.io
import at.specure.info.TransportType
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.ActiveNetworkWatcher
import at.specure.info.network.DetailedNetworkInfo
import at.specure.info.network.NetworkInfo
import at.specure.info.wifi.WifiInfoWatcher
import at.specure.util.filterOnlyActiveDataCell
import at.specure.util.isCoarseLocationPermitted
import at.specure.util.isReadPhoneStatePermitted
import at.specure.util.permission.LocationAccess
import at.specure.util.synchronizedForEach
import at.specure.util.toCellNetworkInfo
import at.specure.util.toSignalStrengthInfo
import cz.mroczis.netmonster.core.INetMonster
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.model.cell.ICell
import timber.log.Timber
import java.util.Collections

private const val WIFI_UPDATE_DELAY = 2000L
private const val CELL_UPDATE_DELAY = 1000L
private const val WIFI_MESSAGE_ID = 1

/**
 * Basic implementation of [SignalStrengthInfo] that using [ActiveNetworkWatcher] and [WifiInfoWatcher] to detect network changes and handle
 * signal strength changes of current network available on the mobile device
 */
class SignalStrengthWatcherImpl(
    private val context: Context,
    private val netmonster: INetMonster,
    private val subscriptionManager: SubscriptionManager,
    private val telephonyManager: TelephonyManager,
    private val activeNetworkWatcher: ActiveNetworkWatcher,
    private val wifiInfoWatcher: WifiInfoWatcher,
    locationAccess: LocationAccess
) : SignalStrengthWatcher, LocationAccess.LocationAccessChangeListener {

    private val listeners = Collections.synchronizedSet(mutableSetOf<SignalStrengthWatcher.SignalStrengthListener>())

    private val handler = Looper.myLooper()?.let { Handler(it) }

    private val signalUpdateRunnable = Runnable {
        processSignalChange()
        scheduleUpdate()
    }

    private var wifiListenerRegistered = false

    private var signalStrengthInfo: SignalStrengthInfo? = null

    private var networkInfo: NetworkInfo? = null

    override val lastNetworkInfo: NetworkInfo?
        get() = networkInfo

    override val lastSignalStrength: SignalStrengthInfo?
        get() = signalStrengthInfo

    init {
        locationAccess.addListener(this)
    }

    private val strengthListener = object : PhoneStateListener() {

        // discard signal strength from GT-I9100G (Galaxy S II) - passes wrong info
        private val ignoredDevices = setOf("GT-I9100G", "HUAWEI P2-6011")

        private val isDeviceIgnored: Boolean
            get() = ignoredDevices.contains(Build.MODEL)

        @SuppressLint("MissingPermission")
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            when (activeNetworkWatcher.currentNetworkInfo?.type) {
                TransportType.CELLULAR,
                TransportType.WIFI -> processSignalChange()
                else -> {
                    // no signal information to process
                }
            }
        }
    }

    private fun scheduleUpdate() {
        handler?.removeCallbacks(signalUpdateRunnable)
        handler?.postDelayed(signalUpdateRunnable, CELL_UPDATE_DELAY)
    }

    private fun processSignalChange() = io {
        var cells: List<ICell>? = null
        if (context.isCoarseLocationPermitted() && context.isReadPhoneStatePermitted()) {
            try {
                cells = netmonster.getCells()

                val timeNanos = System.nanoTime()
                val dataSubscriptionId = subscriptionManager.getCurrentDataSubscriptionId()

                val primaryCells = cells?.filterOnlyActiveDataCell(dataSubscriptionId)

                primaryCells?.toList()?.let {
                    it.forEach { iCell ->
                        Timber.d("NM network type ${netmonster.getNetworkType(iCell.subscriptionId)} from subscription: $dataSubscriptionId")
                        signalStrengthInfo = iCell.toSignalStrengthInfo(timeNanos)
                        networkInfo = iCell.toCellNetworkInfo(
                            activeNetworkWatcher.currentNetworkInfo,
                            telephonyManager.getTelephonyManagerForSubscription(iCell.subscriptionId),
                            NetMonsterFactory.getTelephony(context, iCell.subscriptionId),
                            netmonster
                        )
                    }
                }
                if (networkInfo is CellNetworkInfo) {
                    Timber.d(
                        "NM network type Primary cells: ${primaryCells.size}   Network type: ${(networkInfo as CellNetworkInfo).networkType.displayName}"
                    )
                }
                notifyInfoChanged()
            } catch (e: SecurityException) {
                Timber.e("SecurityException: Not able to read telephonyManager.allCellInfo")
            } catch (e: IllegalStateException) {
                Timber.e("IllegalStateException: Not able to read telephonyManager.allCellInfo")
            } catch (e: NullPointerException) {
                Timber.e("NullPointerException: Not able to read telephonyManager.allCellInfo from other reason")
            }
        }
    }

    private val activeNetworkListener = object : ActiveNetworkWatcher.NetworkChangeListener {

        override fun onActiveNetworkChanged(detailedNetworkInfo: DetailedNetworkInfo) {
            if (detailedNetworkInfo.networkInfo == null) {
                unregisterWifiCallbacks()
                unregisterCellCallbacks()

                Timber.i("Network changed to NULL")
                signalStrengthInfo = null
                networkInfo = null
                notifyInfoChanged()
                return
            }

            if (detailedNetworkInfo.networkInfo.type == TransportType.CELLULAR) {
                registerCellCallbacks()
            }

            if (detailedNetworkInfo.networkInfo.type == TransportType.WIFI) {
                registerWifiCallbacks()
            }

            if (detailedNetworkInfo.networkInfo.type == TransportType.ETHERNET) {
                unregisterWifiCallbacks()
                unregisterCellCallbacks()
                signalStrengthInfo = null
                networkInfo = detailedNetworkInfo.networkInfo
                notifyInfoChanged()
            }
        }
    }

    private val wifiUpdateHandler = Handler {
        handleWifiUpdate()
        return@Handler true
    }

    private fun handleWifiUpdate() {
        val wifiInfo = wifiInfoWatcher.activeWifiInfo
        if (wifiInfo != null) {
            signalStrengthInfo = SignalStrengthInfo.from(wifiInfo)
            networkInfo = wifiInfo
        }
        notifyInfoChanged()
        scheduleWifiUpdate()
    }

    private fun scheduleWifiUpdate() {
        wifiUpdateHandler.removeMessages(WIFI_MESSAGE_ID)
        if (wifiListenerRegistered) {
            wifiUpdateHandler.sendEmptyMessageDelayed(WIFI_MESSAGE_ID, WIFI_UPDATE_DELAY)
        }
    }

    private fun notifyInfoChanged() {
        listeners.synchronizedForEach { it.onSignalStrengthChanged(DetailedNetworkInfo(networkInfo, signalStrengthInfo, null)) }
    }

    override fun addListener(listener: SignalStrengthWatcher.SignalStrengthListener) {
        listeners.add(listener)
        listener.onSignalStrengthChanged(DetailedNetworkInfo(networkInfo, signalStrengthInfo, null))
        if (listeners.size == 1) {
            registerCallbacks()
        }
    }

    override fun removeListener(listener: SignalStrengthWatcher.SignalStrengthListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            unregisterCallbacks()
        }
    }

    private fun registerCallbacks() {
        activeNetworkWatcher.addListener(activeNetworkListener)
    }

    private fun unregisterCallbacks() {
        activeNetworkWatcher.removeListener(activeNetworkListener)
        unregisterCellCallbacks()
        unregisterWifiCallbacks()
    }

    private fun registerCellCallbacks() {
        Timber.i("Network changed to CELLULAR")
        handler?.postDelayed(signalUpdateRunnable, CELL_UPDATE_DELAY)
        unregisterWifiCallbacks()
    }

    private fun registerWifiCallbacks() {
        Timber.i("Network changed to WIFI")
        if (!wifiListenerRegistered) {
            wifiListenerRegistered = true
            handleWifiUpdate()
        }
        unregisterCellCallbacks()
    }

    private fun unregisterCellCallbacks() {
        handler?.removeCallbacks(signalUpdateRunnable)
    }

    private fun unregisterWifiCallbacks() {
        if (wifiListenerRegistered) {
            wifiUpdateHandler.removeMessages(WIFI_MESSAGE_ID)
            wifiListenerRegistered = false
        }
    }

    override fun onLocationAccessChanged(isAllowed: Boolean) {
        if (listeners.isNotEmpty() && isAllowed) {
            unregisterCallbacks()
            registerCallbacks()
        }
    }
}