/*******************************************************************************
 * Copyright 2015, 2016 alladin-IT GmbH
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
package at.rtr.rmbt.util.net.rtp

import at.rtr.rmbt.util.ByteUtil
import at.rtr.rmbt.util.net.udp.NioUdpStreamSender
import at.rtr.rmbt.util.net.udp.StreamSender.UdpStreamCallback
import at.rtr.rmbt.util.net.udp.StreamSender.UdpStreamSenderSettings
import at.rtr.rmbt.util.net.udp.UdpStreamSender
import java.io.Closeable
import java.io.DataOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteOrder
import java.nio.channels.DatagramChannel
import java.util.Random
import java.util.TreeSet
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * @author lb
 */
object RtpUtil {

    fun <T : Closeable> runVoipStream(
        socket: T?,
        closeOnFinish: Boolean,
        targetHost: InetAddress,
        targetPort: Int,
        sampleRate: Int,
        bps: Int,
        payloadType: RealtimeTransportProtocol.PayloadType,
        sequenceNumber: Long,
        ssrc: Int,
        callDuration: Long,
        delay: Long,
        timeout: Long,
        useNio: Boolean,
        receiveCallback: UdpStreamCallback?
    ): T? {
        return runVoipStream(
            socket, closeOnFinish, targetHost, targetPort, null, sampleRate, bps, payloadType,
            sequenceNumber, ssrc, callDuration, delay, timeout, useNio, receiveCallback
        )
    }

    /**
     * runs an rtp/voip stream (incoming and outgoing)
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Closeable> runVoipStream(
        socket: T?,
        closeOnFinish: Boolean,
        targetHost: InetAddress,
        targetPort: Int,
        incomingPort: Int?,
        sampleRate: Int,
        bps: Int,
        payloadType: RealtimeTransportProtocol.PayloadType,
        sequenceNumber: Long,
        ssrc: Int,
        callDuration: Long,
        delay: Long,
        timeout: Long,
        useNio: Boolean,
        receiveCallback: UdpStreamCallback?
    ): T? {

        val payloadSize = (sampleRate / (1000 / delay) * (bps / 8)).toInt()
        val r = Random()
        val payloadTimestamp = (sampleRate / (1000 / delay)).toInt()
        val initialRtpPacket = RtpPacket(payloadType, 0, LongArray(0), sequenceNumber.toInt(), 0, ssrc.toLong())
        val numPackets = (callDuration / delay).toInt()
        val settings = UdpStreamSenderSettings<T>(
            socket, closeOnFinish, targetHost, targetPort, numPackets, delay, timeout, TimeUnit.MILLISECONDS, false, 0
        )
        settings.incomingPort = incomingPort

        if (receiveCallback == null) {
            settings.writeOnly = true
        }

        val callback: UdpStreamCallback = object : UdpStreamCallback {

            override fun onSend(dataOut: DataOutputStream, packetNumber: Int): Boolean {
                if (packetNumber > 0) {
                    initialRtpPacket.increaseSequenceNumber(1)
                    initialRtpPacket.increaseTimestamp(payloadTimestamp.toLong())
                    initialRtpPacket.setHasMarker(false)
                } else {
                    initialRtpPacket.setHasMarker(true)
                }

                val payload = ByteArray(payloadSize)
                r.nextBytes(payload)
                initialRtpPacket.payload = payload

                val data = initialRtpPacket.getBytes()
                dataOut.write(data)
                return true
            }

            override fun onReceive(dp: DatagramPacket) {
                receiveCallback?.onReceive(dp)
            }

            override fun onBind(port: Int?) {
                // report the actually bound local port (was: incomingPort, which is null when the
                // server provides no in_port objective → voip_objective_in_port serialized as null
                // and the server-side QoS evaluation fails all VoIP conditions)
                receiveCallback!!.onBind(if (incomingPort != null) incomingPort else port)
            }
        }

        return if (!useNio) {
            val udpStreamSender = UdpStreamSender(settings as UdpStreamSenderSettings<DatagramSocket>, callback)
            udpStreamSender.send() as T?
        } else {
            val udpStreamSender = NioUdpStreamSender(settings as UdpStreamSenderSettings<DatagramChannel>, callback)
            udpStreamSender.send() as T?
        }
    }

    /**
     * extract the rtp version from the first header byte
     */
    fun getVersion(firstHeaderByte: Byte): RealtimeTransportProtocol.RtpVersion {
        return RealtimeTransportProtocol.RtpVersion.getByVersion((firstHeaderByte.toInt() shr 6) and 0x03)
    }

    /**
     * get the synchronization source identifier
     * @return rtp packet ssrc or -1 if packet data is invalid
     */
    fun getSsrc(data: ByteArray?): Long {
        return if (data != null && data.size >= 11) {
            ByteUtil.getLong(data, 8, 11, ByteOrder.BIG_ENDIAN)
        } else {
            -1
        }
    }

