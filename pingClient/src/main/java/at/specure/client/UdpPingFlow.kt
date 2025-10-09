package at.specure.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.random.Random


class UdpPingFlow(
    val configuration: PingClientConfiguration
) {
    private var sequenceNumber = Random.nextInt()

    fun pingFlow(): Flow<PingResult> = flow {
        val intervalMs = configuration.pingIntervalMillis
        val timeoutMs = configuration.pingTimeoutMillis
        val socket = DatagramSocket()
        val address = InetAddress.getByName(configuration.host)

        while (currentCoroutineContext().isActive) {
            val currentSeq = sequenceNumber
            sequenceNumber = (sequenceNumber + 1).and(0xFFFFFFFF.toInt()) // overflow-safe

            val encoding = Charsets.US_ASCII
            val standardEncoding = StandardCharsets.US_ASCII
            val buffer = ByteBuffer.allocate(configuration.protocolId.toByteArray(encoding).size + Int.SIZE_BYTES + (configuration.token?.toByteArray(encoding)?.size ?: 0))
            buffer.order(ByteOrder.BIG_ENDIAN)
            println("Ping flow buffer size: ${buffer.capacity()}")
            buffer.put(configuration.protocolId.toByteArray(encoding))
            buffer.putInt(currentSeq)
            buffer.put(configuration.token?.toByteArray(encoding))
            val data = buffer.array()
            println("Sending Ping packet:")
            println(data)
            val packet = DatagramPacket(data, data.size, address, configuration.port)
            println("Ping sending: ${String(packet.data, encoding)}")
            try {
                val startTime = System.currentTimeMillis()
                socket.send(packet)
                val responseBuffer = ByteArray(8000)
                val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)

               withTimeout(timeoutMs) {
                   socket.soTimeout = timeoutMs.toInt()

                   socket.receive(responsePacket)
                   val receivedData = responsePacket.data.copyOf(packet.length)

                   val byteBuffer = ByteBuffer.wrap(receivedData)

                   // Read header (first 4 bytes as string)
                   val headerBytes = ByteArray(4)
                   byteBuffer.get(headerBytes)
                   val header = String(headerBytes, StandardCharsets.US_ASCII)

                   // Read next 4 bytes as Int (big-endian)
                   val sequenceNumber = byteBuffer.int
                    println("Ping response received for sequence number: $sequenceNumber...")
                   if (header == configuration.successResponseHeader) {
                       val rtt = System.currentTimeMillis() - startTime
                       emit(PingResult.Success(currentSeq, rtt.toDouble()))
                   } else {
                       emit(PingResult.ServerError(currentSeq))
                   }
                }
            } catch (e: SocketTimeoutException) {
                emit(PingResult.Lost(currentSeq))
            } catch (e: TimeoutCancellationException) {
                emit(PingResult.Lost(currentSeq))
            } catch (e: Exception) {
                emit(PingResult.ClientError(currentSeq, e))
            }

            delay(intervalMs)
        }

        println("Ping flow ended")

        socket.close()
    }.flowOn(Dispatchers.IO)
}
