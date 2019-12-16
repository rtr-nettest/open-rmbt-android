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

package at.specure.info.strength

import android.annotation.SuppressLint
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthCdma
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthTdscdma
import android.telephony.CellSignalStrengthWcdma
import android.telephony.SignalStrength
import at.specure.info.TransportType
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NetworkInfo
import at.specure.info.network.WifiNetworkInfo
import timber.log.Timber

/**
 * Class that contains data about signal strength
 */
open class SignalStrengthInfo(

    /**
     * Transport type of network
     */
    val transport: TransportType,

    /**
     * Signal strength in dBm
     */
    val value: Int?,

    /**
     * RSRQ in db
     */
    val rsrq: Int?,

    /**
     * Signal level in range 0..4
     */
    val signalLevel: Int,

    /**
     * Minimum signal value for current network type in dBm
     */
    val min: Int,

    /**
     * Maximum signal value for current network type in dBm
     */
    val max: Int,

    /**
     * Timestamp in nanoseconds when data was received
     */
    val timestampNanos: Long
) {

    companion object {
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

        fun from(signal: CellSignalStrengthLte): SignalStrengthInfoLte = SignalStrengthInfoLte(
            transport = TransportType.CELLULAR,
            value = signal.dbm,
            rsrq = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.rsrq else null,
            signalLevel = signal.level,
            min = LTE_RSRP_SIGNAL_MIN,
            max = LTE_RSRP_SIGNAL_MAX,
            timestampNanos = System.nanoTime(),
            cqi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.cqi else null,
            rsrp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.rsrp else null,
            rssi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) signal.rsrq else null,
            rssnr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.rssnr.fixRssnr() else null,
            timingAdvance = signal.timingAdvance
        )

        fun from(signal: CellSignalStrengthWcdma) = SignalStrengthInfo(
            transport = TransportType.CELLULAR,
            value = signal.dbm,
            rsrq = null,
            signalLevel = signal.level,
            min = WCDMA_RSRP_SIGNAL_MIN,
            max = WCDMA_RSRP_SIGNAL_MAX,
            timestampNanos = System.nanoTime()
        )

        fun from(signal: CellSignalStrengthGsm) = SignalStrengthInfoGsm(
            transport = TransportType.CELLULAR,
            value = signal.dbm,
            rsrq = null,
            signalLevel = signal.level,
            min = CELLULAR_SIGNAL_MIN,
            max = CELLULAR_SIGNAL_MAX,
            timestampNanos = System.nanoTime(),
            bitErrorRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) signal.bitErrorRate else null,
            timingAdvance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.timingAdvance else null
        )

        fun from(signal: CellSignalStrengthCdma) = SignalStrengthInfo(
            transport = TransportType.CELLULAR,
            value = signal.dbm,
            rsrq = null,
            signalLevel = signal.level,
            min = WCDMA_RSRP_SIGNAL_MIN,
            max = WCDMA_RSRP_SIGNAL_MAX,
            timestampNanos = System.nanoTime()
        )

        fun from(info: WifiNetworkInfo) = SignalStrengthInfoWiFi(
            transport = TransportType.WIFI,
            value = info.rssi,
            rsrq = null,
            signalLevel = info.signalLevel,
            max = WIFI_MAX_SIGNAL_VALUE,
            min = WIFI_MIN_SIGNAL_VALUE,
            timestampNanos = System.nanoTime(),
            linkSpeed = info.linkSpeed
        )

        fun from(signalStrength: SignalStrength?, network: NetworkInfo?, cellInfo: CellInfo?): SignalStrengthInfo? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                signalStrengthQ(signalStrength)
            } else {
                signalStrengthOld(signalStrength, network, cellInfo)
            }
        }

        @SuppressLint("NewApi")
        private fun signalStrengthQ(signalStrength: SignalStrength?): SignalStrengthInfo? {
            if (signalStrength == null) {
                return null
            }
            var signal: SignalStrengthInfo? = null

            val transportType = TransportType.CELLULAR
            val timestampNanos = System.nanoTime()

            signalStrength.cellSignalStrengths.forEach {
                when (it) {
                    is CellSignalStrengthLte -> {
                        signal = SignalStrengthInfoLte(
                            transport = transportType,
                            value = it.dbm,
                            rsrq = it.rsrq,
                            signalLevel = it.level,
                            min = LTE_RSRP_SIGNAL_MIN,
                            max = LTE_RSRP_SIGNAL_MAX,
                            timestampNanos = timestampNanos,
                            cqi = it.cqi,
                            rsrp = it.rsrp,
                            rssi = it.rssi,
                            rssnr = it.rssnr.fixRssnr(),
                            timingAdvance = it.timingAdvance
                        )
                    }
                    is CellSignalStrengthNr -> {
                        signal = SignalStrengthInfoNr(
                            transport = transportType,
                            value = it.dbm,
                            rsrq = it.csiRsrq,
                            signalLevel = it.level,
                            min = NR_RSRP_SIGNAL_MIN,
                            max = NR_RSRP_SIGNAL_MAX,
                            timestampNanos = timestampNanos,
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
                            transport = transportType,
                            value = it.dbm,
                            rsrq = null,
                            signalLevel = it.level,
                            min = WCDMA_RSRP_SIGNAL_MIN,
                            max = WCDMA_RSRP_SIGNAL_MAX,
                            timestampNanos = timestampNanos
                        )
                    }
                    is CellSignalStrengthGsm -> {
                        signal = SignalStrengthInfoGsm(
                            transport = transportType,
                            value = it.dbm,
                            rsrq = null,
                            signalLevel = it.level,
                            min = CELLULAR_SIGNAL_MIN,
                            max = CELLULAR_SIGNAL_MAX,
                            timestampNanos = timestampNanos,
                            bitErrorRate = it.bitErrorRate,
                            timingAdvance = it.timingAdvance
                        )
                    }
                    else -> {
                        signal = SignalStrengthInfo(
                            transport = transportType,
                            value = it.dbm,
                            rsrq = null,
                            signalLevel = it.level,
                            min = CELLULAR_SIGNAL_MIN,
                            max = CELLULAR_SIGNAL_MAX,
                            timestampNanos = timestampNanos
                        )
                    }
                }
            }

            return signal
        }

        private fun signalStrengthOld(signalStrength: SignalStrength?, network: NetworkInfo?, cellInfo: CellInfo?): SignalStrengthInfo? {
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

            val transportType = TransportType.CELLULAR
            val timestampNanos = System.nanoTime()

            when (cellInfo) {
                null -> {
                    signal = null
                }
                is CellInfoLte -> {
                    signal = SignalStrengthInfoLte(
                        transport = transportType,
                        value = signalValue,
                        rsrq = lteRsrq,
                        signalLevel = calculateCellSignalLevel(signalValue, signalMin, signalMax),
                        min = LTE_RSRP_SIGNAL_MIN,
                        max = LTE_RSRP_SIGNAL_MAX,
                        timestampNanos = timestampNanos,
                        cqi = lteCqi ?: 0,
                        rsrp = lteRsrp ?: 0,
                        rssi = 0,
                        rssnr = lteRssnr.fixRssnr(),
                        timingAdvance = cellInfo.cellSignalStrength.timingAdvance
                    )
                }
                is CellInfoWcdma -> {
                    signal = SignalStrengthInfo(
                        transport = transportType,
                        value = signalValue,
                        rsrq = null,
                        signalLevel = calculateCellSignalLevel(signalValue, signalMin, signalMax),
                        min = WCDMA_RSRP_SIGNAL_MIN,
                        max = WCDMA_RSRP_SIGNAL_MAX,
                        timestampNanos = timestampNanos
                    )
                }
                is CellInfoGsm -> {
                    signal = SignalStrengthInfoGsm(
                        transport = transportType,
                        value = signalValue,
                        rsrq = null,
                        signalLevel = calculateCellSignalLevel(signalValue, signalMin, signalMax),
                        min = CELLULAR_SIGNAL_MIN,
                        max = CELLULAR_SIGNAL_MAX,
                        timestampNanos = timestampNanos,
                        bitErrorRate = signalStrength?.gsmBitErrorRate ?: 0,
                        timingAdvance = 0
                    )
                }
                else -> {
                    SignalStrengthInfo(
                        transport = transportType,
                        value = signalValue,
                        rsrq = lteRsrq,
                        signalLevel = calculateCellSignalLevel(signalValue, signalMin, signalMax),
                        max = signalMax,
                        min = signalMin,
                        timestampNanos = timestampNanos
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

        private fun Int?.fixRssnr(): Int? {
            return if (this == null || (this > 300 || this < -200)) {
                null
            } else
                this
        }
    }
}