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
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.atomic.AtomicBoolean

class UdpStreamReceiver(
    private val settings: UdpStreamReceiverSettings,
    private val callback: UdpStreamCallback?
) {

    /**
     * @author lb
     */
    class UdpStreamReceiverSettings(
        var socket: DatagramSocket,
        var packets: Int,
        var sendResponse: Boolean
    )

    private val isRunning = AtomicBoolean(false)

    fun stop() {
        isRunning.set(false)
    }

    @Throws(InterruptedException::class, IOException::class)
    fun receive() {
        val byteOut = ByteArrayOutputStream()
        val dataOut = DataOutputStream(byteOut)

        isRunning.set(true)
        var packetsReceived = 0

        while (isRunning.get()) {
            if (Thread.interrupted()) {
                settings.socket.close()
                isRunning.set(false)
                throw InterruptedException()
            }

            val data = ByteArray(1024)
            val packet = DatagramPacket(data, data.size)

            settings.socket.receive(packet)
            packetsReceived++

            callback?.onReceive(packet)

            if (packetsReceived >= settings.packets) {
                isRunning.set(false)
            }

            if (settings.sendResponse) {
                byteOut.reset()

                if (callback != null && callback.onSend(dataOut, packetsReceived)) {
                    val dataToSend = byteOut.toByteArray()
                    val dp = DatagramPacket(dataToSend, dataToSend.size, packet.address, packet.port)
                    settings.socket.send(dp)
                }
            }
        }
    }
}
