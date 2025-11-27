package at.specure.measurement.coverage.domain.monitors

interface ConnectivityMonitor {

    fun start(
        onAirplaneEnabled: () -> Unit,
        onAirplaneDisabled: () -> Unit,
        onMobileDataEnabled: () -> Unit,
        onMobileDataDisabled: () -> Unit
    )

    fun stop()

    fun isAirplaneModeCurrentlyEnabled(): Boolean
}