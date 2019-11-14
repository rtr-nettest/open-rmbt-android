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

/**
 * Ip change watcher that tracks changes of IPv4 and IPv6 address changes for an active network
 */
interface IpChangeWatcher {

    /**
     * The latest IPv4 available for current active network
     */
    val lastIPv4Address: IpInfo

    /**
     * The latest IPv6 available for current active network
     */
    val lastIPv6Address: IpInfo

    /**
     * Request an IPv4 address update
     */
    fun updateIpV4()

    /**
     * Request an IPv6 address update
     */
    fun updateIpV6()

    /**
     * Add listener for IPv4 address changes
     */
    fun addListener(listener: OnIpV4ChangedListener)

    /**
     * Remove listener from IPv4 address change tracking
     */
    fun removeListener(listener: OnIpV4ChangedListener)

    /**
     * Add listener for IPv6 address changes
     */
    fun addListener(listener: OnIpV6ChangedListener)

    /**
     * Remove listener from IPv6 address change tracking
     */
    fun removeListener(listener: OnIpV6ChangedListener)

    /**
     * Listener to track IPv4 changes
     */
    interface OnIpV4ChangedListener {

        /**
         * Method will be called after IPv4 change was detected
         */
        fun onIpV4Changed(info: IpInfo)
    }

    /**
     * Listener to track IPv6 changes
     */
    interface OnIpV6ChangedListener {

        /**
         * Method will be called after IPv6 change was detected
         */
        fun onIpV6Changed(info: IpInfo)
    }
}