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
package at.rtr.rmbt.util

import java.nio.ByteOrder

/**
 * Utility class for bytes and bits
 * @author lb
 */
object ByteUtil {

    /**
     * get an int value from a byte array
     */
    fun getInt(b: ByteArray, start: Int, end: Int, byteOrder: ByteOrder): Int {
        return getLong(b, start, end, byteOrder).toInt()
    }

    fun getLong(b: ByteArray, start: Int, end: Int, byteOrder: ByteOrder): Long {
        var i: Long = 0
        for (n in 0..(end - start)) {
            i = i shl 8
            i += if (ByteOrder.BIG_ENDIAN == byteOrder) {
                (b[n + start].toInt() and 0xff).toLong()
            } else {
                (b[(end - start) - (n - start)].toInt() and 0xff).toLong()
            }
        }

        return i
    }

    /**
     * set an int value in a byte array
     */
    fun setInt(bytes: ByteArray, start: Int, end: Int, value: Int, byteOrder: ByteOrder): ByteArray {
        return setLong(bytes, start, end, value.toLong(), byteOrder)
    }

    /**
     * set a long value in a byte array
     */
    fun setLong(bytes: ByteArray, start: Int, end: Int, value: Long, byteOrder: ByteOrder): ByteArray {
        var value = value
        for (n in 0..(end - start)) {
            val b = (value % 256).toByte()
            bytes[if (ByteOrder.BIG_ENDIAN == byteOrder) (end - start) - (n - start) else n + start] = b
            value = value shr 8
        }

        return bytes
    }

    /**
     * get the value of a specific bit of a byte
     */
    fun getBit(b: Byte, bit: Int): Boolean {
        return (b.toInt() shr bit) == 1
    }

    /**
     * set the value of a specific bit of a byte
     */
    fun setBit(b: Byte, bit: Int, value: Boolean): Byte {
        return if (value) {
            (b.toInt() or (1 shl bit)).toByte()
        } else {
            ((b.toInt() or (1 shl bit)) xor (1 shl bit)).toByte()
        }
    }

    /**
     * set a specific amount of bits on the right side to a value
     */
    fun setRightBitsValue(b: Byte, bitlen: Int, value: Int): Byte {
        val bitmask = (0xff shr bitlen) shl bitlen
        return ((b.toInt() and bitmask) or (value and bitmask.inv())).toByte()
    }

    /**
     * set a specific amount of bits on the left side to a value
     */
    fun setLeftBitsValue(b: Byte, bitlen: Int, value: Int): Byte {
        val bitmask = ((0xff shr (8 - bitlen)) shl (8 - bitlen)).inv()
        val valueBitmask = ((0xff shr bitlen) shl bitlen).inv()
        return ((b.toInt() and bitmask) or ((value and valueBitmask).toByte().toInt() shl (8 - bitlen))).toByte()
    }

    /**
     * functionality same as [java.util.Arrays.toString] but all byte values are being treated as unsigned
     */
    fun toStringUnsigned(a: ByteArray?): String {
        if (a == null) {
            return "null"
        }
        val iMax = a.size - 1
        if (iMax == -1) {
            return "[]"
        }

        val b = StringBuilder()
        b.append('[')
        var i = 0
        while (true) {
            b.append(a[i].toInt() and 0xff)
            if (i == iMax) {
                return b.append(']').toString()
            }
            b.append(", ")
            i++
        }
    }

    /**
     * this method copies a byte array to an int array and treats the byte values as unsigned
     */
    fun toUnsignedInt(a: ByteArray): IntArray {
        val b = IntArray(a.size)
        for (i in a.indices) {
            b[i] = a[i].toInt() and 0xff
        }

        return b
    }
}
