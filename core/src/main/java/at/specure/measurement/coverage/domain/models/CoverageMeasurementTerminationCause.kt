package at.specure.measurement.coverage.domain.models

sealed class CoverageMeasurementTerminationCause(val cause: String) {
    class EndedByUser(): CoverageMeasurementTerminationCause("ended by user")
    class EndedByActiveSimChange(): CoverageMeasurementTerminationCause("ended by active sim change")
    class EndedByNetworkChange(): CoverageMeasurementTerminationCause("ended by network change")
    class EndedByTooManySignals(): CoverageMeasurementTerminationCause("ended by too many signals")
    class EndedByTooManyFences(): CoverageMeasurementTerminationCause("ended by too many fences")
    class EndedByTooManyGeolocations(): CoverageMeasurementTerminationCause("ended by too many geolocations")
    class EndedByMeasurementTimeExpired(): CoverageMeasurementTerminationCause("ended by measurement time expired")
    class EndedByMeasurementLoopTimeExpired(): CoverageMeasurementTerminationCause("ended by measurement loop time expired")
    class EndedByAirplaneModeEnabled(): CoverageMeasurementTerminationCause("ended by airplane mode enabled")
    class EndedByMobileDataDisabled(): CoverageMeasurementTerminationCause("ended by mobile data disabled")
    class EndedByBackOnMobileData(): CoverageMeasurementTerminationCause("ended by back on mobile data")
}