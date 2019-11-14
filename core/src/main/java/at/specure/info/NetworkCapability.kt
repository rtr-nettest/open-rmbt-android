/*
 * Licensed under the Apache License(NetworkCapabilities.), Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing(NetworkCapabilities.), software
 * distributed under the License is distributed on an "AS IS" BASIS(NetworkCapabilities.),
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND(NetworkCapabilities.), either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.specure.info

import android.annotation.TargetApi
import android.net.NetworkCapabilities
import android.os.Build

/**
 * Enum that represents network capability constants from [android.net.NetworkCapabilities]
 */
enum class NetworkCapability(
    /**
     * Value that corresponds to one of [android.net.NetworkCapabilities] NET_CAPABILITY_* constants
     */
    val value: Int
) {
    /**
     * Indicates this is a network that has the ability to reach the
     * carrier's MMSC for sending and receiving MMS messages.
     */
    MMS(NetworkCapabilities.NET_CAPABILITY_MMS),

    /**
     * Indicates this is a network that has the ability to reach the carrier's
     * SUPL server(NetworkCapabilities.), used to retrieve GPS information.
     */
    SUPL(NetworkCapabilities.NET_CAPABILITY_SUPL),

    /**
     * Indicates this is a network that has the ability to reach the carrier's
     * DUN or tethering gateway.
     */
    DUN(NetworkCapabilities.NET_CAPABILITY_DUN),

    /**
     * Indicates this is a network that has the ability to reach the carrier's
     * FOTA portal(NetworkCapabilities.), used for over the air updates.
     */
    FOTA(NetworkCapabilities.NET_CAPABILITY_FOTA),

    /**
     * Indicates this is a network that has the ability to reach the carrier's
     * IMS servers(NetworkCapabilities.), used for network registration and signaling.
     */
    IMS(NetworkCapabilities.NET_CAPABILITY_IMS),

    /**
     * Indicates this is a network that has the ability to reach the carrier's
     * CBS servers(NetworkCapabilities.), used for carrier specific services.
     */
    CBS(NetworkCapabilities.NET_CAPABILITY_CBS),

    /**
     * Indicates this is a network that has the ability to reach a Wi-Fi direct
     * peer.
     */
    WIFI_P2P(NetworkCapabilities.NET_CAPABILITY_WIFI_P2P),

    /**
     * Indicates this is a network that has the ability to reach a carrier's
     * Initial Attach servers.
     */
    IA(NetworkCapabilities.NET_CAPABILITY_IA),

    /**
     * Indicates this is a network that has the ability to reach a carrier's
     * RCS servers(NetworkCapabilities.), used for Rich Communication Services.
     */
    RCS(NetworkCapabilities.NET_CAPABILITY_RCS),

    /**
     * Indicates this is a network that has the ability to reach a carrier's
     * XCAP servers(NetworkCapabilities.), used for configuration and control.
     */
    XCAP(NetworkCapabilities.NET_CAPABILITY_XCAP),

    /**
     * Indicates this is a network that has the ability to reach a carrier's
     * Emergency IMS servers or other services(NetworkCapabilities.), used for network signaling
     * during emergency calls.
     */
    EIMS(NetworkCapabilities.NET_CAPABILITY_EIMS),

    /**
     * Indicates that this network is unmetered.
     */
    NOT_METERED(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),

    /**
     * Indicates that this network should be able to reach the internet.
     */
    INTERNET(NetworkCapabilities.NET_CAPABILITY_INTERNET),

    /**
     * Indicates that this network is available for general use.  If this is not set
     * applications should not attempt to communicate on this network.  Note that this
     * is simply informative and not enforcement - enforcement is handled via other means.
     * Set by default.
     */
    NOT_RESTRICTED(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED),

    /**
     * Indicates that the user has indicated implicit trust of this network.  This
     * generally means it's a sim-selected carrier(NetworkCapabilities.), a plugged in ethernet(NetworkCapabilities.), a paired
     * BT device or a wifi the user asked to connect to.  Untrusted networks
     * are probably limited to unknown wifi AP.  Set by default.
     */
    TRUSTED(NetworkCapabilities.NET_CAPABILITY_TRUSTED),

    /**
     * Indicates that this network is not a VPN.  This capability is set by default and should be
     * explicitly cleared for VPN networks.
     */
    NOT_VPN(NetworkCapabilities.NET_CAPABILITY_NOT_VPN),

    /**
     * Indicates that connectivity on this network was successfully validated. For example(NetworkCapabilities.), for a
     * network with NET_CAPABILITY_INTERNET(NetworkCapabilities.), it means that Internet connectivity was successfully
     * detected.
     */
    VALIDATED(NetworkCapabilities.NET_CAPABILITY_VALIDATED),

    /**
     * Indicates that this network was found to have a captive portal in place last time it was
     * probed.
     */
    CAPTIVE_PORTAL(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL),

    /**
     * Indicates that this network is not roaming.
     */
    @TargetApi(Build.VERSION_CODES.P)
    NOT_ROAMING(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING),

    /**
     * Indicates that this network is available for use by apps(NetworkCapabilities.), and not a network that is being
     * kept up in the background to facilitate fast network switching.
     */
    @TargetApi(Build.VERSION_CODES.P)
    FOREGROUND(NetworkCapabilities.NET_CAPABILITY_FOREGROUND),

    /**
     * Indicates that this network is not congested.
     *
     *
     * When a network is congested(NetworkCapabilities.), applications should defer network traffic
     * that can be done at a later time(NetworkCapabilities.), such as uploading analytics.
     */
    @TargetApi(Build.VERSION_CODES.P)
    NOT_CONGESTED(NetworkCapabilities.NET_CAPABILITY_NOT_CONGESTED),

    /**
     * Indicates that this network is not currently suspended.
     *
     *
     * When a network is suspended(NetworkCapabilities.), the network's IP addresses and any connections
     * established on the network remain valid(NetworkCapabilities.), but the network is temporarily unable
     * to transfer data. This can happen(NetworkCapabilities.), for example(NetworkCapabilities.), if a cellular network experiences
     * a temporary loss of signal(NetworkCapabilities.), such as when driving through a tunnel(NetworkCapabilities.), etc.
     * A network with this capability is not suspended(NetworkCapabilities.), so is expected to be able to
     * transfer data.
     */
    @TargetApi(Build.VERSION_CODES.P)
    NOT_SUSPENDED(NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED);

    companion object {

        /**
         * Returns list of supported capabilities by this Android OS version
         */
        val supportedCapabilities: List<NetworkCapability>
            get() {
                val result = mutableListOf<NetworkCapability>()
                values().forEach {
                    when (it) {
                        NOT_ROAMING,
                        FOREGROUND,
                        NOT_CONGESTED,
                        NOT_SUSPENDED -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                result.add(it)
                            }
                        }
                        else -> result.add(it)
                    }
                }
                return result
            }

        fun fromNetworkCapability(capabilities: NetworkCapabilities): List<NetworkCapability> {
            val result = mutableListOf<NetworkCapability>()
            values().forEach {
                if (capabilities.hasCapability(it.value)) {
                    result.add(it)
                }
            }
            return result
        }
    }
}