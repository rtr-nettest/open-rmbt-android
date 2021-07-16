package at.specure.util

import android.Manifest
import android.os.SystemClock
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import at.rtr.rmbt.util.BandCalculationUtil
import at.specure.data.entity.CellInfoRecord
import at.specure.data.entity.CellLocationRecord
import at.specure.data.entity.SignalRecord
import at.specure.info.TransportType
import at.specure.info.band.CellBand
import at.specure.info.band.CellBand.Companion.fromChannelNumber
import at.specure.info.cell.CellChannelAttribution
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.cell.CellTechnology
import at.specure.info.network.MobileNetworkType
import at.specure.info.network.NRConnectionState
import at.specure.info.strength.SignalSource
import at.specure.info.strength.SignalStrengthInfo
import at.specure.info.strength.SignalStrengthInfo.Companion.CELLULAR_SIGNAL_MAX
import at.specure.info.strength.SignalStrengthInfo.Companion.CELLULAR_SIGNAL_MIN
import at.specure.info.strength.SignalStrengthInfo.Companion.LTE_RSRP_SIGNAL_MAX
import at.specure.info.strength.SignalStrengthInfo.Companion.LTE_RSRP_SIGNAL_MIN
import at.specure.info.strength.SignalStrengthInfo.Companion.NR_RSRP_SIGNAL_MAX
import at.specure.info.strength.SignalStrengthInfo.Companion.NR_RSRP_SIGNAL_MIN
import at.specure.info.strength.SignalStrengthInfo.Companion.TDSCDMA_RSRP_SIGNAL_MAX
import at.specure.info.strength.SignalStrengthInfo.Companion.TDSCDMA_RSRP_SIGNAL_MIN
import at.specure.info.strength.SignalStrengthInfo.Companion.WCDMA_RSRP_SIGNAL_MAX
import at.specure.info.strength.SignalStrengthInfo.Companion.WCDMA_RSRP_SIGNAL_MIN
import at.specure.info.strength.SignalStrengthInfo.Companion.calculateCellSignalLevel
import at.specure.info.strength.SignalStrengthInfo.Companion.calculateNRSignalLevel
import at.specure.info.strength.SignalStrengthInfoCommon
import at.specure.info.strength.SignalStrengthInfoGsm
import at.specure.info.strength.SignalStrengthInfoLte
import at.specure.info.strength.SignalStrengthInfoNr
import cz.mroczis.netmonster.core.INetMonster
import cz.mroczis.netmonster.core.db.model.NetworkType
import cz.mroczis.netmonster.core.model.band.BandGsm
import cz.mroczis.netmonster.core.model.band.BandLte
import cz.mroczis.netmonster.core.model.band.BandNr
import cz.mroczis.netmonster.core.model.band.BandTdscdma
import cz.mroczis.netmonster.core.model.band.BandWcdma
import cz.mroczis.netmonster.core.model.band.IBand
import cz.mroczis.netmonster.core.model.cell.CellCdma
import cz.mroczis.netmonster.core.model.cell.CellGsm
import cz.mroczis.netmonster.core.model.cell.CellLte
import cz.mroczis.netmonster.core.model.cell.CellNr
import cz.mroczis.netmonster.core.model.cell.CellTdscdma
import cz.mroczis.netmonster.core.model.cell.CellWcdma
import cz.mroczis.netmonster.core.model.cell.ICell
import cz.mroczis.netmonster.core.model.connection.PrimaryConnection
import cz.mroczis.netmonster.core.model.signal.ISignal
import cz.mroczis.netmonster.core.model.signal.SignalCdma
import cz.mroczis.netmonster.core.model.signal.SignalGsm
import cz.mroczis.netmonster.core.model.signal.SignalLte
import cz.mroczis.netmonster.core.model.signal.SignalNr
import cz.mroczis.netmonster.core.model.signal.SignalTdscdma
import cz.mroczis.netmonster.core.model.signal.SignalWcdma
import cz.mroczis.netmonster.core.telephony.ITelephonyManagerCompat
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

