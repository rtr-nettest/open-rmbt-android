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

import java.io.DataOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

interface StreamSender<T> {

    fun send(): T?

    /**
     * @author lb
     */
    interface UdpStreamCallback {

        /**
         * is called before sending data
         * @param dataOut output stream for the packet's payload
         * @param packetNumber the current packet number
         */
        fun onSend(dataOut: DataOutputStream, packetNumber: Int): Boolean

        /**
         * is called after a datagram packet has been received
         * @param dp the received datagram packet
         */
        fun onReceive(dp: DatagramPacket)

        /**
         * is called when the socket/channel is bound to a specific port
         * @param port
         */
        fun onBind(port: Int?)
    }

    /**
     * @author lb
     */
    class UdpStreamSenderSettings<T> {
        var packets = 0
        var delay: Long = 0
        var timeUnit: TimeUnit? = null
        var targetHost: InetAddress? = null
        var targetPort = 0
        var incomingPort: Int? = null
        var responseSoTimeout: Long = 0
        var timeout: Long = 0
        var socket: T? = null
        var isNonblocking = false
        var writeOnly = false
        var closeOnFinish = false

        constructor(
            socket: T?,
            closeOnFinish: Boolean,
            targetHost: InetAddress?,
            targetPort: Int,
            packets: Int,
            delay: Long,
            timeout: Long,
            timeUnit: TimeUnit?,
            writeOnly: Boolean,
            responseSoTimeout: Int
        ) {
            this.socket = socket
            this.targetHost = targetHost
            this.targetPort = targetPort
            this.packets = packets
            this.delay = delay
            this.timeout = timeout
            this.timeUnit = timeUnit
            this.writeOnly = writeOnly
            this.responseSoTimeout = responseSoTimeout.toLong()
            this.closeOnFinish = closeOnFinish
        }

        constructor(
            socket: T?,
            closeOnFinish: Boolean,
            targetHost: InetAddress?,
            targetPort: Int,
            packets: Int,
            delay: Long,
            timeUnit: TimeUnit?
        ) : this(socket, closeOnFinish, targetHost, targetPort, packets, delay, 0, timeUnit, false, 0)

        override fun toString(): String =
            "UdpStreamSenderSettings [packets=$packets, delay=$delay, timeUnit=$timeUnit, targetHost=$targetHost, " +
                "targetPort=$targetPort, responseSoTimeout=$responseSoTimeout, timeout=$timeout, socket=$socket, " +
                "isNonblocking=$isNonblocking, writeOnly=$writeOnly]"
    }
}