    fun calculateQoS(rtpControlDataMap: Map<Int, RtpControlData>, initialSequenceNumber: Long, sampleRate: Int): RtpQoSResult {
        val sequenceNumberSet = TreeSet(rtpControlDataMap.keys)

        val jitterMap = HashMap<Int, Float>()
        val sequenceSet = TreeSet<RtpSequence>()

        var maxJitter: Long = 0
        var meanJitter: Long = 0
        var skew: Long = 0
        var maxDelta: Long = 0
        var tsDiff: Long = 0

        var prevSeqNr = -1
        for (x in sequenceNumberSet) {
            val i = rtpControlDataMap[prevSeqNr]
            val j = rtpControlDataMap[x]
            if (prevSeqNr >= 0) {
                tsDiff = j!!.receivedNs - i!!.receivedNs
                val prevJitter = jitterMap[prevSeqNr]!!
                val delta = Math.abs(calculateDelta(i, j, sampleRate))
                val jitter = prevJitter + (delta.toFloat() - prevJitter) / 16f
                jitterMap[x] = jitter
                maxDelta = Math.max(delta, maxDelta)
                skew += TimeUnit.NANOSECONDS.convert(
                    ((j.rtpPacket.getTimestamp() - i.rtpPacket.getTimestamp()).toFloat() / sampleRate.toFloat() * 1000f).toLong(),
                    TimeUnit.MILLISECONDS
                ) - tsDiff
                maxJitter = Math.max(jitter.toLong(), maxJitter)
                meanJitter = (meanJitter + jitter).toLong()
            } else {
                jitterMap[x] = 0f
            }
            prevSeqNr = x
            sequenceSet.add(RtpSequence(j!!.receivedNs, x))
        }

        var nextSeq = initialSequenceNumber
        var packetsOutOfOrder = 0
        var maxSequential = 0
        var minSequential = 0
        var curSequential = 0
        for (i in sequenceSet) {
            if (i.seq.toLong() != nextSeq) {
                packetsOutOfOrder++
                maxSequential = Math.max(curSequential, maxSequential)
                if (curSequential > 1) {
                    minSequential = if (curSequential < minSequential) curSequential else if (minSequential == 0) curSequential else minSequential
                }
                curSequential = 0
            } else {
                curSequential++
            }

            nextSeq++
        }

        maxSequential = Math.max(curSequential, maxSequential)
        if (curSequential > 1) {
            minSequential = if (curSequential < minSequential) curSequential else if (minSequential == 0) curSequential else minSequential
        }

        if (minSequential == 0 && maxSequential > 0) {
            minSequential = maxSequential
        }

        return RtpQoSResult(
            maxJitter,
            if (jitterMap.size > 0) meanJitter / jitterMap.size else 0L,
            skew,
            maxDelta,
            packetsOutOfOrder,
            minSequential,
            maxSequential,
            jitterMap
        )
    }

    private fun calculateDelta(i: RtpControlData, j: RtpControlData, sampleRate: Int): Long {
        val msDiff = j.receivedNs - i.receivedNs
        val tsDiff = TimeUnit.NANOSECONDS.convert(
            ((j.rtpPacket.getTimestamp() - i.rtpPacket.getTimestamp()).toFloat() / sampleRate.toFloat() * 1000f).toLong(),
            TimeUnit.MILLISECONDS
        )
        return msDiff - tsDiff
    }

    /**
     * @author lb
     */
    class RtpControlData(val rtpPacket: RtpPacket, val receivedNs: Long)

    private class RtpSequence(val timestampNs: Long, val seq: Int) : Comparable<RtpSequence> {
        override fun compareTo(o: RtpSequence): Int {
            return timestampNs.compareTo(o.timestampNs)
        }
    }

    class RtpQoSResult(
        val maxJitter: Long,
        val meanJitter: Long,
        val skew: Long,
        val maxDelta: Long,
        val outOfOrder: Int,
        minSequential: Int,
        maxSequential: Int,
        val jitterMap: Map<Int, Float>
    ) {
        val receivedPackets: Int = jitterMap.size
        val minSequential: Int = if (minSequential > receivedPackets) receivedPackets else minSequential
        val maxSequencial: Int = if (maxSequential > receivedPackets) receivedPackets else maxSequential

        override fun toString(): String {
            return "RtpQoSResult [jitterMap=" + jitterMap +
                ", receivedPackets=" + receivedPackets +
                ", outOfOrder=" + outOfOrder + ", minSequential=" + minSequential + ", maxSequencial=" + maxSequencial +
                ", maxJitter=" + (maxJitter.toFloat() / 1000000f) +
                ", meanJitter=" + (meanJitter.toFloat() / 1000000f) + ", skew=" +
                (skew.toFloat() / 1000000f) + ", maxDelta=" + (maxDelta.toFloat() / 1000000) + "]"
        }
    }
}
