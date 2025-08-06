package at.specure

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.DatagramChannel
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class UdpPingClient(
    private val configuration: PingClientConfiguration,
) {
    // Companion object for constants


    private val sequenceNumber = AtomicInteger(Random.nextInt())
    private var pingJob: Job? = null

    /**
     * Starts the ping process.
     * @return A Flow that emits a [PingResult] for each attempt.
     *         The flow runs indefinitely until the scope is cancelled or [@see stop] is called.
     */
    fun start(): Flow<PingResult> = channelFlow {
        pingJob = CoroutineScope(Dispatchers.IO).launch {
            // Using try-with-resources to ensure the channel is closed
            DatagramChannel.open().use { channel ->
                channel.configureBlocking(false)
                channel.connect(InetSocketAddress(configuration.host, configuration.port))

                println("Starting Ping client...")

                // Flow to emit every 100ms
                val ticker = flow {
                    while (true) {
                        emit(Unit)
                        delay(configuration.pingIntervalMillis)
                    }
                }

                ticker.collect {
                    val currentSequenceNumber = sequenceNumber.getAndIncrement()
                    val requestPacket = createPingPacket(currentSequenceNumber)

                    try {
                        val startTimeNanos = System.nanoTime()
                        channel.write(requestPacket)

                        val response = withTimeoutOrNull(configuration.pingTimeoutMillis) {
                            receiveResponse(channel, currentSequenceNumber, startTimeNanos)
                        }

                        val result = when (response) {
                            is ServerResponse.Success -> PingResult.Success(response.sequenceNumber, response.rttMillis)
                            is ServerResponse.Error -> PingResult.ServerError(response.sequenceNumber)
                            null -> PingResult.Lost(currentSequenceNumber)
                        }
                        send(result)

                    } catch (e: Exception) {
                        // Catches network errors, e.g., if the host is unreachable
                        println(e)
                        send(PingResult.ClientError(currentSequenceNumber, e))
                    }
                }
            }
        }
    }

    /**
     * Stops the ping process gracefully.
     */
    fun stop() {
        println("Stopping Ping client...")
        pingJob?.cancel()
    }

    private fun createPingPacket(sequenceNumber: Int): ByteBuffer {
        val protocolIdBytes = configuration.protocolId.toByteArray(Charsets.US_ASCII)
        val tokenBytes = configuration.token.toByteArray(Charsets.US_ASCII)
        // Allocate buffer and set byte order to big-endian (network standard)
        val buffer = ByteBuffer.allocate(protocolIdBytes.size + Int.SIZE_BYTES + tokenBytes.size)
            .order(ByteOrder.BIG_ENDIAN)

        buffer.put(protocolIdBytes)
        buffer.putInt(sequenceNumber)
        buffer.put(tokenBytes)
        buffer.flip()
        return buffer
    }

    private suspend fun receiveResponse(
        channel: DatagramChannel,
        expectedSequenceNumber: Int,
        startTimeNanos: Long
    ): ServerResponse? {
        val buffer = ByteBuffer.allocate(1024).order(ByteOrder.BIG_ENDIAN)

        // Loop to ignore mismatched or malformed packets
        while (true) {
            buffer.clear()
            if (channel.read(buffer) > 0) {
                buffer.flip()

                // A valid response must have at least the protocol ID and sequence number
                if (buffer.remaining() < configuration.headerSizeBytes) {
                    continue // Malformed packet, wait for another one
                }

                val protocolIdBytes = ByteArray(4)
                buffer.get(protocolIdBytes)
                val responseProtocolId = String(protocolIdBytes, Charsets.US_ASCII)

                val responseSequenceNumber = buffer.int

                when (responseProtocolId) {
                    configuration.successResponseHeader -> {
                        // If it's a success packet, it MUST match the expected sequence number
                        if (responseSequenceNumber == expectedSequenceNumber) {
                            val rttNanos = System.nanoTime() - startTimeNanos
                            val rttMillis = TimeUnit.NANOSECONDS.toMillis(rttNanos)
                            println("Ping success!  $responseSequenceNumber RTT: $rttMillis ms")
                            return ServerResponse.Success(responseSequenceNumber, rttMillis)
                        }
                    }
                    configuration.errorResponseHeader -> {
                        // An error packet is relevant if it matches our sequence number
                        if (responseSequenceNumber == expectedSequenceNumber || responseSequenceNumber == 0) {
                            println("Ping error!    $responseSequenceNumber")
                            return ServerResponse.Error(responseSequenceNumber)
                        }
                    }
                }
                // If the packet was for a different sequence number or had an unknown
                // protocol ID, ignore it and continue the loop to wait for the correct one.
            }
            delay(1) // Small delay to prevent a tight busy-wait loop
        }
    }
}