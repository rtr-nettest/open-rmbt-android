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
import java.nio.ByteOrder

/**
 * contains basic RTP definitions and operations (payload types, versions, codecs, exceptions, header generation)
 * @author lb
 */
class RealtimeTransportProtocol {

    class RtpException(var rtpErrorType: RtpErrorType) : Exception() {

        enum class RtpErrorType {
            PACKET_SIZE_TOO_SMALL,
            INVALID_HEADER
        }

        companion object {
            private const val serialVersionUID = 1L
        }
    }

    enum class CodecType {
        AUDIO,
        VIDEO,
        BOTH,
        UNKNOWN
    }

    /**
     * RTP payload types as defined in RFC 3551
     * @author lb
     */
    enum class PayloadType(
        val value: Int,
        val sampleRate: Int,
        val channels: Int,
        val codecType: CodecType
    ) {
        UNKNOWN(-1, -1, -1, CodecType.UNKNOWN),
        PCMU(0, 8000, 1, CodecType.AUDIO),
        GSM(3, 8000, 1, CodecType.AUDIO),
        G723(4, 8000, 1, CodecType.AUDIO),
        DVI4_8(5, 8000, 1, CodecType.AUDIO),
        DVI4_16(6, 16000, 1, CodecType.AUDIO),
        LPC(7, 8000, 1, CodecType.AUDIO),
        PCMA(8, 8000, 1, CodecType.AUDIO),
        G722(9, 8000, 1, CodecType.AUDIO),
        L16_1(10, 44100, 2, CodecType.AUDIO),
        L16_2(11, 44100, 1, CodecType.AUDIO),
        QCELP(12, 8000, 1, CodecType.AUDIO),
        CN(13, 8000, 1, CodecType.AUDIO),
        MPA(14, 90000, 1, CodecType.AUDIO),
        G728(15, 8000, 1, CodecType.AUDIO),
        DVI4_11(16, 11025, 1, CodecType.AUDIO),
        DVI4_22(17, 22050, 1, CodecType.AUDIO),
        G729(18, 8000, 1, CodecType.AUDIO),
        G726_40(-1, 8000, 1, CodecType.AUDIO),
        G726_32(-1, 8000, 1, CodecType.AUDIO),
        G726_24(-1, 8000, 1, CodecType.AUDIO),
        G726_16(-1, 8000, 1, CodecType.AUDIO),
        G729D(-1, 8000, 1, CodecType.AUDIO),
        G729E(-1, 8000, 1, CodecType.AUDIO),
        GSM_EFR(-1, 8000, 1, CodecType.AUDIO),
        L8(-1, -1, -1, CodecType.AUDIO),
        RED(-1, -1, -1, CodecType.AUDIO),
        VDVI(-1, -1, 1, CodecType.AUDIO),
        CELB(25, 90000, -1, CodecType.VIDEO),
        JPEG(26, 90000, -1, CodecType.VIDEO),
        NV(28, 90000, -1, CodecType.VIDEO),
        H261(31, 90000, -1, CodecType.VIDEO),
        MPV(32, 90000, -1, CodecType.VIDEO),
        MP2T(33, 90000, -1, CodecType.BOTH),
        H263(34, 90000, -1, CodecType.VIDEO),
        H263_1998(-1, 90000, -1, CodecType.VIDEO);

        companion object {
            fun getByCodecValue(value: Int): PayloadType {
                for (p in values()) {
                    if (p.value == value) {
                        return p
                    }
                }

                return UNKNOWN
            }

            fun getByCodecValue(value: Int, defaultType: PayloadType): PayloadType {
                val p = getByCodecValue(value)
                if (UNKNOWN == p) {
                    return defaultType
                }
                return p
            }
        }
    }

    enum class RtpVersion(val version: Int) {
        VER0(0),
        VER1(1),
        VER2(2),
        UNKNOWN(-1);

        companion object {
            fun getByVersion(version: Int): RtpVersion {
                for (v in values()) {
                    if (v.version == version) {
                        return v
                    }
                }

                return UNKNOWN
            }
        }
    }

    companion object {
        /**
         * creates the first 4 bytes of the RTP header
         */
        fun createHeaderBytes(
            version: RtpVersion,
            hasPadding: Boolean,
            hasExtension: Boolean,
            csrcCount: Int,
            setMarker: Boolean,
            payloadType: PayloadType,
            sequenceNumber: Int,
            timeStamp: Long,
            ssrc: Long
        ): ByteArray {
            val h = ByteArray(12)
            h[0] = ByteUtil.setLeftBitsValue(h[0], 2, version.version)
            h[0] = ByteUtil.setBit(h[0], 5, hasPadding)
            h[0] = ByteUtil.setBit(h[0], 4, hasExtension)
            h[0] = ByteUtil.setRightBitsValue(h[0], 4, csrcCount)
            h[1] = ByteUtil.setBit(h[1], 7, setMarker)
            h[1] = ByteUtil.setRightBitsValue(h[1], 7, payloadType.value)
            // network byte order = big endian
            ByteUtil.setInt(h, 2, 3, sequenceNumber, ByteOrder.BIG_ENDIAN)
            ByteUtil.setLong(h, 4, 7, timeStamp, ByteOrder.BIG_ENDIAN)
            ByteUtil.setLong(h, 8, 11, ssrc, ByteOrder.BIG_ENDIAN)
            return h
        }

        fun createCsrcIdentifierBytes(csrcIds: LongArray?): ByteArray? {
            if (csrcIds != null && csrcIds.isNotEmpty()) {
                val h = ByteArray(csrcIds.size * 4)
                for (i in csrcIds.indices) {
                    ByteUtil.setLong(h, i * 4, 3 + i * 4, csrcIds[i], ByteOrder.BIG_ENDIAN)
                }
                return h
            }
            return null
        }
    }
}
