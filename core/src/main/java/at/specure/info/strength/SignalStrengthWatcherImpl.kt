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
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthTdscdma
import android.telephony.CellSignalStrengthWcdma
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import at.specure.info.TransportType
import at.specure.info.cell.CellInfoWatcher
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.ActiveNetworkWatcher
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NetworkInfo
import at.specure.info.wifi.WifiInfoWatcher
import at.specure.util.synchronizedForEach
import timber.log.Timber
import java.util.Collections

private const val WIFI_UPDATE_DELAY = 2000L
private const val WIFI_MESSAGE_ID = 1
private const val WIFI_MIN_SIGNAL_VALUE = -100
private const val WIFI_MAX_SIGNAL_VALUE = -30

private const val CELLULAR_SIGNAL_MIN = -110
private const val CELLULAR_SIGNAL_MAX = -50

private const val LTE_RSRP_SIGNAL_MIN = -130
private const val LTE_RSRP_SIGNAL_MAX = -70

private const val WCDMA_RSRP_SIGNAL_MIN = -120
private const val WCDMA_RSRP_SIGNAL_MAX = -24

private const val NR_RSRP_SIGNAL_MIN = -140
private const val NR_RSRP_SIGNAL_MAX = -44

/**
 * Basic implementation of [SignalStrengthInfo] that using [ActiveNetworkWatcher] and [WifiInfoWatcher] to detect network changes and handle
 * signal strength changes of current network available on the mobile device
 */
