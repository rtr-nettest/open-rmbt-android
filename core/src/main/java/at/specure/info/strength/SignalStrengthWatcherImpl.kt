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
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import at.specure.info.TransportType
import at.specure.info.cell.CellInfoWatcher
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.ActiveNetworkWatcher
import at.specure.info.network.DetailedNetworkInfo
import at.specure.info.network.NetworkInfo
import at.specure.info.wifi.WifiInfoWatcher
import at.specure.util.permission.LocationAccess
import at.specure.util.synchronizedForEach
import cz.mroczis.netmonster.core.model.cell.ICell
import timber.log.Timber
import java.util.Collections

private const val WIFI_UPDATE_DELAY = 2000L
private const val CELL_UPDATE_DELAY = 1000L
private const val WIFI_MESSAGE_ID = 1
private const val CELL_MESSAGE_ID = 2

/**
 * Basic implementation of [SignalStrengthInfo] that using [ActiveNetworkWatcher] and [WifiInfoWatcher] to detect network changes and handle
 * signal strength changes of current network available on the mobile device
 */
class SignalStrengthWatcherImpl(
    private val activeNetworkWatcher: ActiveNetworkWatcher,
    private val wifiInfoWatcher: WifiInfoWatcher,
    private val cellInfoWatcher: CellInfoWatcher,
    locationAccess: LocationAccess
) : SignalStrengthWatcher, LocationAccess.LocationAccessChangeListener {

    private val listeners =
        Collections.synchronizedSet(mutableSetOf<SignalStrengthWatcher.SignalStrengthListener>())

    private val handler = Looper.myLooper()?.let { Handler(it) }

    private var wifiListenerRegistered = false
    private var cellListenerRegistered = false

    private var signalStrengthInfo: SignalStrengthInfo? = null
    private var secondaryActiveSignalStrengthInfo: List<SignalStrengthInfo?>? = null
    private var secondary5GActiveSignalStrengthInfo: List<SignalStrengthInfo?>? = null

    private var networkInfo: NetworkInfo? = null
    private var inactiveNetworkInfo: List<ICell?>? = null
    private var secondaryActiveNetworkInfo: List<CellNetworkInfo?>? = null
    private var secondary5GActiveNetworkInfo: List<CellNetworkInfo?>? = null

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
                TransportType.WIFI -> notifyInfoChanged()
                else -> {
                    // no signal information to process
                }
            }
        }
    }

    private val activeNetworkListener = object : ActiveNetworkWatcher.NetworkChangeListener {

        override fun onActiveNetworkChanged(newNetworkInfo: NetworkInfo?) {
            if (newNetworkInfo == null) {
                unregisterWifiCallbacks()
                unregisterCellCallbacks()

                Timber.i("Network changed to NULL")
                signalStrengthInfo = null
                networkInfo = null
                secondaryActiveSignalStrengthInfo = null
                secondary5GActiveSignalStrengthInfo = null
                secondaryActiveNetworkInfo = null
                inactiveNetworkInfo = null
                secondary5GActiveNetworkInfo = null
                notifyInfoChanged()
                return
            }

            if (newNetworkInfo.type == TransportType.CELLULAR) {
                registerCellCallbacks()
            }

            if (newNetworkInfo.type == TransportType.WIFI) {
                registerWifiCallbacks()
            }

            if (newNetworkInfo.type == TransportType.ETHERNET) {
                unregisterWifiCallbacks()
                unregisterCellCallbacks()
                signalStrengthInfo = null
                secondaryActiveSignalStrengthInfo = null
                secondary5GActiveSignalStrengthInfo = null
                secondaryActiveNetworkInfo = null
                inactiveNetworkInfo = null
                secondary5GActiveNetworkInfo = null
                networkInfo = newNetworkInfo
                notifyInfoChanged()
            }
        }
    }

    private val wifiUpdateHandler = Handler {
        handleWifiUpdate()
        return@Handler true
    }

    private val cellUpdateHandler = Handler {
        handleCellUpdate()
        return@Handler true
    }

    private fun handleWifiUpdate() {
        secondaryActiveSignalStrengthInfo = null
        secondary5GActiveSignalStrengthInfo = null
        secondaryActiveNetworkInfo = null
        inactiveNetworkInfo = null
        secondary5GActiveNetworkInfo = null
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

    private fun handleCellUpdate() {
        val cellInfo = cellInfoWatcher.activeNetwork
        signalStrengthInfo = cellInfoWatcher.signalStrengthInfo
        if (cellInfo != null) {
            networkInfo = cellInfo
        }
        secondaryActiveSignalStrengthInfo = cellInfoWatcher.secondaryActiveCellSignalStrengthInfos
        secondary5GActiveSignalStrengthInfo =
            cellInfoWatcher.secondary5GActiveCellSignalStrengthInfos
        secondaryActiveNetworkInfo = cellInfoWatcher.secondaryActiveCellNetworks
        inactiveNetworkInfo = cellInfoWatcher.allCellInfos
        secondary5GActiveNetworkInfo = cellInfoWatcher.secondary5GActiveCellNetworks
        notifyInfoChanged()
        scheduleCellUpdate()
    }

    private fun scheduleCellUpdate() {
        cellUpdateHandler.removeMessages(CELL_MESSAGE_ID)
        if (cellListenerRegistered) {
            cellUpdateHandler.sendEmptyMessageDelayed(CELL_MESSAGE_ID, CELL_UPDATE_DELAY)
            cellInfoWatcher.updateInfo()
        }
    }

    private fun notifyInfoChanged() {
        listeners.synchronizedForEach {
            it.onSignalStrengthChanged(
                DetailedNetworkInfo(
                    networkInfo,
                    signalStrengthInfo,
                    inactiveNetworkInfo,
                    secondaryActiveNetworkInfo,
                    secondaryActiveSignalStrengthInfo,
                    secondary5GActiveNetworkInfo,
                    secondary5GActiveSignalStrengthInfo
                )
            )
        }
    }

    override fun addListener(listener: SignalStrengthWatcher.SignalStrengthListener) {
        listeners.add(listener)
        listener.onSignalStrengthChanged(
            DetailedNetworkInfo(
                networkInfo,
                signalStrengthInfo,
                inactiveNetworkInfo,
                secondaryActiveNetworkInfo,
                secondaryActiveSignalStrengthInfo,
                secondary5GActiveNetworkInfo,
                secondary5GActiveSignalStrengthInfo
            )
        )
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
//        processSignalChange()
//        handler?.postDelayed(signalUpdateRunnable, CELL_UPDATE_DELAY)
        if (!cellListenerRegistered) {
            cellListenerRegistered = true
            handleCellUpdate()
        }
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
        if (cellListenerRegistered) {
            cellUpdateHandler.removeMessages(CELL_MESSAGE_ID)
            cellListenerRegistered = false
        }
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