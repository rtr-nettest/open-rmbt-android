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

package at.specure.info.ip

import at.rmbt.client.control.IpProtocol

/**
 * Data class that describes IPv6 state
 */
data class IpInfo(
    /**
     * Ip protocol used [IpProtocol.V4] or [IpProtocol.V6]
     */
    val protocol: IpProtocol,

    /**
     * Private address of device in the local network
     */
    val privateAddress: String?,

    /**
     * Public address of network
     */
    val publicAddress: String?,

    /**
     * Ip status regarding to values from [IpStatus]
     */
    val ipStatus: IpStatus,

    /**
     * Captive portal status [CaptivePortal.CaptivePortalStatus]
     */
    val captivePortalStatus: CaptivePortal.CaptivePortalStatus
)