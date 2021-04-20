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

import at.specure.info.network.DetailedNetworkInfo
import at.specure.info.network.NetworkInfo

/**
 * Watcher that is responsible for tracking signal strength of active network
 */
interface SignalStrengthWatcher {

    /**
     * The last received signal strength
     */
    val lastSignalStrength: SignalStrengthInfo?

    /**
     * The last received networkInfo
     */
    val lastNetworkInfo: NetworkInfo?

    /**
     * Add listener to start receiving updates of [SignalStrengthInfo]
     */
    fun addListener(listener: SignalStrengthListener)

    /**
     * Remove listener from receiving signal strength updates
     */
    fun removeListener(listener: SignalStrengthListener)

    /**
     * Callback that is used by [SignalStrengthWatcher] to receive [SignalStrengthInfo] updates of currently active network
     */
    interface SignalStrengthListener {

        /**
         * Triggers when signal data of currently active network is updated
         * NULL will be triggered if no active network available
         */
        fun onSignalStrengthChanged(signalInfo: DetailedNetworkInfo?)
    }
}