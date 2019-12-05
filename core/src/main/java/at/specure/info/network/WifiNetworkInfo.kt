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

import android.net.wifi.SupplicantState
import at.specure.info.TransportType
import at.specure.info.band.WifiBand
import java.util.UUID

/**
 * Data object that contains information about WiFi network
 */
open class WifiNetworkInfo(
    /**
     * Return the basic service set identifier (BSSID) of the current access point.
     * The BSSID in the form of a six-byte MAC address: {@code XX:XX:XX:XX:XX:XX},
     * may be {@code null} if there is no network currently connected.
     */
    val bssid: String?,

    /**
     * Wifi Band and frequency information
     */
    val band: WifiBand,

    /**
     * {@code true} if this network does not broadcast its SSID, so an
     * SSID-specific probe request must be used for scans.
     */
    val isSSIDHidden: Boolean,

    /**
     * Device IP address in WiFi network
     */
    val ipAddress: String?,

    /**
     * The current link speed in Mbps.
     */
    val linkSpeed: Int,

    /**
     * Each configured network has a unique small integer ID, used to identify
     * the network when performing operations on the supplicant. This method
     * returns the ID for the currently connected network.
     * Network ID, or -1 if there is no currently connected network
     */
    val networkId: Int,

    /**
     * Returns the received signal strength indicator of the current 802.11
     * network, in dBm.
     */
    val rssi: Int,

    /**
     * Wifi signal level in range from 0 to 100
     */
    val signalLevel: Int,

    /**
     * Returns the service set identifier (SSID) of the current 802.11 network.
     * If the SSID can be decoded as UTF-8, it will be returned surrounded by double
     * quotation marks. Otherwise, it is returned as a string of hex digits. The
     * SSID may be &lt;unknown ssid&gt; if there is no network currently connected,
     * or if the caller has insufficient permissions to access the SSID.
     */
    val ssid: String,

    /**
     * Return the state of the supplicant's negotiation with an
     * access point, in the form of a [android.net.wifi.SupplicantState] object.
     */
    val supplicantState: SupplicantState,

    /** Return the detailed state of the supplicant's negotiation with an
     * access point, in the form of a [android.net.NetworkInfo.DetailedState] object.
     */
    val supplicantDetailedState: android.net.NetworkInfo.DetailedState

) : NetworkInfo(TransportType.WIFI, UUID.nameUUIDFromBytes(ssid.toByteArray()).toString()) {

    override val name: String?
        get() = ssid
}