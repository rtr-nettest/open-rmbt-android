/*******************************************************************************
 * Copyright 2013-2015 alladin-IT GmbH
 * Copyright 2013-2015 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
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

import at.rtr.rmbt.client.helper.Config

// immutable! (accessed by multiple threads!)
open class RMBTTestParameter(
    val host: String?,
    val port: Int,
    val isEncryption: Boolean,
    val token: String?,
    val duration: Int,
    val numThreads: Int,
    val numPings: Int,
    val startTime: Long,
    val serverType: String?
) {

    val pretestDuration = 2
    val doPingIntervalMilliseconds = 1000 // TODO: Configure with server

    // QoS
    constructor(host: String?, port: Int, encryption: Boolean, duration: Int, numThreads: Int, numPings: Int) :
        this(host, port, encryption, null, duration, numThreads, numPings, 0, Config.SERVER_TYPE_QOS)

    val uuid: String?
        @JvmName("getUUID")
        get() {
            val t = token ?: return null
            val parts = t.split("_")
            if (parts.isEmpty()) {
                return null
            }
            return parts[0]
        }

    @Throws(IllegalArgumentException::class)
    fun check() {
        if (host == null || host.isEmpty()) {
            throw IllegalArgumentException("no host")
        }
        if (port <= 0) {
            throw IllegalArgumentException("no port")
        }
        if (uuid == null) {
            throw IllegalArgumentException("no uuid")
        }
        if (numThreads <= 0) {
            throw IllegalArgumentException("num threads <= 0")
        }
    }
}
