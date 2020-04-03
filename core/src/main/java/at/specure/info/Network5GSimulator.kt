package at.specure.info

import android.telephony.CellInfo
import at.specure.config.Config
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.cell.uuid
import at.specure.info.network.MobileNetworkType
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
            signalStrength = null
        )
    }

    fun downBitPerSec(value: Long): Long {
        return if (config.developer5GSimulationEnabled) {
            val input = if (value < SPEED_IN_MAX) value else SPEED_IN_MAX
            val noise = Random.nextLong(100000)
            ((input / SPEED_IN_MAX.toDouble()) * SPEED_OUT_MAX).toLong() - noise
        } else {
            value
        }
    }

    fun upBitPerSec(value: Long): Long {
        return if (config.developer5GSimulationEnabled) {
            val input = if (value < SPEED_IN_MAX) value else SPEED_IN_MAX
            val noise = Random.nextLong(100000)
            ((input / SPEED_IN_MAX.toDouble()) * SPEED_OUT_MAX).toLong() - noise
        } else {
            value
        }
    }
}