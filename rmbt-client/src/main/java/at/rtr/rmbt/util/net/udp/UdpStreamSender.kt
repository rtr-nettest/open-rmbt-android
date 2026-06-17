/*******************************************************************************
 * Copyright 2015 alladin-IT GmbH
 * Copyright 2015 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.rtr.rmbt.util.net.udp

import at.rtr.rmbt.util.net.udp.StreamSender.UdpStreamCallback
import at.rtr.rmbt.util.net.udp.StreamSender.UdpStreamSenderSettings
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * udp stream sender used by the udp and voip qos test
 * @author lb
 */
class UdpStreamSender(
    private val settings: UdpStreamSenderSettings<DatagramSocket>,
    private val callback: UdpStreamCallback?
) : StreamSender<DatagramSocket> {

    private val isRunning = AtomicBoolean(false)

    fun stop() {
        isRunning.set(false)
    }

    /**
     * send a stream of udp packets
     * @return the [DatagramSocket] used for this stream or null if an exception occurred
     */
    override fun send(): DatagramSocket? {
        println("UDP Stream: $settings")

        isRunning.set(true)

        var packetsSent = 0
        val byteOut = ByteArrayOutputStream()
        val dataOut = DataOutputStream(byteOut)

        val delayMs = TimeUnit.MILLISECONDS.convert(settings.delay, settings.timeUnit)
        var lastSendTimestamp: Long = 0

        val startTimeMs = System.currentTimeMillis()
        val timeoutMs = TimeUnit.MILLISECONDS.convert(settings.timeout, settings.timeUnit)
        val stopTimeMs = if (timeoutMs > 0) timeoutMs + startTimeMs else 0

        val socket = settings.socket!!

        while (isRunning.get()) {
            if (Thread.interrupted()) {
                isRunning.set(false)
                throw InterruptedException()
            }

            if (stopTimeMs > 0 && stopTimeMs < System.currentTimeMillis()) {
                isRunning.set(false)
                throw TimeoutException()
            }

            // calculate correct packet delay
            var currentDelay = System.currentTimeMillis() - lastSendTimestamp
            currentDelay = if (currentDelay > delayMs) 0 else delayMs - currentDelay
            if (currentDelay > 0) {
                Thread.sleep(currentDelay)
            }

            byteOut.reset()

            try {
                if (callback != null && callback.onSend(dataOut, packetsSent)) {
                    val data = byteOut.toByteArray()

                    val packet = if (!socket.isConnected) {
                        DatagramPacket(data, data.size, settings.targetHost, settings.targetPort)
                    } else {
                        DatagramPacket(data, data.size)
                    }

                    socket.send(packet)
                    packetsSent++
                    lastSendTimestamp = System.currentTimeMillis()
                }

                if (!settings.writeOnly) {
                    try {
                        val buffer = ByteArray(1024)

                        val dp = DatagramPacket(buffer, buffer.size)
                        socket.soTimeout = TimeUnit.MILLISECONDS.convert(settings.responseSoTimeout, settings.timeUnit).toInt()
                        socket.receive(dp)
                        callback?.onReceive(dp)
                    } catch (e: SocketTimeoutException) {
                        e.printStackTrace()
                    }
                }

                if (packetsSent >= settings.packets) {
                    isRunning.set(false)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                if (settings.closeOnFinish) {
                    socket.close()
                }
                return null
            }
        }

        return socket
    }
}
