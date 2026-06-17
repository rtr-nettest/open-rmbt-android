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
package at.rtr.rmbt.client.helper

class IntermediateResult {
    var pingNano: Long = 0

    var downBitPerSec: Long = 0

    var upBitPerSec: Long = 0

    var status: TestStatus? = null

    var progress = 0f

    var downBitPerSecLog = 0.0

    var upBitPerSecLog = 0.0

    var remainingWait: Long = 0

    var jitter: Long = 0

    var packetLossUp: Long = 0

    var packetLossDown: Long = 0

    fun setLogValues() {
        downBitPerSecLog = toLog(downBitPerSec)
        upBitPerSecLog = toLog(upBitPerSec)
    }

    companion object {
        fun toLog(value: Long): Double {
            if (value < 1e5) return 0.0
            return (2.0 + Math.log10(value / 1e7)) / 4.0
            // value in bps
            // < 0.1 -> 0
            // 0.1 Mbps -> 0
            // 1000 Mbps -> 1
            // > 1000 Mbps -> >1
        }
    }
}
