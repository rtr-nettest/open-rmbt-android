package at.specure.server

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread
import kotlin.random.Random

fun main() {
    val port = 9999
    val buffer = ByteArray(1024)
    val encoding = StandardCharsets.UTF_8
    val packetLossProbability = 0.3   // 30% chance to drop a packet
    val minDelayMs = 100L
    val maxDelayMs = 1000L

    DatagramSocket(port).use { socket ->
        println("✅ UDP mock server is running on port $port")

        while (true) {
            val packet = DatagramPacket(buffer, buffer.size)
            socket.receive(packet)

            val receivedData = packet.data.copyOf(packet.length)

            val byteBuffer = ByteBuffer.wrap(receivedData)

            // Read header (first 4 bytes as string)
            val headerBytes = ByteArray(4)
            byteBuffer.get(headerBytes)
            val header = String(headerBytes, StandardCharsets.US_ASCII)

            // Read next 4 bytes as Int (big-endian)
            val sequenceNumber = byteBuffer.int

            val receivedText = String(receivedData, encoding)
            println("📩 Received: $sequenceNumber $receivedText from ${packet.address}:${packet.port}")

            // Simulate packet loss
            if (Random.nextDouble() < packetLossProbability) {
                println("❌ Simulating packet loss for: $receivedText")
                continue
            }

            // Simulate random delay before responding
            val delayMs = Random.nextLong(minDelayMs, maxDelayMs)

            thread {
                Thread.sleep(delayMs)
                val buffer = ByteBuffer.allocate(4 + Int.SIZE_BYTES)
                buffer.put("RR01".toByteArray(encoding))
                buffer.putInt(sequenceNumber)
                val responseData = buffer.array()
                val responseText = String(responseData, StandardCharsets.US_ASCII)
                val responsePacket = DatagramPacket(
                    responseData,
                    responseData.size,
                    packet.address,
                    packet.port
                )
                socket.send(responsePacket)
                println("📤 Sent response after ${delayMs}ms: $responseText\n")
            }
        }
    }
}
