package at.specure.measurement.coverage.domain.monitors

interface ConnectivityMonitor {

    fun start(
        onAirplaneEnabled: () -> Unit,
        onAirplaneDisabled: () -> Unit,
        onMobileDataEnabled: () -> Unit,
        onMobileDataDisabled: () -> Unit,
        onIpAddressChanged: (ipAddress: String?) -> Unit,
        onVpnStateChanged: (vpnActive: Boolean) -> Unit
    )

    fun stop()

    fun isAirplaneModeCurrentlyEnabled(): Boolean

    fun isMobileDataEnabled(): Boolean

    fun getCurrentIpAddress(): String?
}