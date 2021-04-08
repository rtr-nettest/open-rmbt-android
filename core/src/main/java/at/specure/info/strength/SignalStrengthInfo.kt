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
import android.os.Parcelable
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoTdscdma
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrength
import android.telephony.CellSignalStrengthCdma
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthTdscdma
import android.telephony.CellSignalStrengthWcdma
import android.telephony.SignalStrength
import androidx.annotation.RequiresApi
import at.specure.info.TransportType
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NRConnectionState
import at.specure.info.network.NetworkInfo
import at.specure.info.network.WifiNetworkInfo
import at.specure.info.strength.SignalStrengthInfo.Companion.checkValueAvailable
import timber.log.Timber
import kotlin.math.abs

/**
 * Class that contains data about signal strength
 */
abstract class SignalStrengthInfo : Parcelable {
    /**
     * Transport type of network
     */
    abstract val transport: TransportType

    /**
     * Signal strength in dBm
     */
    abstract val value: Int?

    /**
     * RSRQ in db
     */
    abstract val rsrq: Int?

    /**
     * Signal level in range 0..4
     */
    abstract val signalLevel: Int

    /**
     * Minimum signal value for current network type in dBm
     */
    abstract val min: Int

    /**
     * Maximum signal value for current network type in dBm
     */
    abstract val max: Int

    /**
     * Timestamp in nanoseconds when data was received
     */
    abstract val timestampNanos: Long

    /**
     * Source of the signal information
     */
    abstract val source: SignalSource

    companion object {
        const val WIFI_MIN_SIGNAL_VALUE = -100
        const val WIFI_MAX_SIGNAL_VALUE = -30

        const val CELLULAR_SIGNAL_MIN = -110
        const val CELLULAR_SIGNAL_MAX = -50

        const val LTE_RSRP_SIGNAL_MIN = -130
        const val LTE_RSRP_SIGNAL_MAX = -70

        const val CDMA_RSRP_SIGNAL_MIN = -120
        const val CDMA_RSRP_SIGNAL_MAX = -24

        const val WCDMA_RSRP_SIGNAL_MIN = -120
        const val WCDMA_RSRP_SIGNAL_MAX = -24

        const val TDSCDMA_RSRP_SIGNAL_MIN = -120
        const val TDSCDMA_RSRP_SIGNAL_MAX = -24

        const val NR_RSRP_SIGNAL_MIN = -140 // dbm
        const val NR_RSRP_SIGNAL_MAX = -44 // values taken from CellSignalStrengthNr

        const val NR_RSRQ_SIGNAL_MIN = -20 // dbm
        const val NR_RSRQ_SIGNAL_MAX = -3 // values taken from CellSignalStrengthNr

        const val NR_SINR_SIGNAL_MIN = -23 // dbm
        const val NR_SINR_SIGNAL_MAX = 40 // values taken from CellSignalStrengthNr

        // Lifted from Default carrier configs and max range of SSRSRP from
        // mSsRsrpThresholds array
        // Boundaries: [-140 dB, -44 dB]
        const val SSRSRP_SIGNAL_STRENGTH_NONE = -140
        const val SSRSRP_SIGNAL_STRENGTH_POOR = -110
        const val SSRSRP_SIGNAL_STRENGTH_MODERATE = -90
        const val SSRSRP_SIGNAL_STRENGTH_GOOD = -80
        const val SSRSRP_SIGNAL_STRENGTH_FULL = -65
        const val SSRSRP_SIGNAL_STRENGTH_MAX = -44

        @SuppressLint("BinaryOperationInTimber")
        @RequiresApi(Build.VERSION_CODES.Q)
        fun from(signal: CellSignalStrengthNr, source: SignalSource): SignalStrengthInfoNr {
            Timber.d(
                "Extracting from 1: $signal \n\n\n to: dbm: ${signal.dbm} csiRsrp: ${signal.csiRsrp} csiRsrq: ${signal.csiRsrq} csiSinr: ${signal.csiSinr} " +
                        "ssRsrp: ${signal.ssRsrp} ssRsrq: ${signal.ssRsrq} ssSinr: ${signal.ssSinr}"
            )
            return SignalStrengthInfoNr(
                transport = TransportType.CELLULAR,
                value = signal.extractSignalValue()?.fixNrRsrp(),
                rsrq = signal.extractSignalQualityValue()?.fixNrRsrq(),
                signalLevel = calculateNRCellSignalLevel(signal),
                min = NR_RSRP_SIGNAL_MIN,
                max = NR_RSRP_SIGNAL_MAX,
                timestampNanos = System.nanoTime(),
                csiRsrp = if (signal.csiRsrp == CellInfo.UNAVAILABLE) null else signal.csiRsrp.fixNrRsrp(),
                csiRsrq = if (signal.csiRsrq == CellInfo.UNAVAILABLE) null else signal.csiRsrq.fixNrRsrq(),
                csiSinr = if (signal.csiSinr == CellInfo.UNAVAILABLE) null else signal.csiSinr.fixNrSinr(),
                ssRsrp = if (signal.ssRsrp == CellInfo.UNAVAILABLE) null else signal.ssRsrp.fixNrRsrp(),
                ssRsrq = if (signal.ssRsrq == CellInfo.UNAVAILABLE) null else signal.ssRsrq.fixNrRsrq(),
                ssSinr = if (signal.ssSinr == CellInfo.UNAVAILABLE) null else signal.ssSinr.fixNrSinr(),
                source = source
            )
        }

        fun from(signal: CellSignalStrengthLte, source: SignalSource): SignalStrengthInfoLte = SignalStrengthInfoLte(
            transport = TransportType.CELLULAR,
            value = signal.dbm,
            rsrq = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.rsrq.fixLteRsrq() else null,
            signalLevel = signal.level,
            min = LTE_RSRP_SIGNAL_MIN,
            max = LTE_RSRP_SIGNAL_MAX,
            timestampNanos = System.nanoTime(),
            cqi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.cqi.checkValueAvailable() else null,
            rsrp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.rsrp.fixLteRsrp() else signal.dbm.fixLteRsrp(),
            rssi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) signal.rssi.checkValueAvailable() else null,
            rssnr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.rssnr.fixRssnr() else null,
            timingAdvance = signal.timingAdvance.fixLteTimingAdvance(),
            source = source
        )

