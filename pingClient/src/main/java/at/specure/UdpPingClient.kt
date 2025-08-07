package at.specure

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
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

    private val encoding = Charsets.US_ASCII
    private val sequenceNumber = AtomicInteger(Random.nextInt())
    private var pingJob: Job? = null

    /**
     * Starts the ping process.
     * @return A Flow that emits a [PingResult] for each attempt.
     *         The flow runs indefinitely until the scope is cancelled or [@see stop] is called.
     */
    fun start(): Flow<PingResult> = channelFlow {
        pingJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Using try-with-resources to ensure the channel is closed
                DatagramChannel.open().use { channel ->
                    channel.configureBlocking(false)
                    channel.connect(InetSocketAddress(configuration.host, configuration.port))

                    val ticker = flow {
                        while (currentCoroutineContext().isActive) { // Loop only while active
                            emit(Unit)
                            delay(configuration.pingIntervalMillis)
                        }
                    }

                    ticker.collect {
                        val currentSequenceNumber = sequenceNumber.getAndIncrement()

                        try {
                            val requestPacket = createPingPacket(currentSequenceNumber)
                            val startTimeNanos = System.nanoTime()
                            // This is non-blocking because of configureBlocking(false)
                            channel.write(requestPacket)

                        val response = withTimeoutOrNull(configuration.pingTimeoutMillis) {
                            receiveResponse(channel, currentSequenceNumber, startTimeNanos)
                        }

                            val result = when (response) {
                                is ServerResponse.Success -> PingResult.Success(response.sequenceNumber, response.rttMillis)
                                is ServerResponse.Error -> PingResult.ServerError(response.sequenceNumber)
                                else -> PingResult.Lost(currentSequenceNumber)
                            }
                            // FIX: Check if channel is still open before sending
                            if (isActive) {
                                send(result)
                            }
                        } catch (e: IOException) {
                            // Catch specific IO exceptions
                            if (isActive) {
                                send(PingResult.ClientError(currentSequenceNumber, e))
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                // This is expected on stop(), re-throw it to ensure proper cancellation
                throw e
            } catch (e: Exception) {
                // Log any other unexpected errors
                println("An unexpected error occurred in the ping client: ${e.message}")
            }
        }
    }.cancellable() // Make the flow itself cancellable

    /**
     * Stops the ping process gracefully.
     */
    fun stop() {
        pingJob?.cancel()
    }

    private fun createPingPacket(sequenceNumber: Int): ByteBuffer {
        val protocolIdBytes = configuration.protocolId.toByteArray(encoding)
        val tokenBytes = configuration.token.toByteArray(encoding)
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
        println("ping prepared buffer")
        while (currentCoroutineContext().isActive) {
            buffer.clear()
            println("ping buffer cleared")
            if (channel.read(buffer) > 0) {
                buffer.flip()
                println("ping buffer read")

                // A valid response must have at least the protocol ID and sequence number
                if (buffer.remaining() < configuration.protocolId.toByteArray(encoding).size) {
                    println("ping malformed packet")
                    continue // Malformed packet, wait for another one
                }

                val protocolIdBytes = ByteArray(4)
                println("ping reading bytes")
                buffer.get(protocolIdBytes)
                val responseProtocolId = String(protocolIdBytes, encoding)

                val responseSequenceNumber = buffer.int
                println("ping received: $responseProtocolId $responseSequenceNumber")
                when (responseProtocolId) {
                    configuration.successResponseHeader -> {
                        // If it's a success packet, it MUST match the expected sequence number
                        if (responseSequenceNumber == expectedSequenceNumber) {
                            val rttNanos = System.nanoTime() - startTimeNanos
                            val rttMillis = TimeUnit.NANOSECONDS.toMillis(rttNanos)
                            return ServerResponse.Success(responseSequenceNumber, rttMillis)
                        }
                    }
                    configuration.errorResponseHeader -> {
                        // An error packet is relevant if it matches our sequence number
                        if (responseSequenceNumber == expectedSequenceNumber || responseSequenceNumber == 0) {
                            return ServerResponse.Error(responseSequenceNumber)
                        }
                    }
                }
            }
            delay(1) // Small delay to prevent a tight busy-wait loop
        }
        return null // Will be returned if the coroutine is cancelled
    }
}