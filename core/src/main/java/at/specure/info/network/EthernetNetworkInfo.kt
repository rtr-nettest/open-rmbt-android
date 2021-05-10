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

package at.specure.info.network

import at.specure.info.TransportType
import java.util.UUID

/**
 * Data object that contains information about WiFi network
 */
open class EthernetNetworkInfo(

    /**
     * The current link speed in Mbps.
     */
    val linkSpeed: Int?,

    /**
     * Each configured network has a unique small integer ID, used to identify
     * the network when performing operations on the supplicant. This method
     * returns the ID for the currently connected network.
     * Network ID, or null if there is no currently connected network or data is not available
     */
    val networkId: Int?,

    override val name: String? = null

) : NetworkInfo(TransportType.ETHERNET, UUID.nameUUIDFromBytes(("").toByteArray()).toString())