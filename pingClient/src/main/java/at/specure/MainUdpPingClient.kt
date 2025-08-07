package at.specure

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


    fun main() = runBlocking {
        // Replace with your actual server details and token

        val configuration = PingClientConfiguration(
            port = 9999,
            host = "localhost",
            //port = 444,
            //host = "udpv4.netztest.at",
            token = "aJNB0gLXqMtM5xV7VBWslw==",
            protocolId = "RP01",
            pingIntervalMillis = 100,
            pingTimeoutMillis = 2000,
            successResponseHeader = "RR01",
            errorResponseHeader = "RE01"
        )

        val client = UdpPingClient(
            configuration
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
        println("Pinging stopped.")

    }