fun ISignal.toSignalStrengthInfo(timestampNanos: Long): SignalStrengthInfo? {
    return when (this) {
        is SignalNr -> {
            this.toSignalStrengthInfo(timestampNanos)
        }
        is SignalLte -> {
            this.toSignalStrengthInfo(timestampNanos)
        }
        is SignalGsm -> {
            this.toSignalStrengthInfo(timestampNanos)
        }
        is SignalCdma -> {
            this.toSignalStrengthInfo(timestampNanos)
        }
        is SignalWcdma -> {
            this.toSignalStrengthInfo(timestampNanos)
        }
        is SignalTdscdma -> {
            this.toSignalStrengthInfo(timestampNanos)
        }
        else -> null
    }
}

fun SignalTdscdma.toSignalStrengthInfo(timestampNanos: Long): SignalStrengthInfo {
    return SignalStrengthInfoCommon(
        transport = TransportType.CELLULAR,
        value = this.dbm,
        rsrq = null,
        signalLevel = calculateCellSignalLevel(this.dbm, TDSCDMA_RSRP_SIGNAL_MIN, TDSCDMA_RSRP_SIGNAL_MAX),
        min = TDSCDMA_RSRP_SIGNAL_MIN,
        max = TDSCDMA_RSRP_SIGNAL_MAX,
        timestampNanos = timestampNanos,
        source = SignalSource.NM_CELL_INFO
    )
}

fun SignalCdma.toSignalStrengthInfo(timestampNanos: Long): SignalStrengthInfo {
    return SignalStrengthInfoCommon(
        transport = TransportType.CELLULAR,
        value = this.dbm,
        rsrq = null,
        signalLevel = calculateCellSignalLevel(this.dbm, WCDMA_RSRP_SIGNAL_MIN, WCDMA_RSRP_SIGNAL_MAX),
        min = WCDMA_RSRP_SIGNAL_MIN,
        max = WCDMA_RSRP_SIGNAL_MAX,
        timestampNanos = timestampNanos,
        source = SignalSource.NM_CELL_INFO
    )
}

fun SignalWcdma.toSignalStrengthInfo(timestampNanos: Long): SignalStrengthInfo {
    return SignalStrengthInfoCommon(
        transport = TransportType.CELLULAR,
        value = this.dbm,
        rsrq = null,
        signalLevel = calculateCellSignalLevel(this.dbm, WCDMA_RSRP_SIGNAL_MIN, WCDMA_RSRP_SIGNAL_MAX),
        min = WCDMA_RSRP_SIGNAL_MIN,
        max = WCDMA_RSRP_SIGNAL_MAX,
        timestampNanos = timestampNanos,
        source = SignalSource.NM_CELL_INFO
    )
}

fun SignalGsm.toSignalStrengthInfo(timestampNanos: Long): SignalStrengthInfo {
    return SignalStrengthInfoGsm(
        transport = TransportType.CELLULAR,
        value = this.dbm,
        rsrq = null,
        signalLevel = calculateCellSignalLevel(this.dbm, CELLULAR_SIGNAL_MIN, CELLULAR_SIGNAL_MAX),
        min = CELLULAR_SIGNAL_MIN,
        max = CELLULAR_SIGNAL_MAX,
        timestampNanos = timestampNanos,
        source = SignalSource.NM_CELL_INFO,
        bitErrorRate = this.bitErrorRate,
        timingAdvance = this.timingAdvance
    )
}

fun SignalLte.toSignalStrengthInfo(timestampNanos: Long): SignalStrengthInfo {
    val signalValue = this.rsrp?.toInt()
    return SignalStrengthInfoLte(
        transport = TransportType.CELLULAR,
        value = signalValue,
        rsrq = this.rsrq?.toInt(),
        signalLevel = calculateCellSignalLevel(signalValue, LTE_RSRP_SIGNAL_MIN, LTE_RSRP_SIGNAL_MAX),
        min = LTE_RSRP_SIGNAL_MIN,
        max = LTE_RSRP_SIGNAL_MAX,
        timestampNanos = timestampNanos,
        source = SignalSource.NM_CELL_INFO,
        cqi = this.cqi,
        rsrp = this.rsrp?.toInt(),
        rssi = this.rssi,
        rssnr = this.snr?.toInt(),
        timingAdvance = this.timingAdvance
    )
}

