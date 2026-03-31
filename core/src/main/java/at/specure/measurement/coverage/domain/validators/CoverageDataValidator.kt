package at.specure.measurement.coverage.domain.validators

import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.info.network.NetworkInfo
import at.specure.test.DeviceInfo

interface CoverageDataValidator {
    fun isDataValidToSaveNewFence(
        newTimestamp: Long,
        newLocation: DeviceInfo.Location?,
        newNetworkInfo: NetworkInfo?,
        lastRecordedFenceRecord: CoverageMeasurementFenceRecord?
    ): Boolean

    fun isDataValidToFinishLastFence(
        newTimestamp: Long,
        newLocation: DeviceInfo.Location?,
        lastRecordedFenceRecord: CoverageMeasurementFenceRecord?
    ): Boolean

    fun areDataValidToReplaceSomeOldFence(
        newTimestamp: Long,
        newLocation: DeviceInfo.Location?,
        newNetworkInfo: NetworkInfo?,
        lastRecordedFenceRecord: CoverageMeasurementFenceRecord?
    ): Boolean

    fun isNotTheSameMobileNetwork(oldNetwork: NetworkInfo?, newNetwork: NetworkInfo?): Boolean

    fun isBackToMobile(oldNetwork: NetworkInfo?, newNetwork: NetworkInfo?): Boolean
}