package at.specure.measurement.coverage.domain.models.state

enum class CoverageMeasurementState {
    // TODO: maybe add one more state like CREATED to know we already have session ready locally
    INITIALIZING, // user press start and there are logging values until response from server comes
    RUNNING, // we have response from server on start and ping is running network is fine
    PAUSED, // we have response from server on start, but condition for pausing is met
    FINISHED_LOOP_CORRECTLY // user stopped the measurement or maximum time is achieved
}