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
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * udp stream sender used by the udp and voip qos test
 * @author lb
 */
class NioUdpStreamSender(
    private val settings: UdpStreamSenderSettings<DatagramChannel>,
    private val callback: UdpStreamCallback?
) : StreamSender<DatagramChannel> {

    private val isRunning = AtomicBoolean(false)

    fun stop() {
        isRunning.set(false)
    }

    /**
     * send a stream of udp packets
     * @return the [DatagramChannel] used for this stream or null if an exception occurred
     */
    override fun send(): DatagramChannel? {
        isRunning.set(true)

        var packetsSent = 0
        var packetsRcv = 0
        val byteOut = ByteArrayOutputStream()
        val dataOut = DataOutputStream(byteOut)
        val buffer = ByteBuffer.allocate(1024)
        val targetAddress: SocketAddress = InetSocketAddress(settings.targetHost, settings.targetPort)

        val delayMs = TimeUnit.MILLISECONDS.convert(settings.delay, settings.timeUnit)
        var lastSendTimestamp: Long = 0

        val startTimeMs = System.currentTimeMillis()
        val timeoutMs = TimeUnit.MILLISECONDS.convert(settings.timeout, settings.timeUnit)
        val stopTimeMs = if (timeoutMs > 0) timeoutMs + startTimeMs else 0

        var channel: DatagramChannel? = null
        var selector: Selector? = null

        try {
            if (settings.socket == null) {
                channel = DatagramChannel.open()
                channel.configureBlocking(false)
                if (settings.incomingPort != null) {
                    channel.socket().bind(InetSocketAddress(settings.incomingPort!!))
                    callback?.onBind(channel.socket().localPort)
                } else {
                    channel.socket().bind(null)
                }

                callback?.onBind(channel.socket().localPort)
            } else {
                channel = settings.socket
            }

            val ch = channel!!

            selector = Selector.open()

            if (settings.writeOnly) {
                ch.register(selector, SelectionKey.OP_WRITE)
            } else {
                ch.register(selector, SelectionKey.OP_READ or SelectionKey.OP_WRITE)
            }

            while (isRunning.get()) {
                if (Thread.interrupted()) {
                    isRunning.set(false)
                    throw InterruptedException()
                }

                if (stopTimeMs > 0 && stopTimeMs < System.currentTimeMillis()) {
                    isRunning.set(false)
                    throw TimeoutException("Exceeded timeout of " + timeoutMs + "ms")
                }

                // calculate correct packet delay
                var currentDelay = System.currentTimeMillis() - lastSendTimestamp
                currentDelay = if (currentDelay > delayMs) 0 else delayMs - currentDelay
                if (currentDelay > 0) {
                    Thread.sleep(currentDelay)
                }

                selector.select(1000)
                val readyKeys = selector.selectedKeys()
                if (!readyKeys.isEmpty()) {
                    val iterator = readyKeys.iterator()
                    while (iterator.hasNext()) {
                        val key = iterator.next()
                        iterator.remove()
                        if (key.isReadable && packetsRcv < settings.packets && key.isValid) {
                            buffer.clear()
                            ch.receive(buffer)
                            buffer.flip()
                            val dp = DatagramPacket(buffer.array(), buffer.array().size)
                            callback?.onReceive(dp)
                            packetsRcv++
                        }
                        if (key.isWritable && packetsSent < settings.packets && key.isValid) {
                            byteOut.reset()
                            buffer.clear()
                            try {
                                if (callback != null) {
                                    if (callback.onSend(dataOut, packetsSent)) {
                                        val data = byteOut.toByteArray()
                                        buffer.put(data)
                                        buffer.flip()
                                        ch.send(buffer, targetAddress)
                                        packetsSent++
                                        lastSendTimestamp = System.currentTimeMillis()
                                    }
                                }
                            } catch (e: IOException) {
                                e.printStackTrace()
                                return null
                            }
                        }
                    }
                }

                if (!settings.writeOnly) {
                    if (packetsSent >= settings.packets && packetsRcv >= settings.packets) {
                        isRunning.set(false)
                    }
                } else {
                    if (packetsSent >= settings.packets) {
                        isRunning.set(false)
                    }
                }
            }

            return ch
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (selector != null && selector.isOpen) {
                try {
                    selector.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (channel != null && channel.socket() != null && !channel.socket().isClosed && settings.closeOnFinish) {
                try {
                    channel.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return null
    }
}
