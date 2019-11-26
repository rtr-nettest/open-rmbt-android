/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.specure.info.strength

import at.specure.info.TransportType

/**
 * Class that contains data about signal strength
 */
open class SignalStrengthInfo(

    /**
     * Transport type of network
     */
    val transport: TransportType,

    /**
     * Signal strength in dBm
     */
    val value: Int?,

    /**
     * RSRQ in db
     */
    val rsrq: Int?,

    /**
     * Signal level in range 0..4
     */
    val signalLevel: Int,

    /**
     * Minimum signal value for current network type in dBm
     */
    val min: Int,

    /**
     * Maximum signal value for current network type in dBm
     */
    val max: Int,

    /**
     * Timestamp in nanoseconds when data was received
     */
    val timestampNanos: Long
)