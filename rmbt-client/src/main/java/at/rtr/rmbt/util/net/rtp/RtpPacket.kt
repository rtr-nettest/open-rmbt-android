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
package at.rtr.rmbt.util.net.rtp

import at.rtr.rmbt.util.ByteUtil
import at.rtr.rmbt.util.net.rtp.RealtimeTransportProtocol.PayloadType
import at.rtr.rmbt.util.net.rtp.RealtimeTransportProtocol.RtpException
import at.rtr.rmbt.util.net.rtp.RealtimeTransportProtocol.RtpException.RtpErrorType
import at.rtr.rmbt.util.net.rtp.RealtimeTransportProtocol.RtpVersion
import java.nio.ByteOrder

/**
 * rtp packet including header and payload
 * @author lb
 */
class RtpPacket {
    var header: ByteArray = ByteArray(0)
    var csrcIdentifier: ByteArray? = null
    var payload: ByteArray? = null

    constructor(payloadType: PayloadType, csrcCount: Int, csrc: LongArray?, seqNumber: Int, timeStamp: Long, ssrc: Long) :
        this(payloadType, csrcCount, csrc, seqNumber, timeStamp, ssrc, null)

    constructor(
        payloadType: PayloadType,
        csrcCount: Int,
        csrc: LongArray?,
        seqNumber: Int,
        timeStamp: Long,
        ssrc: Long,
        payload: ByteArray?
    ) {
        this.header = RealtimeTransportProtocol.createHeaderBytes(
            RtpVersion.VER2, false, false,
            csrcCount, false, payloadType, seqNumber, timeStamp, ssrc
        )

        this.csrcIdentifier = RealtimeTransportProtocol.createCsrcIdentifierBytes(csrc)
        this.payload = payload
    }

    constructor(packet: ByteArray?) {
        if (packet == null || packet.size < 12) {
            throw RtpException(RtpErrorType.PACKET_SIZE_TOO_SMALL)
        }

        try {
            header = ByteArray(12)
            System.arraycopy(packet, 0, header, 0, header.size)
            var curPos = header.size
            val csrsCount = getCsrcCount()
            if (csrsCount > 0) {
                val csrc = ByteArray(csrsCount * 4)
                System.arraycopy(packet, curPos, csrc, 0, csrc.size)
                curPos += csrc.size
                csrcIdentifier = csrc
            }
            if (packet.size > curPos) {
                val payloadSize = packet.size - curPos
                val p = ByteArray(payloadSize)
                System.arraycopy(packet, curPos, p, 0, p.size)
                payload = p
            }
        } catch (e: Exception) {
            throw RtpException(RtpErrorType.INVALID_HEADER)
        }
    }

    fun getPayloadType(): PayloadType {
        return PayloadType.getByCodecValue(header[1].toInt() and 0x7F)
    }

    fun setPayloadType(payloadType: PayloadType) {
        header[1] = ByteUtil.setRightBitsValue(header[1], 7, payloadType.value)
    }

    fun hasMarker(): Boolean {
        return ByteUtil.getBit(header[1], 7)
    }

    fun setHasMarker(hasMarker: Boolean) {
        header[1] = ByteUtil.setBit(header[1], 7, hasMarker)
    }

    fun getVersion(): RtpVersion {
        return RtpUtil.getVersion(header[0])
    }

    fun setVersion(version: RtpVersion) {
        header[0] = ByteUtil.setLeftBitsValue(header[0], 2, version.version)
    }

    fun getCsrcCount(): Int {
        return header[0].toInt() and 0x0F
    }

    fun setCsrcCount(csrcCount: Int) {
        header[0] = ByteUtil.setRightBitsValue(header[0], 4, csrcCount)
    }

    fun getCsrcIdentifiersAsLong(): LongArray {
        val csrc = csrcIdentifier
        if (csrc != null && csrc.isNotEmpty()) {
            val csrcIds = LongArray(csrc.size / 4)
            for (i in csrcIds.indices) {
                csrcIds[i] = ByteUtil.getLong(csrc, i * 4, 3 + i * 4, ByteOrder.BIG_ENDIAN)
            }

            return csrcIds
        }

        return LongArray(0)
    }

    fun hasPadding(): Boolean {
        return ByteUtil.getBit(header[0], 5)
    }

    fun setHasPadding(hasPadding: Boolean) {
        header[0] = ByteUtil.setBit(header[0], 5, hasPadding)
    }

    fun hasExtension(): Boolean {
        return ByteUtil.getBit(header[0], 4)
    }

    fun setHasExtension(hasExtension: Boolean) {
        header[0] = ByteUtil.setBit(header[0], 4, hasExtension)
    }

    fun getSequnceNumber(): Int {
        return ByteUtil.getInt(header, 2, 3, ByteOrder.BIG_ENDIAN)
    }

    fun setSequnceNumber(seqNumber: Int) {
        header = ByteUtil.setInt(header, 2, 3, seqNumber, ByteOrder.BIG_ENDIAN)
    }

    fun increaseSequenceNumber(delta: Int) {
        setSequnceNumber(getSequnceNumber() + delta)
    }

    fun getTimestamp(): Long {
        return ByteUtil.getLong(header, 4, 7, ByteOrder.BIG_ENDIAN)
    }

    fun setTimestamp(timestamp: Long) {
        header = ByteUtil.setLong(header, 4, 7, timestamp, ByteOrder.BIG_ENDIAN)
    }

    fun increaseTimestamp(delta: Long) {
        setTimestamp(getTimestamp() + delta)
    }

    fun getSsrc(): Long {
        return ByteUtil.getLong(header, 8, 11, ByteOrder.BIG_ENDIAN)
    }

    fun setSsrc(ssrc: Long) {
        header = ByteUtil.setLong(header, 8, 11, ssrc, ByteOrder.BIG_ENDIAN)
    }

    fun getBytes(): ByteArray {
        val d = ByteArray(
            header.size +
                (csrcIdentifier?.size ?: 0) +
                (payload?.size ?: 0)
        )

        var curPos = 0
        System.arraycopy(header, 0, d, 0, header.size)
        curPos += header.size
        csrcIdentifier?.let {
            System.arraycopy(it, 0, d, curPos, it.size)
            curPos += it.size
        }
        payload?.let {
            System.arraycopy(it, 0, d, curPos, it.size)
        }

        return d
    }

    override fun toString(): String {
        return "RtpPacket [payload=" + payload.contentToString() +
            ", getPayloadType()=" + getPayloadType() + ", hasMarker()=" +
            hasMarker() + ", getVersion()=" + getVersion() +
            ", getCsrcCount()=" + getCsrcCount() +
            ", getCsrcIdentifiersAsLong()=" +
            getCsrcIdentifiersAsLong().contentToString() +
            ", hasPadding()=" + hasPadding() + ", hasExtension()=" +
            hasExtension() + ", getSequnceNumber()=" + getSequnceNumber() +
            ", getTimestamp()=" + getTimestamp() + ", getSsrc()=" +
            getSsrc() + "]"
    }
}
