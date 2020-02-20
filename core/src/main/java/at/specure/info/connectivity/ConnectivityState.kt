package at.specure.info.connectivity

import androidx.annotation.Keep

@Keep
enum class ConnectivityState {
    ON_AVAILABLE,
    ON_CAPABILITIES_CHANGED,
    ON_LINK_PROPERTIES_CHANGED,
    ON_LOSING,
    ON_LOST,
    ON_UNAVAILABLE
}

@Keep
data class ConnectivityStateBundle(val state: ConnectivityState, val timeNanos: Long, val message: String?)