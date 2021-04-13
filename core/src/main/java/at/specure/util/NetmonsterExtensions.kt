package at.specure.util

import at.rtr.rmbt.util.BandCalculationUtil
import at.specure.data.entity.CellInfoRecord
import at.specure.info.TransportType
import at.specure.info.cell.CellTechnology
import at.specure.info.network.MobileNetworkType
import cz.mroczis.netmonster.core.INetMonster
import cz.mroczis.netmonster.core.db.model.NetworkType
import cz.mroczis.netmonster.core.model.band.BandGsm
import cz.mroczis.netmonster.core.model.band.BandLte
import cz.mroczis.netmonster.core.model.band.BandNr
import cz.mroczis.netmonster.core.model.band.BandTdscdma
import cz.mroczis.netmonster.core.model.band.BandWcdma
import cz.mroczis.netmonster.core.model.cell.CellCdma
import cz.mroczis.netmonster.core.model.cell.CellGsm
import cz.mroczis.netmonster.core.model.cell.CellLte
import cz.mroczis.netmonster.core.model.cell.CellNr
import cz.mroczis.netmonster.core.model.cell.CellTdscdma
import cz.mroczis.netmonster.core.model.cell.CellWcdma
import cz.mroczis.netmonster.core.model.cell.ICell
import cz.mroczis.netmonster.core.model.connection.PrimaryConnection
import timber.log.Timber
import java.util.UUID

fun ICell.toCellInfoRecord(testUUID: String, netMonster: INetMonster) = CellInfoRecord(
    testUUID = testUUID,
    uuid = this.uuid(),
    isActive = this.connectionStatus is PrimaryConnection,
    cellTechnology = CellTechnology.fromMobileNetworkType(this.mobileNetworkType(netMonster)),
    transportType = TransportType.CELLULAR,
    registered = this.connectionStatus is PrimaryConnection,
    areaCode = areaCode(),
    channelNumber = channelNumber(),
    frequency = frequency(),
    locationId = locationId(),
    mcc = this.network?.mcc?.toIntOrNull(),
    mnc = this.network?.mnc?.toIntOrNull(),
    primaryScramblingCode = primaryScramblingCode(),
    dualSimDetectionMethod = "NOT_AVAILABLE" // TODO it is for local purpose only
)

fun ICell.channelNumber(): Int? {
    return when (band) {
        is BandNr,
        is BandGsm,
        is BandLte,
        is BandTdscdma,
        is BandWcdma -> band?.channelNumber
        else -> null
    }
}

fun ICell.frequency(): Double? {
    return when (band) {
        is BandNr -> (band as BandNr).downlinkFrequency.toDouble()
        is BandGsm -> BandCalculationUtil.getBandFromArfcn((band as BandGsm).arfcn).frequencyDL
        is BandLte -> BandCalculationUtil.getBandFromEarfcn((band as BandLte).downlinkEarfcn).frequencyDL
        is BandTdscdma -> BandCalculationUtil.getBandFromUarfcn((band as BandTdscdma).downlinkUarfcn).frequencyDL
        is BandWcdma -> BandCalculationUtil.getBandFromUarfcn((band as BandWcdma).downlinkUarfcn).frequencyDL
        else -> null
    }
}

fun ICell.mobileNetworkType(netMonster: INetMonster): MobileNetworkType {
    var networkTypeFromNM: NetworkType? = null
    try {
        networkTypeFromNM = netMonster.getNetworkType(this.subscriptionId)
    } catch (e: SecurityException) {
        Timber.e("SecurityException: Not able to read network type")
    } catch (e: IllegalStateException) {
        Timber.e("IllegalStateException: Not able to read network type")
    }
    return networkTypeFromNM?.mapToMobileNetworkType() ?: MobileNetworkType.UNKNOWN
}

fun NetworkType.mapToMobileNetworkType(): MobileNetworkType {
    return when (this) {
        is NetworkType.Cdma,
        is NetworkType.Gsm,
        is NetworkType.Lte,
        is NetworkType.Nr,
        is NetworkType.Tdscdma,
        is NetworkType.Wcdma -> MobileNetworkType.fromValue(this.technology)
        else -> MobileNetworkType.UNKNOWN
    }
}

fun ICell.areaCode(): Int? {
    return when (this) {
        is CellNr -> this.tac
        is CellTdscdma -> this.lac
        is CellLte -> this.tac
        is CellCdma -> this.bid
        is CellWcdma -> this.lac
        is CellGsm -> this.lac
        else -> null
    }
}

fun ICell.primaryScramblingCode(): Int? {
    return when (this) {
        is CellNr -> this.pci
        is CellTdscdma -> null
        is CellLte -> this.pci
        is CellCdma -> null
        is CellWcdma -> this.psc
        is CellGsm -> this.bsic
        else -> null
    }
}

fun ICell.locationId(): Int? {
    return when (this) {
        is CellNr -> null
        is CellTdscdma -> this.cid
        is CellLte -> this.cid
        is CellCdma -> this.bid
        is CellWcdma -> this.cid
        is CellGsm -> this.cid
        else -> null
    }
}

fun ICell.uuid(): String {
    return when (this) {
        is CellNr -> this.uuid()
        is CellTdscdma -> this.uuid()
        is CellLte -> this.uuid()
        is CellCdma -> this.uuid()
        is CellWcdma -> this.uuid()
        is CellGsm -> this.uuid()
        else -> ""
    }
}

fun CellNr.uuid(): String {
    val id = buildString {
        append("nr")
        append(nci)
        append(pci)
    }.toByteArray()
    return UUID.nameUUIDFromBytes(id).toString()
}

fun CellTdscdma.uuid(): String {
    val id = buildString {
        append("tdscdma")
        append(cid)
        append(cpid)
    }.toByteArray()
    return UUID.nameUUIDFromBytes(id).toString()
}

fun CellLte.uuid(): String {
    val id = buildString {
        append("lte")
        append(eci)
        append(pci)
    }.toByteArray()
    return UUID.nameUUIDFromBytes(id).toString()
}

fun CellWcdma.uuid(): String {
    val id = buildString {
        append("wcdma")
        append(cid)
    }.toByteArray()
    return UUID.nameUUIDFromBytes(id).toString()
}

fun CellGsm.uuid(): String {
    val id = buildString {
        append("gsm")
        append(cid)
    }.toByteArray()
    return UUID.nameUUIDFromBytes(id).toString()
}

fun CellCdma.uuid(): String {
    val id = buildString {
        append("cdma")
        append(bid)
        append(sid)
    }.toByteArray()
    return UUID.nameUUIDFromBytes(id).toString()
}