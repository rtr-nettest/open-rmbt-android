package at.specure.measurement.coverage.domain.validators

import at.specure.data.entity.CoverageMeasurementFenceRecord
import at.specure.info.network.NetworkInfo
import at.specure.test.DeviceInfo

interface CoverageDataValidator {
    fun areDataValidToSaveNewFence(
        newTimestamp: Long,
        newLocation: DeviceInfo.Location?,
        newNetworkInfo: NetworkInfo?,
        lastRecordedFenceRecord: CoverageMeasurementFenceRecord?
    ): Boolean
}