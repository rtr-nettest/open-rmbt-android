package at.specure

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


//    fun main() = runBlocking {
//        val client = UdpPingClient("your_server_host", 12345, "your_secret_token")
//
//        val job = launch {
//            client.start().collect { result ->
//                when (result) {
//                    is PingResult.Success -> println("Ping success: seq=${result.sequenceNumber}, rtt=${result.rtt}ms")
//                    is PingResult.Lost -> println("Ping lost: seq=${result.sequenceNumber}")
//                    is PingResult.Error -> println("Ping error: seq=${result.sequenceNumber}, error=${result.exception.message}")
//                }
//            }
//        }
//
//        // Let the ping run for 10 seconds
//        delay(10000)
//
//        // Stop the pinging
//        client.stop()
//        job.cancelAndJoin()
//        println("Pinging stopped.")
//    }

    fun main() = runBlocking {
        // Replace with your actual server details and token
        val client = UdpPingClient(
            PingClientConfiguration(
                port = 444,
                host = "udpv4.netztest.at",
                token = "aJNB0gLXqMtM5xV7VBWslw==",
                protocolId = "RP01",
                pingIntervalMillis = 100,
                pingTimeoutMillis = 2000,
                headerSizeBytes = 8,
                successResponseHeader = "RR01",
                errorResponseHeader = "RE01"
            )
        )

        println("Starting UDP ping...")
        val job = launch {
            client.start().collect { result ->
                when (result) {
                    is PingResult.Success -> {
                        println("✅ Ping success: seq=${result.sequenceNumber}, rtt=${result.rttMillis}ms")
                    }
                    is PingResult.Lost -> {
                        println("❌ Ping lost: seq=${result.sequenceNumber} (timeout)")
                    }
                    is PingResult.ServerError -> {
                        println("❗ Server error for seq=${result.sequenceNumber}")
                    }
                    is PingResult.ClientError -> {
                        println("🔥 Client error for seq=${result.sequenceNumber}, exception: ${result.exception.message}")
                    }
                }
            }
        }

        // Let the ping run for 10 seconds as an example
        delay(10000)

        // Stop the pinging process
        println("\nStopping ping client...")
        client.stop()
        job.cancelAndJoin() // Ensure the collecting coroutine also finishes
        println("Pinging stopped.")
    }

