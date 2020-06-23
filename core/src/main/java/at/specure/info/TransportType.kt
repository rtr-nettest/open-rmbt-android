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

package at.specure.info

import android.annotation.TargetApi
import android.net.NetworkCapabilities
import android.os.Build
import at.specure.data.ServerNetworkType

/**
 * Enum that represents [android.net.NetworkCapabilities] constants for transport types
 */
enum class TransportType(
    /**
     * Value that corresponds to one of [android.net.NetworkCapabilities] TRANSPORT_* constants
     */
    val value: Int
) {
    /**
     * Indicates this network uses a Cellular transport.
     */
    CELLULAR(NetworkCapabilities.TRANSPORT_CELLULAR),

    /**
     * Indicates this network uses a Wi-Fi transport.
     */
    WIFI(NetworkCapabilities.TRANSPORT_WIFI),

    /**
     * Indicates this network uses a Bluetooth transport.
     */
    BLUETOOTH(NetworkCapabilities.TRANSPORT_BLUETOOTH),

    /**
     * Indicates this network uses an Ethernet transport.
     */
    ETHERNET(NetworkCapabilities.TRANSPORT_ETHERNET),

    /**
     * Indicates this network uses a VPN transport.
     */
    VPN(NetworkCapabilities.TRANSPORT_VPN),

    /**
     * Indicates this network uses a Wi-Fi Aware transport.
     */
    @TargetApi(Build.VERSION_CODES.O)
    WIFI_AWARE(NetworkCapabilities.TRANSPORT_WIFI_AWARE),

    /**
     * Indicates this network uses a LoWPAN transport.
     */
    @TargetApi(Build.VERSION_CODES.O_MR1)
    LOWPAN(NetworkCapabilities.TRANSPORT_LOWPAN),

    BROWSER(ServerNetworkType.TYPE_UNKNOWN.intValue);

    companion object {

        /**
         * Returns list of supported transport types by this Android OS version
         */
        val supportedTransports: List<TransportType>
            get() {
                val result = mutableListOf<TransportType>()

                values().forEach {
                    when (it) {
                        WIFI_AWARE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            result.add(it)
                        }
                        LOWPAN -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                            result.add(it)
                        }
                        else -> result.add(it)
                    }
                }

                return result
            }

        fun fromNetworkCapability(capabilities: NetworkCapabilities): TransportType {
            values().forEach {
                if (capabilities.hasTransport(it.value)) {
                    return it
                }
            }

            throw IllegalArgumentException("Unable to find network type in $capabilities")
        }
    }
}