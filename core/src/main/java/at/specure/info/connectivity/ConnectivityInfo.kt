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

package at.specure.info.connectivity

import android.net.NetworkCapabilities
import at.specure.info.NetworkCapability
import at.specure.info.TransportType

/**
 * Represents short info available for active network
 */
data class ConnectivityInfo(

    /**
     * Network Id netId from [android.net.NetworkCapabilities]
     */
    val netId: Int,

    /**
     * Defines transport type of network
     */
    val transportType: TransportType,

    /**
     * Contains all the capabilities of the network
     */
    val capabilities: List<NetworkCapability>,

    /**
     * Contains all raw capabilities of the network
     */
    val capabilitiesRaw: NetworkCapabilities?,

    /**
     * Retrieves the downstream bandwidth for this network in Kbps.  This always only refers to
     * the estimated first hop downstream transport (network to device) bandwidth.
     */
    val linkDownstreamBandwidthKbps: Int,

    /**
     * Retrieves the upstream bandwidth for this network in Kbps.  This always only refers to
     * the estimated first hop upstream transport (device to network)  bandwidth.
     */
    val linkUpstreamBandwidthKbps: Int
)