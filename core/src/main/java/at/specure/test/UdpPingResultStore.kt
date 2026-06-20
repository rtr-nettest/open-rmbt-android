package at.specure.test

import at.rmbt.client.control.UdpPingBody
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory hand-off for UDP-ping samples collected during a measurement (prototype).
 *
 * The test controller fills it keyed by test UUID while the measurement runs; the result repository
 * drains it when building the /testresult body. This avoids a DB schema change while the backend is
 * not yet upgraded to receive the [UdpPingBody] array.
 */
object UdpPingResultStore {

    private val pings = ConcurrentHashMap<String, List<UdpPingBody>>()

    fun put(testUUID: String, results: List<UdpPingBody>) {
        if (results.isNotEmpty()) {
            pings[testUUID] = results
        }
    }

    /** Returns and removes the samples for [testUUID], or null if none were recorded. */
    fun take(testUUID: String): List<UdpPingBody>? = pings.remove(testUUID)
}
