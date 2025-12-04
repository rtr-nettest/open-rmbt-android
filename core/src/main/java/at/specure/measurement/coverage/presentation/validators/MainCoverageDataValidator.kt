package at.specure.measurement.coverage.presentation.validators

import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.NetworkInfo
import at.specure.measurement.coverage.domain.validators.CoverageDataValidator
import at.specure.measurement.coverage.domain.validators.DurationValidator
import at.specure.measurement.coverage.domain.validators.LocationValidator
import at.specure.measurement.coverage.domain.validators.NetworkValidator
import at.specure.test.DeviceInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainCoverageDataValidator @Inject constructor(
    private val networkValidator: NetworkValidator,
    private val locationValidator: LocationValidator,
    private val durationValidator: DurationValidator,
): CoverageDataValidator {

    override fun areDataValidToSaveNewFence(
        newTimestamp: Long,
        newLocation: DeviceInfo.Location?,
        newNetworkInfo: NetworkInfo?,
        lastRecordedFenceRecord: CoverageMeasurementFenceRecord?
    ): Boolean {
        if (!locationValidator.isLocationValidAndDistantEnough(newLocation, lastRecordedFenceRecord?.location)) {
            return false
        }
        if (!networkValidator.isNetworkToBeLogged(newNetworkInfo)) {
            return false
        }
        if (!durationValidator.isMinimalTimePassed(newTimestamp, lastRecordedFenceRecord?.entryTimestampMillis)) {
            return false
        }
        return true
    }

    override fun areDataValidToReplaceSomeOldFence(
        newTimestamp: Long,
        newLocation: DeviceInfo.Location?,
        newNetworkInfo: NetworkInfo?,
        lastRecordedFenceRecord: CoverageMeasurementFenceRecord?
    ): Boolean {
        if (!locationValidator.isLocationFreshAndAccurate(newLocation)) {
            return false
        }
        if (!locationValidator.isTheSameLocation(newLocation, lastRecordedFenceRecord?.location)) {
            return false
        }
        if (!networkValidator.isNetworkToBeLogged(newNetworkInfo)) {
            return false
        }
        // TODO: ask if it is valid to check for replacing fences
        if (!durationValidator.isMinimalTimePassed(newTimestamp, lastRecordedFenceRecord?.entryTimestampMillis)) {
            return false
        }
        return true
    }

    // todo check if this is enough or make another checks
    override fun isNotTheSameMobileNetwork(oldNetwork: NetworkInfo?, newNetwork: NetworkInfo?): Boolean {
        if (oldNetwork == null) return true
        if (newNetwork == null) return true
        if (oldNetwork is CellNetworkInfo && newNetwork is CellNetworkInfo
            && oldNetwork.mcc == newNetwork.mcc
            && oldNetwork.mnc == newNetwork.mnc
            ) {
            return true
        }
        return false
    }

    override fun isBackToMobile(oldNetwork: NetworkInfo?, newNetwork: NetworkInfo?): Boolean {
        if (newNetwork == null) return false
        val newIsMobileNetwork = (newNetwork is CellNetworkInfo)
        val oldIsMobileNetwork = (oldNetwork != null && oldNetwork is CellNetworkInfo)
        return oldIsMobileNetwork.not() && newIsMobileNetwork
    }
}