class SignalStrengthWatcherImpl(
    private val telephonyManager: TelephonyManager,
    private val activeNetworkWatcher: ActiveNetworkWatcher,
    private val wifiInfoWatcher: WifiInfoWatcher,
    private val cellInfoWatcher: CellInfoWatcher
) :
    SignalStrengthWatcher {

    private val listeners = Collections.synchronizedSet(mutableSetOf<SignalStrengthWatcher.SignalStrengthListener>())

    private var cellListenerRegistered = false
    private var wifiListenerRegistered = false

    private var signalStrengthInfo: SignalStrengthInfo? = null

    override val lastSignalStrength: SignalStrengthInfo?
        get() = signalStrengthInfo

    private val strengthListener = object : PhoneStateListener() {

        // discard signal strength from GT-I9100G (Galaxy S II) - passes wrong info
        private val ignoredDevices = setOf("GT-I9100G", "HUAWEI P2-6011")

        private val isDeviceIgnored: Boolean
            get() = ignoredDevices.contains(Build.MODEL)

        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            Timber.d("Signal Strength changed")

            if (isDeviceIgnored) {
                Timber.i("Signal Strength is ignored for current device")
                return
            }

            val signal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                signalStrengthQ(signalStrength)
            } else {
                signalStrengthOld(signalStrength)
            }

            if (signal?.value != null && signal.value != 0) {
                signalStrengthInfo = signal
                notifyInfoChanged()
            }
        }
    }

    @SuppressLint("NewApi")
    private fun signalStrengthQ(signalStrength: SignalStrength?): SignalStrengthInfo? {
        if (signalStrength == null) {
            return null
        }
        var signal: SignalStrengthInfo? = null

        signalStrength.cellSignalStrengths.forEach {
            when (it) {
                is CellSignalStrengthLte -> {
                    signal = SignalStrengthInfoLte(
                        transport = TransportType.CELLULAR,
                        value = it.dbm,
                        rsrq = it.rsrq,
                        signalLevel = it.level,
                        min = LTE_RSRP_SIGNAL_MIN,
                        max = LTE_RSRP_SIGNAL_MAX,
                        cqi = it.cqi,
                        rsrp = it.rsrp,
                        rssi = it.rssi,
                        rssnr = it.rssnr,
                        timingAdvance = it.timingAdvance
                    )
                }
                is CellSignalStrengthNr -> {
                    signal = SignalStrengthInfoNr(
                        transport = TransportType.CELLULAR,
                        value = it.dbm,
                        rsrq = it.csiRsrq,
                        signalLevel = it.level,
                        min = NR_RSRP_SIGNAL_MIN,
                        max = NR_RSRP_SIGNAL_MAX,
                        csiRsrp = it.csiRsrp,
                        csiRsrq = it.csiRsrq,
                        csiSinr = it.csiSinr,
                        ssRsrp = it.ssRsrp,
                        ssRsrq = it.ssRsrq,
                        ssSinr = it.ssSinr
                    )
                }
                is CellSignalStrengthTdscdma,
                is CellSignalStrengthWcdma -> {
                    signal = SignalStrengthInfo(
                        transport = TransportType.CELLULAR,
                        value = it.dbm,
                        rsrq = null,
                        signalLevel = it.level,
                        min = WCDMA_RSRP_SIGNAL_MIN,
                        max = WCDMA_RSRP_SIGNAL_MAX
                    )
                }
                is CellSignalStrengthGsm -> {
                    signal = SignalStrengthInfoGsm(
                        transport = TransportType.CELLULAR,
                        value = it.dbm,
                        rsrq = null,
                        signalLevel = it.level,
                        min = CELLULAR_SIGNAL_MIN,
                        max = CELLULAR_SIGNAL_MAX,
                        bitErrorRate = it.bitErrorRate,
                        timingAdvance = it.timingAdvance
                    )
                }
                else -> {
                    signal = SignalStrengthInfo(
                        transport = TransportType.CELLULAR,
                        value = it.dbm,
                        rsrq = null,
                        signalLevel = it.level,
                        min = CELLULAR_SIGNAL_MIN,
                        max = CELLULAR_SIGNAL_MAX
                    )
                }
            }
        }

        return signal
    }

    private fun signalStrengthOld(signalStrength: SignalStrength?): SignalStrengthInfo? {
        val network = activeNetworkWatcher.currentNetworkInfo
        val cellInfo = cellInfoWatcher.cellInfo
        var strength: Int? = null
        var lteRsrp: Int? = null
        var lteRsrq: Int? = null
        var lteRssnr: Int? = null
        var lteCqi: Int? = null
        var errorRate: Int? = null

        if (network is CellNetworkInfo && signalStrength != null) {
            val type = network.networkType
            if (type == MobileNetworkType.CDMA) {
                strength = signalStrength.cdmaDbm
            } else if (type == MobileNetworkType.EVDO_0 || type == MobileNetworkType.EVDO_A || type == MobileNetworkType.EVDO_B) {
                strength = signalStrength.evdoDbm
            } else if (type == MobileNetworkType.LTE || type == MobileNetworkType.LTE_CA) {
                try {
                    lteRsrp = SignalStrength::class.java.getMethod("getLteRsrp").invoke(signalStrength) as Int
                    lteRsrq = SignalStrength::class.java.getMethod("getLteRsrq").invoke(signalStrength) as Int
                    lteRssnr = SignalStrength::class.java.getMethod("getLteRssnr").invoke(signalStrength) as Int
                    lteCqi = SignalStrength::class.java.getMethod("getLteCqi").invoke(signalStrength) as Int

                    if (lteRsrp == Integer.MAX_VALUE)
                        lteRsrp = null
                    if (lteRsrq == Integer.MAX_VALUE)
                        lteRsrq = null
                    if (lteRsrq != null && lteRsrq > 0)
                        lteRsrq = -lteRsrq // fix invalid rsrq values for some devices (see #996)
                    if (lteRssnr == Integer.MAX_VALUE) {
                        lteRssnr = null
                    }
                    if (lteCqi == Integer.MAX_VALUE) {
                        lteCqi = null
                    }
                } catch (t: Throwable) {
                    Timber.e(t)
                }
            } else if (signalStrength.isGsm) {
                try {
                    val getGsmDbm = SignalStrength::class.java.getMethod("getGsmDbm")
                    val result = getGsmDbm.invoke(signalStrength) as Int
                    if (result != -1)
                        strength = result
                } catch (t: Throwable) {
                    Timber.e(t)
                }

                if (strength == null) { // fallback if not implemented
                    val dBm: Int?
                    val gsmSignalStrength = signalStrength.gsmSignalStrength
                    val asu = if (gsmSignalStrength == 99) -1 else gsmSignalStrength
                    dBm = if (asu != -1) {
                        -113 + 2 * asu
                    } else {
                        null
                    }
                    strength = dBm
                }
            }
        }

        val signalValue = lteRsrp ?: strength
        val signalMin = if (lteRsrp == null) CELLULAR_SIGNAL_MIN else LTE_RSRP_SIGNAL_MIN
        val signalMax = if (lteRsrp == null) CELLULAR_SIGNAL_MAX else LTE_RSRP_SIGNAL_MAX

        var signal: SignalStrengthInfo? = null

        if (signalValue == null) {
            return null
        }

        when (cellInfo) {
            null -> {
                signal = null
            }
            is CellInfoLte -> {
                signal = SignalStrengthInfoLte(
                    transport = TransportType.CELLULAR,
                    value = signalValue,
                    rsrq = lteRsrq,
                    signalLevel = calculateCellSignalLevel(signalValue, signalMin, signalMax),
                    min = LTE_RSRP_SIGNAL_MIN,
                    max = LTE_RSRP_SIGNAL_MAX,
                    cqi = lteCqi ?: 0,
                    rsrp = lteRsrp ?: 0,
                    rssi = 0,
                    rssnr = lteRssnr ?: 0,
                    timingAdvance = cellInfo.cellSignalStrength.timingAdvance
                )
            }
            is CellInfoWcdma -> {
                signal = SignalStrengthInfo(
                    transport = TransportType.CELLULAR,
                    value = signalValue,
                    rsrq = null,
                    signalLevel = calculateCellSignalLevel(signalValue, signalMin, signalMax),
                    min = WCDMA_RSRP_SIGNAL_MIN,
                    max = WCDMA_RSRP_SIGNAL_MAX
                )
            }
            is CellInfoGsm -> {
                signal = SignalStrengthInfoGsm(
                    transport = TransportType.CELLULAR,
                    value = signalValue,
                    rsrq = null,
                    signalLevel = calculateCellSignalLevel(signalValue, signalMin, signalMax),
                    min = CELLULAR_SIGNAL_MIN,
                    max = CELLULAR_SIGNAL_MAX,
                    bitErrorRate = signalStrength?.gsmBitErrorRate ?: 0,
                    timingAdvance = 0
                )
            }
            else -> {
                SignalStrengthInfo(
                    transport = TransportType.CELLULAR,
                    value = signalValue,
                    rsrq = lteRsrq,
                    signalLevel = calculateCellSignalLevel(signalValue, signalMin, signalMax),
                    max = signalMax,
                    min = signalMin
                )
            }
        }

        return signal
    }

    private fun calculateCellSignalLevel(signal: Int?, min: Int, max: Int): Int {
        val relativeSignal: Double = ((signal ?: 0) - min.toDouble()) / (max - min)
        return when {
            relativeSignal <= 0.0 -> 0
            relativeSignal < 0.25 -> 1
            relativeSignal < 0.5 -> 2
            relativeSignal < 0.75 -> 3
            else -> 4
        }
    }

    private val activeNetworkListener = object : ActiveNetworkWatcher.NetworkChangeListener {

        override fun onActiveNetworkChanged(info: NetworkInfo?) {
            if (info == null) {
                unregisterWifiCallbacks()
                unregisterCellCallbacks()

                Timber.i("Network changed to NULL")
                signalStrengthInfo = null
                notifyInfoChanged()

                return
            }

            if (info.type == TransportType.CELLULAR) {
                registerCellCallbacks()
            }

            if (info.type == TransportType.WIFI) {
                registerWifiCallbacks()
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
            signalStrengthInfo = SignalStrengthInfoWiFi(
                transport = TransportType.WIFI,
                value = wifiInfo.rssi,
                rsrq = null,
                signalLevel = wifiInfo.signalLevel,
                max = WIFI_MAX_SIGNAL_VALUE,
                min = WIFI_MIN_SIGNAL_VALUE,
                linkSpeed = wifiInfo.linkSpeed
            )
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
        listeners.synchronizedForEach { it.onSignalStrengthChanged(signalStrengthInfo) }
    }

    override fun addListener(listener: SignalStrengthWatcher.SignalStrengthListener) {
        listeners.add(listener)
        listener.onSignalStrengthChanged(lastSignalStrength)
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
        if (!cellListenerRegistered) {
            telephonyManager.listen(strengthListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
            cellListenerRegistered = true
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
            telephonyManager.listen(strengthListener, PhoneStateListener.LISTEN_NONE)
            cellListenerRegistered = false
        }
    }

    private fun unregisterWifiCallbacks() {
        if (wifiListenerRegistered) {
            wifiUpdateHandler.removeMessages(WIFI_MESSAGE_ID)
            wifiListenerRegistered = false
        }
    }
}