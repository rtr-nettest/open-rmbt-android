/*******************************************************************************
 * Copyright 2013-2014 alladin-IT GmbH
 * Copyright 2013-2014 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
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
package at.rtr.rmbt.client

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

class InputStreamCounter(`in`: InputStream) : FilterInputStream(`in`) {
    var count: Long = 0
        private set

    @Throws(IOException::class)
    override fun read(): Int {
        val read = `in`.read()
        if (read != -1) count++
        return read
    }

    @Throws(IOException::class)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val read = `in`.read(buffer, offset, length)
        if (read != -1) count += read.toLong()
        return read
    }

    @Throws(IOException::class)
    override fun skip(byteCount: Long): Long {
        val skip = `in`.skip(byteCount)
        count += skip
        return skip
    }

    override fun markSupported(): Boolean = false

    @Synchronized
    override fun mark(readlimit: Int) {
        throw UnsupportedOperationException()
    }

    @Synchronized
    @Throws(IOException::class)
    override fun reset() {
        throw UnsupportedOperationException()
    }
}
