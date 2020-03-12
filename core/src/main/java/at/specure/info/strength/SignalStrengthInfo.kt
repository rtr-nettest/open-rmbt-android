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
import android.os.SystemClock
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
import kotlin.math.abs

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
        const val WIFI_MIN_SIGNAL_VALUE = -100
        const val WIFI_MAX_SIGNAL_VALUE = -30

        const val CELLULAR_SIGNAL_MIN = -110
        const val CELLULAR_SIGNAL_MAX = -50

        const val LTE_RSRP_SIGNAL_MIN = -130
        const val LTE_RSRP_SIGNAL_MAX = -70

        const val WCDMA_RSRP_SIGNAL_MIN = -120
        const val WCDMA_RSRP_SIGNAL_MAX = -24

        const val NR_RSRP_SIGNAL_MIN = -140
        const val NR_RSRP_SIGNAL_MAX = -44

        const val RSSNR_MIN = -200
        const val RSSNR_MAX = 300

        fun from(signal: CellSignalStrengthLte): SignalStrengthInfoLte = SignalStrengthInfoLte(
            transport = TransportType.CELLULAR,
            value = signal.dbm,
            rsrq = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.rsrq.fixLteRsrq() else null,
            signalLevel = signal.level,
            min = LTE_RSRP_SIGNAL_MIN,
            max = LTE_RSRP_SIGNAL_MAX,
            timestampNanos = SystemClock.elapsedRealtimeNanos(),
            cqi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.cqi.checkValueAvailable() else null,
            rsrp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.rsrp.fixLteRsrp() else null,
            rssi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) signal.rssi.checkValueAvailable() else null,
            rssnr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.rssnr.fixRssnr() else null,
            timingAdvance = signal.timingAdvance.fixTimingAdvance()
        )

        fun from(signal: CellSignalStrengthWcdma) = SignalStrengthInfo(
            transport = TransportType.CELLULAR,
            value = signal.dbm,
            rsrq = null,
            signalLevel = signal.level,
            min = WCDMA_RSRP_SIGNAL_MIN,
            max = WCDMA_RSRP_SIGNAL_MAX,
            timestampNanos = SystemClock.elapsedRealtimeNanos()
        )

        fun from(signal: CellSignalStrengthGsm) = SignalStrengthInfoGsm(
            transport = TransportType.CELLULAR,
            value = signal.dbm,
            rsrq = null,
            signalLevel = signal.level,
            min = CELLULAR_SIGNAL_MIN,
            max = CELLULAR_SIGNAL_MAX,
            timestampNanos = SystemClock.elapsedRealtimeNanos(),
            bitErrorRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) signal.bitErrorRate.fixErrorBitRate() else null,
            timingAdvance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.timingAdvance.fixTimingAdvance() else null
        )

        fun from(signal: CellSignalStrengthCdma) = SignalStrengthInfo(
            transport = TransportType.CELLULAR,
            value = signal.dbm,
            rsrq = null,
            signalLevel = signal.level,
            min = WCDMA_RSRP_SIGNAL_MIN,
            max = WCDMA_RSRP_SIGNAL_MAX,
            timestampNanos = SystemClock.elapsedRealtimeNanos()
        )

        fun from(info: WifiNetworkInfo) = SignalStrengthInfoWiFi(
            transport = TransportType.WIFI,
            value = info.rssi,
            rsrq = null,
            signalLevel = info.signalLevel,
            max = WIFI_MAX_SIGNAL_VALUE,
            min = WIFI_MIN_SIGNAL_VALUE,
            timestampNanos = SystemClock.elapsedRealtimeNanos(),
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
            val timestampNanos = SystemClock.elapsedRealtimeNanos()

            signalStrength.cellSignalStrengths.forEach {
                if (it.dbm == Int.MAX_VALUE) {
                    signal = null
                } else {
                    when (it) {
                        is CellSignalStrengthLte -> {
                            signal = SignalStrengthInfoLte(
                                transport = transportType,
                                value = it.dbm,
                                rsrq = it.rsrq.fixLteRsrq(),
                                signalLevel = it.level,
                                min = LTE_RSRP_SIGNAL_MIN,
                                max = LTE_RSRP_SIGNAL_MAX,
                                timestampNanos = timestampNanos,
                                cqi = it.cqi.checkValueAvailable(),
                                rsrp = it.rsrp.fixLteRsrp(),
                                rssi = it.rssi.checkValueAvailable(),
                                rssnr = it.rssnr.fixRssnr(),
                                timingAdvance = it.timingAdvance.fixTimingAdvance()
                            )
                        }
                        is CellSignalStrengthNr -> {
                            signal = SignalStrengthInfoNr(
                                transport = transportType,
                                value = it.dbm,
                                rsrq = it.csiRsrq.checkValueAvailable(),
                                signalLevel = it.level,
                                min = NR_RSRP_SIGNAL_MIN,
                                max = NR_RSRP_SIGNAL_MAX,
                                timestampNanos = timestampNanos,
                                csiRsrp = it.csiRsrp.checkValueAvailable(),
                                csiRsrq = it.csiRsrq.checkValueAvailable(),
                                csiSinr = it.csiSinr.checkValueAvailable(),
                                ssRsrp = it.ssRsrp.checkValueAvailable(),
                                ssRsrq = it.ssRsrq.checkValueAvailable(),
                                ssSinr = it.ssSinr.checkValueAvailable()
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
                                bitErrorRate = it.bitErrorRate.fixErrorBitRate(),
                                timingAdvance = it.timingAdvance.fixTimingAdvance()
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

            val signalValue = lteRsrp.fixLteRsrp() ?: strength
            val signalMin = if (lteRsrp == null) CELLULAR_SIGNAL_MIN else LTE_RSRP_SIGNAL_MIN
            val signalMax = if (lteRsrp == null) CELLULAR_SIGNAL_MAX else LTE_RSRP_SIGNAL_MAX

            var signal: SignalStrengthInfo? = null

            if (signalValue == null) {
                return null
            }

            val transportType = TransportType.CELLULAR
            val timestampNanos = SystemClock.elapsedRealtimeNanos()

            when (cellInfo) {
                is CellInfoLte -> {
                    signal = SignalStrengthInfoLte(
                        transport = transportType,
                        value = signalValue,
                        rsrq = lteRsrq.fixLteRsrq(),
                        signalLevel = calculateCellSignalLevel(signalValue, signalMin, signalMax),
                        min = LTE_RSRP_SIGNAL_MIN,
                        max = LTE_RSRP_SIGNAL_MAX,
                        timestampNanos = timestampNanos,
                        cqi = lteCqi.checkValueAvailable(),
                        rsrp = lteRsrp.fixLteRsrp(),
                        rssi = null,
                        rssnr = lteRssnr.fixRssnr(),
                        timingAdvance = cellInfo.cellSignalStrength.timingAdvance.checkValueAvailable()
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
                        bitErrorRate = signalStrength?.gsmBitErrorRate.fixErrorBitRate(),
                        timingAdvance = null
                    )
                }
                else -> {
                    signal = SignalStrengthInfo(
                        transport = transportType,
                        value = signalValue,
                        rsrq = lteRsrq.fixLteRsrq(),
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

        private fun Int?.fixRssnr(): Int? =
            if (this == null || this > 300 || this < -200 || this == RSSNR_MIN || this == RSSNR_MAX) {
                null
            } else {
                this
            }

        private fun Int?.checkValueAvailable(): Int? = if (this == null || this == Int.MIN_VALUE || this == Int.MAX_VALUE) {
            null
        } else {
            this
        }

        private fun Int?.fixTimingAdvance(): Int? = if (this == null || this == Int.MIN_VALUE || this == Int.MAX_VALUE || this > 2182) {
            null
        } else {
            this
        }

        private fun Int?.fixLteRsrp(): Int? = if (this == null || this == Int.MIN_VALUE || this == Int.MAX_VALUE || this < -140 || this == -1) {
            null
        } else {
            this
        }

        private fun Int?.fixLteRsrq(): Int? =
            if (this == null || this == Int.MIN_VALUE || this == Int.MAX_VALUE || abs(this) > 19.5 || abs(this) < 3.0) {
                null
            } else {
                this
            }

        private fun Int?.fixErrorBitRate(): Int? =
            if (this == null || this == Int.MIN_VALUE || this >= 99) {
                null
            } else {
                this
            }
    }
}