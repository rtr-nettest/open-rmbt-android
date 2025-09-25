package at.specure.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
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
    private val configuration: PingClientConfiguration,
) {
    private val sentTimes = ConcurrentHashMap<Int, Triple<Long, Int, Long>>()
    private var sequenceNumber = Random.nextInt()
    private val logsEnabled = false

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

        coroutineScope {
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
                        val sendInfo = sentTimes.remove(seq)
                        if (sendInfo != null) {
                            val rttMs = (now - sendInfo.first) / 1_000_000.0
                            if (logsEnabled) {
                                print("Ping arrived: $seq $rttMs")
                            }
                            if (rttMs <= configuration.pingTimeoutMillis) {
                                if (logsEnabled) {
                                    println("       Success")
                                }
                                send(PingResult.Success(seq, rttMs))
                            } else {
                                if (logsEnabled) {
                                    println("       Lost")
                                }
                                send(PingResult.Lost(seq))
                            }
                        }
                    } catch (_: SocketTimeoutException) {
                        // ignored
                    } catch (e: Exception) {
                        // Ignore packet errors
                    }
                }
            }

            // Sender loop
            launch(Dispatchers.IO + SupervisorJob()) {
                while (isActive) {
                    val seq = sequenceNumber
                    sequenceNumber = (sequenceNumber + 1) and 0xFFFFFFFF.toInt()
                    val displayedSeq = sentTimes.size + 1
                    sentTimes[seq] = Triple(System.nanoTime(), displayedSeq, System.nanoTime())
                    val header = configuration.protocolId.toByteArray(charset)

                    val packetData = if (configuration.token != null) {
                        // struct.pack('!4sI16s')
                        val tokenBytes = Base64.getDecoder().decode(configuration.token)
                        ByteBuffer.allocate(4 + 4 + tokenBytes.size).order(byteOrder).apply {
                            put(header)
                            putInt(seq)
                            put(tokenBytes)
                        }.array()
                    } else {
                        // struct.pack('!4sI4s8s4s')
                        configuration.seed ?: throw Exception("Seed is missing")
                        configuration.clientIpPublicAddress ?: throw Exception("IP is missing")

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

                    try {
                        val packet = DatagramPacket(packetData, packetData.size, address, configuration.port)
                        socket.send(packet)
                    } catch (e: Exception) {
                        if (logsEnabled) {
                            println("Ping send error: $seq $e")
                        }
                        send(PingResult.ClientError(seq, e))
                    }

                    // Cleanup old pings
                    val nowMs = System.nanoTime()
                    val timedOut = sentTimes.filterValues { (sendTime, _, _) ->
                        (nowMs - sendTime) / 1_000_000 > configuration.pingTimeoutMillis
                    }
                    for ((seqTimeout, triple) in timedOut) {
                        sentTimes.remove(seqTimeout)
                        if (logsEnabled) {
                            println("Ping lost: $seq $timedOut")
                        }
                        send(PingResult.Lost(seq))
                    }

                    delay(configuration.pingIntervalMillis)
                }
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