fun SignalNr.toSignalStrengthInfo(timestampNanos: Long): SignalStrengthInfo {
    return SignalStrengthInfoNr(
        transport = TransportType.CELLULAR,
        value = this.ssRsrp,
        rsrq = null,
        signalLevel = calculateNRSignalLevel(this.ssRsrp),
        min = NR_RSRP_SIGNAL_MIN,
        max = NR_RSRP_SIGNAL_MAX,
        timestampNanos = timestampNanos,
        source = SignalSource.NM_CELL_INFO,
        csiRsrp = this.csiRsrp,
        csiRsrq = this.csiRsrq,
        csiSinr = this.csiSinr,
        ssRsrp = this.ssRsrp,
        ssRsrq = this.ssRsrq,
        ssSinr = this.ssSinr
    )
}

fun ICell.toSignalStrengthInfo(timestampNanos: Long): SignalStrengthInfo? {
    val iSignal: ISignal? = this.signal
    return if (iSignal != null) {
        signal?.toSignalStrengthInfo(timestampNanos)
    } else {
        null
    }
}

@RequiresPermission(allOf = [Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_COARSE_LOCATION])
fun ICell.toCellNetworkInfo(
    apn: String?,
    dataTelephonyManager: TelephonyManager?,
    telephonyManagerNetmonster: ITelephonyManagerCompat,
    netMonster: INetMonster
): CellNetworkInfo {
    return CellNetworkInfo(
        providerName = dataTelephonyManager?.networkOperatorName ?: telephonyManagerNetmonster.getNetworkOperator()?.toPlmn("-") ?: "",
        band = this.band?.toCellBand(),
        networkType = this.mobileNetworkType(netMonster),
        mnc = this.network?.mnc?.toIntOrNull(),
        mcc = this.network?.mcc?.toIntOrNull(),
        locationId = this.locationId(),
        areaCode = this.areaCode(),
        scramblingCode = this.primaryScramblingCode(),
        isRegistered = this.connectionStatus is PrimaryConnection,
        isActive = this.connectionStatus is PrimaryConnection,
        apn = apn ?: "",
        isRoaming = dataTelephonyManager?.isNetworkRoaming ?: false,
        signalStrength = this.toSignalStrengthInfo(System.nanoTime()),
        nrConnectionState = if (dataTelephonyManager != null) {
            NRConnectionState.getNRConnectionState(dataTelephonyManager)
        } else NRConnectionState.NOT_AVAILABLE,
        dualSimDetectionMethod = null,
        cellUUID = this.uuid()
    )
}

fun IBand.toCellBand(): CellBand? {
    return when (this) {
        is BandNr -> this.toCellBand()
        is BandTdscdma -> this.toCellBand()
        is BandLte -> this.toCellBand()
        is BandWcdma -> this.toCellBand()
        is BandGsm -> this.toCellBand()
        else -> null
    }
}

fun BandNr.toCellBand(): CellBand {
    val legacyCellBand = fromChannelNumber(this.channelNumber, CellChannelAttribution.NRARFCN)
    return CellBand(
        frequencyDL = this.downlinkFrequency.toDouble(),
        band = this.number ?: legacyCellBand?.band ?: -1,
        channel = this.channelNumber,
        name = this.name,
        channelAttribution = CellChannelAttribution.NRARFCN,
        frequencyUL = legacyCellBand?.frequencyUL ?: -1.0
    )
}

fun BandLte.toCellBand(): CellBand {
    val legacyCellBand = fromChannelNumber(this.channelNumber, CellChannelAttribution.EARFCN)
    return CellBand(
        frequencyDL = legacyCellBand?.frequencyDL ?: -1.0,
        band = this.number ?: -1,
        channel = this.channelNumber,
        name = this.name,
        channelAttribution = CellChannelAttribution.EARFCN,
        frequencyUL = legacyCellBand?.frequencyUL ?: -1.0
    )
}

fun BandWcdma.toCellBand(): CellBand {
    val legacyCellBand = fromChannelNumber(this.channelNumber, CellChannelAttribution.UARFCN)
    return CellBand(
        frequencyDL = legacyCellBand?.frequencyDL ?: -1.0,
        band = this.number ?: -1,
        channel = this.channelNumber,
        name = this.name,
        channelAttribution = CellChannelAttribution.UARFCN,
        frequencyUL = legacyCellBand?.frequencyUL ?: -1.0
    )
}

