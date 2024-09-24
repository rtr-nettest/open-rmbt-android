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

package at.specure.info.cell

import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import at.specure.info.TransportType
import at.specure.info.band.CellBand
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NRConnectionState
import at.specure.info.network.NetworkInfo
import at.specure.info.strength.SignalStrengthInfo
import android.telephony.CellIdentityCdma
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellIdentityTdscdma
import android.telephony.CellIdentityWcdma
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoTdscdma
import android.telephony.CellInfoWcdma
import android.telephony.SubscriptionInfo
import cz.mroczis.netmonster.core.model.cell.ICell
import java.util.UUID

/**
 * Cellular Network information
 */
class CellNetworkInfo(

    /**
     * Provider or sim operator name
     */
    val providerName: String,

    /**
     * Cell band information of current network
     */
    val band: CellBand?,

    /**
     * Detailed Cellular Network type - it can be aggregated type for more cells like 5G NSA (which contains 5G and 4G cells)
     */
    val networkType: MobileNetworkType,

    /**
     * Detailed Cell type for the current network to know particular technology of the cell, if network uses more than one subtechnologies, as for 5G NSA it could be 5G and 4G cells together
     */
    val cellType: CellTechnology,

    val mnc: Int?,

    val mcc: Int?,

    val locationId: Long?,

    val areaCode: Int?,

    val scramblingCode: Int?,

    val isRegistered: Boolean,

    val isActive: Boolean,

    val isRoaming: Boolean,

    val apn: String?,

    val signalStrength: SignalStrengthInfo?,

    val dualSimDetectionMethod: String?,

    /**
     * additional information about network status because we can have NR cell but it is for NR NSA mode (where is more often LTE cell available)
     */
    val nrConnectionState: NRConnectionState,

    /**
     * Random generated cell UUID
     */
    cellUUID: String,

    /**
     * Raw cellinfo provided by netmonster library
     */
    val rawCellInfo: ICell?,

    val isPrimaryDataSubscription: PrimaryDataSubscription?,

    val cellState: String?,

    val subscriptionsCount: Int,

    override val capabilitiesRaw: String?
) :
    NetworkInfo(TransportType.CELLULAR, cellUUID, capabilitiesRaw) {
    constructor(
        cellUUID: String
    ) : this(
        mcc = null,
        mnc = null,
        providerName = "",
        band = null,
        networkType = MobileNetworkType.UNKNOWN,
        cellType = CellTechnology.CONNECTION_UNKNOWN,
        isRegistered = false,
        isActive = false,
        isRoaming = false,
        nrConnectionState = NRConnectionState.NOT_AVAILABLE,
        scramblingCode = null,
        apn = null,
        signalStrength = null,
        dualSimDetectionMethod = null,
        rawCellInfo = null,
        cellUUID = cellUUID,
        locationId = null,
        areaCode = null,
        isPrimaryDataSubscription = PrimaryDataSubscription.UNKNOWN,
        capabilitiesRaw = "HARDCODED Capabilities 1: ${NetworkCapabilities.TRANSPORT_CELLULAR} networkType = ${MobileNetworkType.UNKNOWN}",
        cellState = null,
        subscriptionsCount = 0
    )

    override val name: String?
        get() = providerName
}

fun CellInfo.uuid(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        when (this) {
            is CellInfoLte -> cellIdentity.uuid()
            is CellInfoWcdma -> cellIdentity.uuid()
            is CellInfoGsm -> cellIdentity.uuid()
            is CellInfoTdscdma -> cellIdentity.uuid()
            is CellInfoCdma -> cellIdentity.uuid()
            is CellInfoNr -> (cellIdentity as CellIdentityNr).uuid()
            else -> throw IllegalArgumentException("Unknown cell info cannot be extracted ${javaClass.name}")
        }
    } else {
        when (this) {
            is CellInfoLte -> cellIdentity.uuid()
            is CellInfoWcdma -> cellIdentity.uuid()
            is CellInfoGsm -> cellIdentity.uuid()
            is CellInfoCdma -> cellIdentity.uuid()
            else -> throw IllegalArgumentException("Unknown cell info cannot be extracted ${javaClass.name}")
        }
    }
}

fun SubscriptionInfo.mccCompat(): Int? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        mccString?.toInt().fixValue()
    } else {
        mcc.fixValue()
    }

fun SubscriptionInfo.mncCompat(): Int? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        mncString?.toInt().fixValue()
    } else {
        mnc.fixValue()
    }

@RequiresApi(Build.VERSION_CODES.Q)
private fun CellIdentityNr.uuid(): String {
    val id = buildString {
        append("nr")
        append(nci)
        append(pci)
    }.toByteArray()
    return UUID.nameUUIDFromBytes(id).toString()
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun CellIdentityTdscdma.uuid(): String {
    val id = buildString {
        append("tdscdma")
        append(cid)
        append(cpid)
    }.toByteArray()
    return UUID.nameUUIDFromBytes(id).toString()
}

private fun CellIdentityLte.uuid(): String {
    val id = buildString {
        append("lte")
        append(ci)
        append(pci)
    }.toByteArray()
    return UUID.nameUUIDFromBytes(id).toString()
}

private fun CellIdentityWcdma.uuid(): String {
    val id = buildString {
        append("wcdma")
        append(cid)
    }.toByteArray()
    return UUID.nameUUIDFromBytes(id).toString()
}

private fun CellIdentityGsm.uuid(): String {
    val id = buildString {
        append("gsm")
        append(cid)
    }.toByteArray()
    return UUID.nameUUIDFromBytes(id).toString()
}

private fun CellIdentityCdma.uuid(): String {
    val id = buildString {
        append("cdma")
        append(networkId)
        append(systemId)
    }.toByteArray()
    return UUID.nameUUIDFromBytes(id).toString()
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun CellIdentityNr.mccCompat(): Int? = mccString?.toInt().fixValue()

@RequiresApi(Build.VERSION_CODES.Q)
private fun CellIdentityNr.mncCompat(): Int? = mncString?.toInt().fixValue()

@RequiresApi(Build.VERSION_CODES.Q)
private fun CellIdentityTdscdma.mccCompat(): Int? = mccString?.toInt().fixValue()

@RequiresApi(Build.VERSION_CODES.Q)
private fun CellIdentityTdscdma.mncCompat(): Int? = mncString?.toInt().fixValue()

private fun CellIdentityLte.mccCompat(): Int? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        mccString?.toInt().fixValue()
    } else {
        mcc.fixValue()
    }

private fun CellIdentityLte.mncCompat(): Int? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        mncString?.toInt().fixValue()
    } else {
        mnc.fixValue()
    }

private fun CellIdentityWcdma.mccCompat(): Int? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        mccString?.toInt().fixValue()
    } else {
        mcc.fixValue()
    }

private fun CellIdentityWcdma.mncCompat(): Int? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        mncString?.toInt().fixValue()
    } else {
        mnc.fixValue()
    }

private fun CellIdentityGsm.mccCompat(): Int? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        mccString?.toInt().fixValue()
    } else {
        mcc.fixValue()
    }

private fun CellIdentityGsm.mncCompat(): Int? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        mncString?.toInt().fixValue()
    } else {
        mnc.fixValue()
    }

fun Int?.fixValue(): Int? {
    return if (this == null || this == Int.MIN_VALUE || this == Int.MAX_VALUE || this < 0) {
        null
    } else {
        this
    }
}