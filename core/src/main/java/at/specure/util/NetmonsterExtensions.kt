package at.specure.util

import android.os.SystemClock
import at.rtr.rmbt.util.BandCalculationUtil
import at.specure.data.entity.CellInfoRecord
import at.specure.data.entity.CellLocationRecord
import at.specure.data.entity.SignalRecord
import at.specure.info.TransportType
import at.specure.info.cell.CellTechnology
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NRConnectionState
import at.specure.info.strength.SignalSource
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
import cz.mroczis.netmonster.core.model.signal.ISignal
import cz.mroczis.netmonster.core.model.signal.SignalGsm
import cz.mroczis.netmonster.core.model.signal.SignalLte
import cz.mroczis.netmonster.core.model.signal.SignalNr
import timber.log.Timber
import java.util.UUID

fun List<ICell>.filterOnlyActiveDataCell(dataSubscriptionId: Int): List<ICell> {
    return this.filter {
        // when there is -1 we will report both sims signal because we are unable to detect correct data subscription
        val isFromDataSubscription = dataSubscriptionId == -1 || it.subscriptionId == dataSubscriptionId
        val isPrimaryCell = it.connectionStatus is PrimaryConnection
        isFromDataSubscription && isPrimaryCell
    }
}

fun ISignal.toSignalRecord(
    testUUID: String,
    cellUUID: String,
    mobileNetworkType: MobileNetworkType,
    testStartTimeNanos: Long,
    nrConnectionState: NRConnectionState
): SignalRecord {

    val currentTime = System.nanoTime()
    var signal = this.dbm
    val wifiLinkSpeed: Int? = null
    // 2G/3G
    var bitErrorRate: Int? = null
    // 4G
    var lteRsrp: Int? = null
    var lteRsrq: Int? = null
    var lteRssnr: Int? = null
    var lteCqi: Int? = null
    var timingAdvance: Int? = null

    var nrCsiRsrp: Int? = null
    var nrCsiRsrq: Int? = null
    var nrCsiSinr: Int? = null
    var nrSsRsrp: Int? = null
    var nrSsRsrq: Int? = null
    var nrSsSinr: Int? = null

    when (this) {
        is SignalNr -> {
            nrCsiRsrp = this.csiRsrp
            nrCsiRsrq = this.csiRsrq
            nrCsiSinr = this.csiSinr
            nrSsRsrp = this.ssRsrp
            nrSsRsrq = this.ssRsrq
            nrSsSinr = this.ssSinr
        }
        is SignalLte -> {
            signal = null
            lteRsrp = this.rsrp?.toInt()
            lteRsrq = this.rsrq?.toInt()
            lteRssnr = this.snr?.toInt()
            lteCqi = this.cqi
            timingAdvance = this.timingAdvance
        }
        is SignalGsm -> {
            bitErrorRate = this.bitErrorRate
            timingAdvance = this.timingAdvance
        }
    }

    Timber.e("Signal saving time 1: $currentTime  starting time: $testStartTimeNanos   current time: ${System.nanoTime()}")
    val startTimestampNsSinceBoot = testStartTimeNanos + (SystemClock.elapsedRealtimeNanos() - System.nanoTime())
    val timeNanos = currentTime - testStartTimeNanos
    var timeNanosLast = if (currentTime < startTimestampNsSinceBoot) currentTime - startTimestampNsSinceBoot else null
    if (timeNanosLast == 0L) {
        timeNanosLast = null
    }

    return SignalRecord(
        testUUID = testUUID,
        cellUuid = cellUUID,
        signal = signal,
        wifiLinkSpeed = wifiLinkSpeed,
        timeNanos = timeNanos,
        timeNanosLast = timeNanosLast,
        bitErrorRate = bitErrorRate,
        lteRsrp = lteRsrp,
        lteRsrq = lteRsrq,
        lteRssnr = lteRssnr,
        lteCqi = lteCqi,
        timingAdvance = timingAdvance,
        mobileNetworkType = mobileNetworkType,
        transportType = TransportType.CELLULAR,
        nrConnectionState = nrConnectionState,
        nrCsiRsrp = nrCsiRsrp,
        nrCsiRsrq = nrCsiRsrq,
        nrCsiSinr = nrCsiSinr,
        nrSsRsrp = nrSsRsrp,
        nrSsRsrq = nrSsRsrq,
        nrSsSinr = nrSsSinr,
        source = SignalSource.NM_CELL_INFO
    )
}

fun ICell.toCellLocation(testUUID: String, timestampMillis: Long, timestampNanos: Long, startTimeNanos: Long): CellLocationRecord? {
    primaryScramblingCode()?.let {
        return CellLocationRecord(
            testUUID = testUUID,
            scramblingCode = it,
            areaCode = this.areaCode(),
            locationId = this.locationId(),
            timestampNanos = timestampNanos - startTimeNanos,
            timestampMillis = timestampMillis
        )
    }
    return null
}

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