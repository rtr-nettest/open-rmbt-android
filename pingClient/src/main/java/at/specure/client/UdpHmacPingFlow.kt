package at.specure.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import java.time.Instant
import java.util.Base64

class UdpHmacPingFlow(
    val configuration: PingClientConfiguration,
) {
    private val sentTimes = ConcurrentHashMap<Int, Triple<Long, Int, Long>>()
    private var sequenceNumber = Random.nextInt()
    private val logsEnabled = true

    fun pingFlow(): Flow<PingResult> = channelFlow {
        val address = try {
            InetAddress.getByName(configuration.host)
        } catch (e: java.net.UnknownHostException) {
            println("Unknown host: ${configuration.host} -> ${e.message}")
            e.printStackTrace()
            // Send a PingResult.ClientError for sequence 0, or just exit flow
            send(PingResult.ClientError(0, e))
            close(e) // stop the flow
            return@channelFlow
        }

        val socket = DatagramSocket().apply { soTimeout = configuration.pingTimeoutMillis.toInt() }
        val charset = StandardCharsets.US_ASCII
        val byteOrder = ByteOrder.BIG_ENDIAN

        // Receiver coroutine
        launch(Dispatchers.IO + SupervisorJob()) {
            val buffer = ByteArray(1024)
            while (isActive) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val bb = ByteBuffer.wrap(packet.data, 0, packet.length).order(byteOrder)
                    val idBytes = ByteArray(4).apply { bb.get(this) }
                    val seq = bb.int

                    val id = String(idBytes, charset)
                    val now = System.nanoTime()

                    // remove ensures we only emit once
                    val sendInfo = sentTimes.remove(seq)
                    if (sendInfo != null) {
                        val rttMs = (now - sendInfo.first) / 1_000_000.0
                        if (rttMs <= configuration.pingTimeoutMillis) {
                            send(PingResult.Success(seq, rttMs))
                        } else {
                            send(PingResult.Lost(seq))
                        }
                    }
                } catch (_: SocketTimeoutException) {
                    // ignore
                } catch (_: Exception) {
                    // ignore
                }
            }
        }

        // Sender loop
        launch(Dispatchers.IO + SupervisorJob()) {
            while (isActive) {
                if (sequenceNumber == Int.MAX_VALUE) {
                    sequenceNumber = Int.MIN_VALUE
                    sentTimes.clear() // clear to avoid collisions
                }
                val seq = sequenceNumber
                sequenceNumber += 1
                val sendTime = System.nanoTime()
                sentTimes[seq] = Triple(sendTime, sentTimes.size + 1, sendTime)

                val packetData = try {
                    val header = configuration.protocolId.toByteArray(charset)
                    if (configuration.token != null) {
                        val tokenBytes = Base64.getDecoder().decode(configuration.token)
                        ByteBuffer.allocate(4 + 4 + tokenBytes.size).order(byteOrder).apply {
                            put(header)
                            putInt(seq)
                            put(tokenBytes)
                        }.array()
                    } else {
                        configuration.seed ?: throw Exception("Seed missing")
                        configuration.clientIpPublicAddress ?: throw Exception("IP missing")

                        val currentTime = (Instant.now().epochSecond and 0xFFFFFFFF).toInt()
                        val timeBytes = ByteBuffer.allocate(4).order(byteOrder).putInt(currentTime).array()
                        val packetHash = hmacSha256(configuration.seed, timeBytes).copyOfRange(0, 8)
                        val ipBytes = ipTo16Bytes(configuration.clientIpPublicAddress)
                        val packetIpHash = hmacSha256(configuration.seed, timeBytes + ipBytes).copyOfRange(0, 4)

                        ByteBuffer.allocate(4 + 4 + 4 + 8 + 4).order(byteOrder).apply {
                            put(header)
                            putInt(seq)
                            put(timeBytes)
                            put(packetHash)
                            put(packetIpHash)
                        }.array()
                    }
                } catch (e: Exception) {
                    // If we cannot build the packet, mark as failed immediately
                    sentTimes.remove(seq)
                    send(PingResult.ClientError(seq, e))
                    delay(configuration.pingIntervalMillis)
                    continue
                }

                try {
                    val packet = DatagramPacket(packetData, packetData.size, address, configuration.port)
                    socket.send(packet)
                } catch (e: Exception) {
                    // remove from sentTimes here to prevent a later timeout duplicate
                    sentTimes.remove(seq)
                    send(PingResult.ClientError(seq, e))
                }

                // Cleanup old pings
                val nowNs = System.nanoTime()
                val timedOut = sentTimes.filterValues { (sendTime, _, _) ->
                    (nowNs - sendTime) / 1_000_000 > configuration.pingTimeoutMillis
                }
                for ((seqTimeout, _) in timedOut) {
                    // remove before sending Lost to ensure we don't double emit
                    if (sentTimes.remove(seqTimeout) != null) {
                        send(PingResult.Lost(seqTimeout))
                    }
                }

                delay(configuration.pingIntervalMillis)
            }
        }
        awaitClose { socket.close() }
    }.flowOn(Dispatchers.IO)

    private fun hmacSha256(key: String, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(data)
    }

    private fun ipTo16Bytes(ip: String): ByteArray {
        val addr = InetAddress.getByName(ip)
        return when (addr) {
            is Inet6Address -> addr.address
            is Inet4Address -> {
                val v4 = addr.address
                ByteBuffer.allocate(16).apply {
                    putShort(0xFFFF.toShort())
                    put(ByteArray(10)) // fill with zeros
                    put(v4)
                }.array()
            }
            else -> ByteArray(16)
        }
    }
}
