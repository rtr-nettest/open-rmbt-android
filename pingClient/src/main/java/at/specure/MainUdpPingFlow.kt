package at.specure

import at.specure.client.PingClientConfiguration
import at.specure.client.PingResult
import at.specure.client.UdpPingFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking


fun main() = runBlocking {
    // Replace with your actual host and port
    val configuration = PingClientConfiguration(
//        port = 9999,
//        host = "localhost",
        port = 444,
        host = "udpv4.netztest.at",
        token = "aJR01et3O7X6p6FjiCwBCw==",
        protocolId = "RP01",
        pingIntervalMillis = 100,
        pingTimeoutMillis = 2000,
        successResponseHeader = "RR01",
        errorResponseHeader = "RE01"
    )

    val pinger = UdpPingFlow(configuration)

    println("Starting UDP ping... (press Ctrl+C to stop)")

    pinger.pingFlow()
        .onEach { result ->
            when (result) {
                is PingResult.Success -> println("Ping success: seq=${result.sequenceNumber}, rtt=${result.rttMillis} ms")
                is PingResult.Lost -> println("Ping lost: seq=${result.sequenceNumber}")
                is PingResult.ClientError -> println("Client error: seq=${result.sequenceNumber}, error=${result.exception}")
                is PingResult.ServerError -> println("Server error: seq=${result.sequenceNumber}")
            }
        }
        .catch { e -> println("Flow error: $e") }
        .launchIn(this) // Run it inside the coroutine scope of runBlocking

    // Keep the app alive for testing
    delay(Long.MAX_VALUE)
}


//
//private var job: Job? = null
//fun start(scope: CoroutineScope, host: String, port: Int, token: String, onResult: (PingResult) -> Unit) {
//    if (job?.isActive == true) return // Already running
//
//    val pinger = UdpPingFlow(host, port, token)
//    job = scope.launch {
//        pinger.pingFlow()
//            .collect { result ->
//                onResult(result)
//            }
//    }
//}
//fun stop() {
//    job?.cancel()
//    job = null
//}