fun BandTdscdma.toCellBand(): CellBand {
    val legacyCellBand = fromChannelNumber(this.channelNumber, CellChannelAttribution.UARFCN)
    return CellBand(
        frequencyDL = legacyCellBand?.frequencyDL ?: -1.0,
        band = this.number ?: -1,
        channel = this.channelNumber,
        name = this.name,
        channelAttribution = CellChannelAttribution.UARFCN,
        frequencyUL = legacyCellBand?.frequencyUL ?: -1.0
    )
}

fun BandGsm.toCellBand(): CellBand {
    val legacyCellBand = fromChannelNumber(this.channelNumber, CellChannelAttribution.ARFCN)
    return CellBand(
        frequencyDL = legacyCellBand?.frequencyDL ?: -1.0,
        band = this.number ?: -1,
        channel = this.channelNumber,
        name = this.name,
        channelAttribution = CellChannelAttribution.ARFCN,
        frequencyUL = legacyCellBand?.frequencyUL ?: -1.0
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

/**
 * Verify if the signal and cell information are in the same technology class, because when network changes from eg. 4G to 3G
 * there is cell technology marked as 3G and signal is LTE
 */
fun ICell.isInformationCorrect(cellTechnology: CellTechnology): Boolean {
    val isCorrect = when (this) {
        is CellNr -> cellTechnology == CellTechnology.CONNECTION_5G
        is CellLte -> cellTechnology == CellTechnology.CONNECTION_4G_5G || cellTechnology == CellTechnology.CONNECTION_5G || cellTechnology == CellTechnology.CONNECTION_4G
        is CellWcdma -> cellTechnology == CellTechnology.CONNECTION_3G
        is CellTdscdma -> cellTechnology == CellTechnology.CONNECTION_3G
        is CellCdma -> cellTechnology == CellTechnology.CONNECTION_2G
        is CellGsm -> cellTechnology == CellTechnology.CONNECTION_2G
        else -> false
    }
    Timber.d("Saving ICell: $this and  technology: ${cellTechnology?.name} is Correct: $isCorrect")
    return isCorrect
}

@Synchronized
fun ICell.toCellInfoRecord(testUUID: String, netMonster: INetMonster): CellInfoRecord? {
    val cellTechnologyFromNetworkType = CellTechnology.fromMobileNetworkType(this.mobileNetworkType(netMonster))
    // for 4G cells we want to have it marked as 4G not 5G also in case when it is NR_NSA
    val cellTechnology = if (this is CellLte && cellTechnologyFromNetworkType == CellTechnology.CONNECTION_5G) {
        CellTechnology.CONNECTION_4G
    } else {
        cellTechnologyFromNetworkType
    }
    cellTechnology?.let {
        if (isInformationCorrect(cellTechnology)) {
            val cellInfoRecord = CellInfoRecord(
                testUUID = testUUID,
                uuid = this.uuid(),
                isActive = this.connectionStatus is PrimaryConnection,
                cellTechnology = cellTechnology,
                transportType = TransportType.CELLULAR,
                registered = this.connectionStatus is PrimaryConnection,
                areaCode = areaCode(),
                channelNumber = channelNumber(),
                frequency = frequency(),
                locationId = locationId(),
                mcc = this.network?.mcc?.toIntOrNull(),
                mnc = this.network?.mnc?.toIntOrNull(),
                primaryScramblingCode = primaryScramblingCode(),
                dualSimDetectionMethod = "NOT_AVAILABLE" // local purpose only
            )
            return cellInfoRecord
        }
    }
    return null
}

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
        is BandGsm -> BandCalculationUtil.getBandFromArfcn((band as BandGsm).arfcn)?.frequencyDL
        is BandLte -> BandCalculationUtil.getBandFromEarfcn((band as BandLte).downlinkEarfcn)?.frequencyDL
        is BandTdscdma -> BandCalculationUtil.getBandFromUarfcn((band as BandTdscdma).downlinkUarfcn)?.frequencyDL
        is BandWcdma -> BandCalculationUtil.getBandFromUarfcn((band as BandWcdma).downlinkUarfcn)?.frequencyDL
        else -> null
    }
}

fun ICell.mobileNetworkType(netMonster: INetMonster): MobileNetworkType {
    var networkTypeFromNM: NetworkType? = null
    try {
        networkTypeFromNM = netMonster.getNetworkType(this.subscriptionId)
//        Timber.d("NM network type direct: ${networkTypeFromNM.technology}")
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
        is CellLte -> this.eci
        is CellCdma -> this.bid
        is CellWcdma -> this.ci
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