        fun from(signal: CellSignalStrengthWcdma, source: SignalSource) = SignalStrengthInfoCommon(
            transport = TransportType.CELLULAR,
            value = signal.dbm,
            rsrq = null,
            signalLevel = signal.level,
            min = WCDMA_RSRP_SIGNAL_MIN,
            max = WCDMA_RSRP_SIGNAL_MAX,
            timestampNanos = System.nanoTime(),
            source = source
        )

        fun from(signal: CellSignalStrengthTdscdma, source: SignalSource) = SignalStrengthInfoCommon(
            transport = TransportType.CELLULAR,
            value = signal.dbm,
            rsrq = null,
            signalLevel = signal.level,
            min = TDSCDMA_RSRP_SIGNAL_MIN,
            max = TDSCDMA_RSRP_SIGNAL_MAX,
            timestampNanos = System.nanoTime(),
            source = source
        )

        fun from(signal: CellSignalStrengthGsm, source: SignalSource) = SignalStrengthInfoGsm(
            transport = TransportType.CELLULAR,
            value = signal.dbm,
            rsrq = null,
            signalLevel = signal.level,
            min = CELLULAR_SIGNAL_MIN,
            max = CELLULAR_SIGNAL_MAX,
            timestampNanos = System.nanoTime(),
            bitErrorRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) signal.bitErrorRate.fixErrorBitRate() else null,
            timingAdvance = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) signal.timingAdvance.fixGsmTimingAdvance() else null,
            source = source
        )

        fun from(signal: CellSignalStrengthCdma, source: SignalSource) = SignalStrengthInfoCommon(
            transport = TransportType.CELLULAR,
            value = signal.dbm,
            rsrq = null,
            signalLevel = signal.level,
            min = WCDMA_RSRP_SIGNAL_MIN,
            max = WCDMA_RSRP_SIGNAL_MAX,
            timestampNanos = System.nanoTime(),
            source = source
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

        fun from(
            signalStrength: SignalStrength?,
            network: NetworkInfo?,
            cellInfo: CellInfo?,
            nrConnectionState: NRConnectionState,
            isDualSim: Boolean
        ): SignalStrengthInfo? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                signalStrengthQ(signalStrength, cellInfo, nrConnectionState)
            } else {
                signalStrengthOld(signalStrength, network, cellInfo, isDualSim)
            }
        }

        @SuppressLint("NewApi", "BinaryOperationInTimber")
        private fun signalStrengthQ(signalStrength: SignalStrength?, cellInfo: CellInfo?, nrConnectionState: NRConnectionState): SignalStrengthInfo? {
            if (signalStrength == null) {
                val message =
                    "SSPQ - SignalStrength: null"
                Timber.v(message)
                return null
            }
            var signal: SignalStrengthInfo? = null

            val transportType = TransportType.CELLULAR
            val timestampNanos = System.nanoTime()

            if (cellInfo != null) {
                signal = extractSignalFromCellInfo(nrConnectionState, cellInfo, timestampNanos, transportType)
            } else {
                signal = extractSignalFromSignalStrengthChangeValue(signalStrength, nrConnectionState, cellInfo, timestampNanos, transportType)
            }
            return signal
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun extractSignalFromCellInfo(
            nrConnectionState: NRConnectionState,
            cellInfo: CellInfo?,
            timestampNanos: Long,
            transportType: TransportType
        ): SignalStrengthInfo? {
            Timber.d("Extracting from cellInfo")
            when (cellInfo) {
                is CellInfoNr ->
                    if (cellInfo.cellSignalStrength is CellSignalStrengthNr) {
                        val cellSignalStrengthNr = cellInfo.cellSignalStrength as CellSignalStrengthNr
                        val signalValue = cellSignalStrengthNr.extractSignalValue()?.fixNrRsrp()
                        // if we are not able to extract signal information from inactive NR cell info (inactive because of NSA mode), we are returning null signal
                        if (signalValue != null) {

                            Timber.d(
                                "Extracting from cell 2: $cellSignalStrengthNr \n\n\n to: dbm: ${cellSignalStrengthNr.dbm} csiRsrp: ${cellSignalStrengthNr.csiRsrp} csiRsrq: ${cellSignalStrengthNr.csiRsrq} csiSinr: ${cellSignalStrengthNr.csiSinr} " +
                                        "ssRsrp: ${cellSignalStrengthNr.ssRsrp} ssRsrq: ${cellSignalStrengthNr.ssRsrq} ssSinr: ${cellSignalStrengthNr.ssSinr}"
                            )

                            return SignalStrengthInfoNr(
                                transport = TransportType.CELLULAR,
                                value = signalValue.fixNrRsrp(),
                                rsrq = cellSignalStrengthNr.ssRsrq.checkValueAvailable()?.fixNrRsrq(),
                                signalLevel = cellSignalStrengthNr.level,
                                min = NR_RSRP_SIGNAL_MIN,
                                max = NR_RSRP_SIGNAL_MAX,
                                timestampNanos = timestampNanos,
                                csiRsrp = cellSignalStrengthNr.csiRsrp.checkValueAvailable()?.fixNrRsrp(),
                                csiRsrq = cellSignalStrengthNr.csiRsrq.checkValueAvailable()?.fixNrRsrq(),
                                csiSinr = cellSignalStrengthNr.csiSinr.checkValueAvailable()?.fixNrSinr(),
                                ssRsrp = cellSignalStrengthNr.ssRsrp.checkValueAvailable()?.fixNrRsrp(),
                                ssRsrq = cellSignalStrengthNr.ssRsrq.checkValueAvailable()?.fixNrRsrq(),
                                ssSinr = cellSignalStrengthNr.ssSinr.checkValueAvailable()?.fixNrSinr(),
                                source = SignalSource.CELL_INFO
                            )
                        } else {
                            return null
                        }
                    } else {
                        return null
                    }
                is CellInfoLte ->
                    if (cellInfo.cellSignalStrength is CellSignalStrengthLte) {
                        val cellSignalStrengthLte = cellInfo.cellSignalStrength as CellSignalStrengthLte
                        return SignalStrengthInfoLte(
                            transport = transportType,
                            value = cellSignalStrengthLte.dbm.let { nrSignal -> -abs(nrSignal) },
                            rsrq = cellSignalStrengthLte.rsrq.fixLteRsrq(),
                            signalLevel = cellSignalStrengthLte.level,
                            min = LTE_RSRP_SIGNAL_MIN,
                            max = LTE_RSRP_SIGNAL_MAX,
                            timestampNanos = timestampNanos,
                            cqi = cellSignalStrengthLte.cqi.checkValueAvailable(),
                            rsrp = cellSignalStrengthLte.rsrp.fixLteRsrp(),
                            rssi = cellSignalStrengthLte.rssi.checkValueAvailable(),
                            rssnr = cellSignalStrengthLte.rssnr.fixRssnr(),
                            timingAdvance = cellInfo.lteTimingAdvance(),
                            source = SignalSource.CELL_INFO
                        )
                    } else {
                        return null
                    }
                is CellInfoGsm ->
                    if (cellInfo.cellSignalStrength is CellSignalStrengthGsm) {
                        val cellSignalStrengthGsm = cellInfo.cellSignalStrength as CellSignalStrengthGsm
                        return SignalStrengthInfoGsm(
                            transport = transportType,
                            value = cellSignalStrengthGsm.dbm,
                            rsrq = null,
                            signalLevel = cellSignalStrengthGsm.level,
                            min = CELLULAR_SIGNAL_MIN,
                            max = CELLULAR_SIGNAL_MAX,
                            timestampNanos = timestampNanos,
                            bitErrorRate = cellSignalStrengthGsm.bitErrorRate.fixErrorBitRate(),
                            timingAdvance = cellSignalStrengthGsm.timingAdvance.fixGsmTimingAdvance(),
                            source = SignalSource.CELL_INFO
                        )
                    } else {
                        return null
                    }
                is CellInfoCdma ->
                    if (cellInfo.cellSignalStrength is CellSignalStrengthCdma) {
                        val cellSignalStrengthCdma = cellInfo.cellSignalStrength as CellSignalStrengthCdma
                        return SignalStrengthInfoCommon(
                            transport = transportType,
                            value = cellSignalStrengthCdma.dbm,
                            rsrq = null,
                            signalLevel = cellSignalStrengthCdma.level,
                            min = CDMA_RSRP_SIGNAL_MIN,
                            max = CDMA_RSRP_SIGNAL_MAX,
                            timestampNanos = timestampNanos,
                            source = SignalSource.CELL_INFO
                        )
                    } else {
                        return null
                    }
                is CellInfoWcdma ->
                    if (cellInfo.cellSignalStrength is CellSignalStrengthWcdma) {
                        val cellSignalStrengthWcdma = cellInfo.cellSignalStrength as CellSignalStrengthWcdma
                        return SignalStrengthInfoCommon(
                            transport = transportType,
                            value = cellSignalStrengthWcdma.dbm,
                            rsrq = null,
                            signalLevel = cellSignalStrengthWcdma.level,
                            min = WCDMA_RSRP_SIGNAL_MIN,
                            max = WCDMA_RSRP_SIGNAL_MAX,
                            timestampNanos = timestampNanos,
                            source = SignalSource.CELL_INFO
                        )
                    } else {
                        return null
                    }
                is CellInfoTdscdma ->
                    if (cellInfo.cellSignalStrength is CellSignalStrengthTdscdma) {
                        val cellSignalStrengthTdscdma = cellInfo.cellSignalStrength as CellSignalStrengthTdscdma
                        return SignalStrengthInfoCommon(
                            transport = transportType,
                            value = cellSignalStrengthTdscdma.dbm,
                            rsrq = null,
                            signalLevel = cellSignalStrengthTdscdma.level,
                            min = TDSCDMA_RSRP_SIGNAL_MIN,
                            max = TDSCDMA_RSRP_SIGNAL_MAX,
                            timestampNanos = timestampNanos,
                            source = SignalSource.CELL_INFO
                        )
                    } else {
                        return null
                    }
            }
            return null
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun extractSignalFromSignalStrengthChangeValue(
            signalStrength: SignalStrength,
            nrConnectionState: NRConnectionState,
            cellInfo: CellInfo?,
            timestampNanos: Long,
            transportType: TransportType
        ): SignalStrengthInfo? {
            Timber.d("Extracting from onSignalStrength")
            var signal: SignalStrengthInfo? = null
            signalStrength.cellSignalStrengths.forEach {
                if (it.dbm == Int.MAX_VALUE) {
                    signal = null
                    val message =
                        "SSPQ - SignalStrength: Int.maxValue"
                    Timber.v(message)
                } else {
                    when (it) {
                        is CellSignalStrengthLte -> {
                            if (nrConnectionState == NRConnectionState.NSA) {
                                // this is a case when there is 4G signal but we detected inactive NR cell during 5G NSA mode
                                if ((cellInfo is CellInfoNr) && (cellInfo.cellSignalStrength is CellSignalStrengthNr)) {
                                    val cellSignalStrengthNr = cellInfo.cellSignalStrength as CellSignalStrengthNr
                                    val signalValue = cellSignalStrengthNr.extractSignalValue()?.fixNrRsrp()
                                    // if we are not able to extract signal information from inactive NR cell info (inactive because of NSA mode), we are returning null signal
                                    if (signalValue != null) {

                                        Timber.d(
                                            "Extracting from 2: $cellSignalStrengthNr \n\n\n to: dbm: ${cellSignalStrengthNr.dbm} csiRsrp: ${cellSignalStrengthNr.csiRsrp} csiRsrq: ${cellSignalStrengthNr.csiRsrq} csiSinr: ${cellSignalStrengthNr.csiSinr} " +
                                                    "ssRsrp: ${cellSignalStrengthNr.ssRsrp} ssRsrq: ${cellSignalStrengthNr.ssRsrq} ssSinr: ${cellSignalStrengthNr.ssSinr}"
                                        )

                                        return SignalStrengthInfoNr(
                                            transport = TransportType.CELLULAR,
                                            value = signalValue.fixNrRsrp(),
                                            rsrq = cellSignalStrengthNr.ssRsrq.checkValueAvailable()?.fixNrRsrq(),
                                            signalLevel = cellSignalStrengthNr.level,
                                            min = NR_RSRP_SIGNAL_MIN,
                                            max = NR_RSRP_SIGNAL_MAX,
                                            timestampNanos = timestampNanos,
                                            csiRsrp = cellSignalStrengthNr.csiRsrp.checkValueAvailable()?.fixNrRsrp(),
                                            csiRsrq = cellSignalStrengthNr.csiRsrq.checkValueAvailable()?.fixNrRsrq(),
                                            csiSinr = cellSignalStrengthNr.csiSinr.checkValueAvailable()?.fixNrSinr(),
                                            ssRsrp = cellSignalStrengthNr.ssRsrp.checkValueAvailable()?.fixNrRsrp(),
                                            ssRsrq = cellSignalStrengthNr.ssRsrq.checkValueAvailable()?.fixNrRsrq(),
                                            ssSinr = cellSignalStrengthNr.ssSinr.checkValueAvailable()?.fixNrSinr(),
                                            source = SignalSource.CELL_INFO
                                        )
                                    } else {
                                        return null
                                    }
                                } else {
                                    return null
                                }
                            } else {
                                signal = SignalStrengthInfoLte(
                                    transport = transportType,
                                    value = it.dbm.let { nrSignal -> -abs(nrSignal) },
                                    rsrq = it.rsrq.fixLteRsrq(),
                                    signalLevel = it.level,
                                    min = LTE_RSRP_SIGNAL_MIN,
                                    max = LTE_RSRP_SIGNAL_MAX,
                                    timestampNanos = timestampNanos,
                                    cqi = it.cqi.checkValueAvailable(),
                                    rsrp = it.rsrp.fixLteRsrp(),
                                    rssi = it.rssi.checkValueAvailable(),
                                    rssnr = it.rssnr.fixRssnr(),
                                    timingAdvance = cellInfo.lteTimingAdvance() ?: it.timingAdvance.fixLteTimingAdvance(),
                                    source = SignalSource.SIGNAL_STRENGTH_CHANGED
                                )
                            }
                        }
                        is CellSignalStrengthNr -> {
                            Timber.d(
                                "Extracting from 3: $it \n\n\n to: dbm: ${it.dbm} csiRsrp: ${it.csiRsrp} csiRsrq: ${it.csiRsrq} csiSinr: ${it.csiSinr} " +
                                        "ssRsrp: ${it.ssRsrp} ssRsrq: ${it.ssRsrq} ssSinr: ${it.ssSinr}"
                            )
                            signal = SignalStrengthInfoNr(
                                transport = transportType,
                                value = it.dbm.let { dbm -> -abs(dbm) }.fixNrRsrp(),
                                rsrq = it.csiRsrq.checkValueAvailable().fixNrRsrq(),
                                signalLevel = it.level,
                                min = NR_RSRP_SIGNAL_MIN,
                                max = NR_RSRP_SIGNAL_MAX,
                                timestampNanos = timestampNanos,
                                csiRsrp = it.csiRsrp.checkValueAvailable()?.fixNrRsrp(),
                                csiRsrq = it.csiRsrq.checkValueAvailable()?.fixNrRsrq(),
                                csiSinr = it.csiSinr.checkValueAvailable()?.fixNrSinr(),
                                ssRsrp = it.ssRsrp.checkValueAvailable()?.fixNrRsrp(),
                                ssRsrq = it.ssRsrq.checkValueAvailable()?.fixNrRsrq(),
                                ssSinr = it.ssSinr.checkValueAvailable()?.fixNrSinr(),
                                source = SignalSource.SIGNAL_STRENGTH_CHANGED
                            )
                        }
                        is CellSignalStrengthTdscdma,
                        is CellSignalStrengthWcdma -> {
                            signal = SignalStrengthInfoCommon(
                                transport = transportType,
                                value = it.dbm,
                                rsrq = null,
                                signalLevel = it.level,
                                min = WCDMA_RSRP_SIGNAL_MIN,
                                max = WCDMA_RSRP_SIGNAL_MAX,
                                timestampNanos = timestampNanos,
                                source = SignalSource.SIGNAL_STRENGTH_CHANGED
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
                                timingAdvance = it.timingAdvance.fixGsmTimingAdvance(),
                                source = SignalSource.SIGNAL_STRENGTH_CHANGED
                            )
                        }
                        else -> {
                            signal = SignalStrengthInfoCommon(
                                transport = transportType,
                                value = it.dbm,
                                rsrq = null,
                                signalLevel = it.level,
                                min = CELLULAR_SIGNAL_MIN,
                                max = CELLULAR_SIGNAL_MAX,
                                timestampNanos = timestampNanos,
                                source = SignalSource.SIGNAL_STRENGTH_CHANGED
                            )
                        }
                    }
                }
            }
            return signal
        }

        private fun signalStrengthOld(
            signalStrength: SignalStrength?,
            network: NetworkInfo?,
            cellInfo: CellInfo?,
            isDualSim: Boolean
        ): SignalStrengthInfo? {
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

                // correction for dual sims, because with dual sims we receive updates for both sims so we must filter signal changes only for relevant sim
                if (isDualSim) {
                    when (network.signalStrength) {
                        is SignalStrengthInfoLte -> {
                            try {
                                lteRsrp = network.signalStrength.rsrp
                                lteRsrq = network.signalStrength.rsrq
                                lteRssnr = network.signalStrength.rssnr
                                lteCqi = network.signalStrength.cqi

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
                        }
                    }
                    strength = network.signalStrength?.value
                }
            }

            if (strength == null) {
                if (cellInfo is CellInfoWcdma) {
                    val cellSignalStrength = (cellInfo as CellInfoWcdma).cellSignalStrength
                    cellSignalStrength?.let {
                        strength = if (it.dbm != CellInfo.UNAVAILABLE) it.dbm else null
                    }
                    if (strength == null)
                        try {
                            val getDbm = CellSignalStrength::class.java.getMethod("getDbm")
                            val result = getDbm.invoke(cellSignalStrength) as Int
                            if (result != CellInfo.UNAVAILABLE)
                                strength = result
                        } catch (t: Throwable) {
                            Timber.e(t)
                        }
                } else {
                    Timber.v("SSP - cellInfo is not wcdma type")
                }
            }

            val signalValue = lteRsrp.fixLteRsrp() ?: strength
            val signalMin = if (lteRsrp == null) CELLULAR_SIGNAL_MIN else LTE_RSRP_SIGNAL_MIN
            val signalMax = if (lteRsrp == null) CELLULAR_SIGNAL_MAX else LTE_RSRP_SIGNAL_MAX

            var signal: SignalStrengthInfo? = null

            var message = ""

            if (network is CellNetworkInfo) {
                message =
                    "SSP - Model: ${Build.MODEL} \n Network type: ${network.networkType}\n SignalStrength: $signalStrength\n SignalValue: $signalValue\n CellInfo: $cellInfo"
                Timber.v(message)
            }

            if (signalValue == null) {
                return null
            }

            val transportType = TransportType.CELLULAR
            val timestampNanos = System.nanoTime()
            Timber.v("Signal time 1: $timestampNanos")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (cellInfo is CellInfoNr) {
                    signal = SignalStrengthInfoCommon(
                        transport = transportType,
                        value = signalValue,
                        rsrq = null,
                        signalLevel = calculateCellSignalLevel(signalValue, signalMin, signalMax),
                        min = NR_RSRP_SIGNAL_MIN,
                        max = NR_RSRP_SIGNAL_MAX,
                        timestampNanos = timestampNanos,
                        source = SignalSource.SIGNAL_STRENGTH_CHANGED
                    )
                }
            }

            if (signal == null) {
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
                            timingAdvance = cellInfo.cellSignalStrength.timingAdvance.checkValueAvailable().fixLteTimingAdvance(),
                            source = SignalSource.SIGNAL_STRENGTH_CHANGED
                        )
                    }
                    is CellInfoWcdma -> {
                        signal = SignalStrengthInfoCommon(
                            transport = transportType,
                            value = signalValue,
                            rsrq = null,
                            signalLevel = calculateCellSignalLevel(signalValue, signalMin, signalMax),
                            min = WCDMA_RSRP_SIGNAL_MIN,
                            max = WCDMA_RSRP_SIGNAL_MAX,
                            timestampNanos = timestampNanos,
                            source = SignalSource.SIGNAL_STRENGTH_CHANGED
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
                            timingAdvance = null,
                            source = SignalSource.SIGNAL_STRENGTH_CHANGED
                        )
                    }
                    else -> {
                        signal = SignalStrengthInfoCommon(
                            transport = transportType,
                            value = signalValue,
                            rsrq = lteRsrq.fixLteRsrq(),
                            signalLevel = calculateCellSignalLevel(signalValue, signalMin, signalMax),
                            max = signalMax,
                            min = signalMin,
                            timestampNanos = timestampNanos,
                            source = SignalSource.SIGNAL_STRENGTH_CHANGED
                        )
                    }
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

        /**
         * Count level of the 5G signal only from ssrsrp field value according to NR ssrsrp thresholds
         * 0 - NONE
         * 1
         * 2
         * 3
         * 4 - HIGHEST
         */
        private fun calculateNRCellSignalLevel(signal: CellSignalStrengthNr): Int {
            val signalValue = signal.extractSignalValue().fixNrRsrp()
            return when {
                signalValue == null -> 0
                signalValue <= SSRSRP_SIGNAL_STRENGTH_NONE -> 0
                signalValue < SSRSRP_SIGNAL_STRENGTH_POOR -> 1
                signalValue < SSRSRP_SIGNAL_STRENGTH_MODERATE -> 2
                signalValue < SSRSRP_SIGNAL_STRENGTH_GOOD -> 3
                else -> 4
            }
        }

        private fun Int?.fixRssnr(): Int? {
            return if (this == null) {
                null
            } else {
                var value = -1 * abs(this)
                if (value < NR_RSRP_SIGNAL_MIN || value > NR_RSRP_SIGNAL_MAX || this == Int.MIN_VALUE) {
                    null
                } else {
                    this
                }
            }
        }

        private fun Int?.fixNrRsrp(): Int? {
            return if (this == null) {
                null
            } else {
                var value = -1 * abs(this)
                if (value < NR_RSRP_SIGNAL_MIN || value > NR_RSRP_SIGNAL_MAX || this == Int.MIN_VALUE || this == -1 * abs(CellInfo.UNAVAILABLE)) {
                    null
                } else {
                    -1 * abs(this)
                }
            }
        }

        private fun Int?.fixNrRsrq(): Int? {
            return if (this == null) {
                null
            } else {
                var value = -1 * abs(this)
                if (value < NR_RSRQ_SIGNAL_MIN || value > NR_RSRQ_SIGNAL_MAX || this == Int.MIN_VALUE || this == -1 * abs(CellInfo.UNAVAILABLE)) {
                    null
                } else {
                    -abs(this)
                }
            }
        }

        private fun Int?.fixNrSinr(): Int? =
            if (this == null || this < NR_SINR_SIGNAL_MIN || this > NR_SINR_SIGNAL_MAX || this == Int.MIN_VALUE || this == CellInfo.UNAVAILABLE) {
                null
            } else {
                this
            }

        internal fun Int?.checkValueAvailable(): Int? = if (this == null || this == Int.MIN_VALUE || this == Int.MAX_VALUE) {
            null
        } else {
            this
        }

        private fun Int?.fixLteTimingAdvance(): Int? =
            if (this == null || this == Int.MIN_VALUE || this == Int.MAX_VALUE || this > 1282 || this < 0) {
                null
            } else {
                this
            }

        private fun Int?.fixGsmTimingAdvance(): Int? =
            if (this == null || this == Int.MIN_VALUE || this == Int.MAX_VALUE || this > 219 || this < 0) {
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
            if (this == null || this == Int.MIN_VALUE || this > 99 || this < 0 || (this in 8..98)) {
                null
            } else {
                this
            }

        private fun CellInfo?.lteTimingAdvance(): Int? {
            if (this != null && this is CellInfoLte) {
                return this.cellSignalStrength.timingAdvance.fixLteTimingAdvance()
            }
            return null
        }
    }
}

fun CellSignalStrengthNr.extractSignalValue(): Int? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (abs(dbm) == abs(CellInfo.UNAVAILABLE)) {
            if (ssRsrp.checkValueAvailable() != null) {
                ssRsrp.checkValueAvailable()?.let { nrSignal -> -abs(nrSignal) }
            } else {
                dbm
            }
        } else dbm
    } else {
        null
    }
}

fun CellSignalStrengthNr.extractSignalQualityValue(): Int? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        if (abs(csiRsrq) == abs(CellInfo.UNAVAILABLE)) {
            if (ssRsrq.checkValueAvailable() != null) {
                ssRsrq.checkValueAvailable()?.let { nrSignalQuality -> -abs(nrSignalQuality) }
            } else {
                csiRsrq
            }
        } else csiRsrq
    } else {
        null
    }
}