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

import java.net.InetAddress

abstract class TestResult {
    @JvmField
    var ip_local: InetAddress? = null

    @JvmField
    var ip_server: InetAddress? = null

    @JvmField
    var port_remote = 0

    @JvmField
    var num_threads = 0

    @JvmField
    var encryption: String = "NONE"

    @JvmField
    var ping_shortest: Long = 0

    @JvmField
    var ping_median: Long = 0

    @JvmField
    var client_version: String? = null

    @JvmField
    val pings: MutableList<Ping> = ArrayList()

    @JvmField
    val speedItems: MutableList<SpeedItem> = ArrayList()

    @JvmField
    var voipTestResult: VoipTestResult? = null

    @JvmField
    var jitterMeanNanos: Long = 0

    @JvmField
    var packetLossPercent = 0f

    companion object {
        @JvmStatic
        fun getSpeedBitPerSec(bytes: Long, nsec: Long): Long {
            return Math.round(bytes.toDouble() / nsec.toDouble() * 1e9 * 8.0)
        }
    }
}
