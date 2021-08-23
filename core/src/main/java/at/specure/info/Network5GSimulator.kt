package at.specure.info

import android.os.SystemClock
import at.specure.config.Config
import at.specure.info.band.CellBand
import at.specure.info.cell.CellChannelAttribution
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.cell.CellTechnology
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NRConnectionState
import at.specure.info.strength.SignalSource
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthInfoNr
import java.util.UUID
import kotlin.random.Random

private const val SPEED_IN_MAX = 65_000_000L
private const val SPEED_OUT_MAX = 2_000_000_000L

object Network5GSimulator {

    lateinit var config: Config

    val isEnabled: Boolean
        get() = config.developer5GSimulationEnabled

    fun fromInfo(
        isActive: Boolean,
        isRoaming: Boolean,
        apn: String?
    ): CellNetworkInfo {

        return CellNetworkInfo(
            providerName = "5G Simulation",
            band = CellBand(5000, CellChannelAttribution.NRARFCN, 5000, "5000", 5000.0, 5000.0),
            networkType = MobileNetworkType.NR,
            cellType = CellTechnology.CONNECTION_5G,
            mcc = null,
            mnc = null,
            locationId = null,
            areaCode = 1,
            scramblingCode = null,
            cellUUID = UUID.nameUUIDFromBytes("5G simulator".toByteArray()).toString(),
            isActive = isActive,
            isRegistered = true,
            isRoaming = isRoaming,
            apn = apn,
            signalStrength = null,
            dualSimDetectionMethod = null,
            nrConnectionState = NRConnectionState.SA,
            rawCellInfo = null
        )
    }

    fun signalStrength(info: SignalStrengthInfo): SignalStrengthInfoNr {
        val value = when {
            info.value == null -> Random.nextInt(
                SignalStrengthInfo.NR_RSRP_SIGNAL_MIN,
                SignalStrengthInfo.NR_RSRP_SIGNAL_MAX
            )
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
            rsrq = info.rsrq,
            source = SignalSource.NOT_AVAILABLE
        )
    }

    fun downBitPerSec(value: Long): Long {
        return value
    }

    fun upBitPerSec(value: Long): Long {
        return value
    }
}