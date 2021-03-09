package at.specure.info

import android.os.SystemClock
import android.telephony.CellInfo
import at.specure.config.Config
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.cell.uuid
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NRConnectionState
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthInfoNr
import kotlin.random.Random

private const val SPEED_IN_MAX = 65_000_000L
private const val SPEED_OUT_MAX = 2_000_000_000L

object Network5GSimulator {

    lateinit var config: Config

    val isEnabled: Boolean
        get() = config.developer5GSimulationEnabled

    fun fromInfo(
        info: CellInfo,
        isActive: Boolean,
        isRoaming: Boolean,
        apn: String?
    ): CellNetworkInfo {

        return CellNetworkInfo(
            providerName = "5G Simulation",
            band = null,
            networkType = MobileNetworkType.NR,
            mcc = null,
            mnc = null,
            locationId = null,
            areaCode = 1,
            scramblingCode = null,
            cellUUID = info.uuid(),
            isActive = isActive,
            isRegistered = info.isRegistered,
            isRoaming = isRoaming,
            apn = apn,
            signalStrength = null,
            dualSimDetectionMethod = null,
            nrConnectionState = NRConnectionState.SA
        )
    }

    fun signalStrength(info: SignalStrengthInfo): SignalStrengthInfoNr {
        val value = when {
            info.value == null -> Random.nextInt(SignalStrengthInfo.NR_RSRP_SIGNAL_MIN, SignalStrengthInfo.NR_RSRP_SIGNAL_MIN)
            info.value!! < SignalStrengthInfo.NR_RSRP_SIGNAL_MIN -> SignalStrengthInfo.NR_RSRP_SIGNAL_MIN
            info.value!! > SignalStrengthInfo.NR_RSRP_SIGNAL_MAX -> SignalStrengthInfo.NR_RSRP_SIGNAL_MAX
            else -> info.value
        }
        return SignalStrengthInfoNr(
            transport = TransportType.CELLULAR,
            value = value,
            signalLevel = info.signalLevel,
            min = SignalStrengthInfo.NR_RSRP_SIGNAL_MIN,
            max = SignalStrengthInfo.NR_RSRP_SIGNAL_MAX,
            timestampNanos = SystemClock.elapsedRealtimeNanos(),
            csiRsrp = Random.nextInt(-140, -44),
            csiRsrq = Random.nextInt(-20, -3),
            csiSinr = Random.nextInt(-23, 23),
            ssRsrp = Random.nextInt(-140, -44),
            ssRsrq = Random.nextInt(-20, -3),
            ssSinr = Random.nextInt(-23, 40),
            rsrq = info.rsrq
        )
    }

    fun downBitPerSec(value: Long): Long {
        return if (config.developer5GSimulationEnabled) {
            if (value == 0L) {
                0L
            } else {
                val input = if (value < SPEED_IN_MAX) value else SPEED_IN_MAX
                val noise = Random.nextLong(100000)
                ((input / SPEED_IN_MAX.toDouble()) * SPEED_OUT_MAX).toLong() - noise
            }
        } else {
            value
        }
    }

    fun upBitPerSec(value: Long): Long {
        return if (config.developer5GSimulationEnabled) {
            if (value == 0L) {
                0L
            } else {
                val input = if (value < SPEED_IN_MAX) value else SPEED_IN_MAX
                val noise = Random.nextLong(100000)
                ((input / SPEED_IN_MAX.toDouble()) * SPEED_OUT_MAX).toLong() - noise
            }
        } else {
            value
        }
    }
}