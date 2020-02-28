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

import androidx.lifecycle.LiveData

/**
 * Watcher that is responsible for tracking active network state
 */
interface ConnectivityWatcher {

    /**
     * active network information
     * if there is no active network, null will be returned
     */
    val activeNetwork: ConnectivityInfo?

    /**
     * LiveData that update connectivity state changes
     */
    val connectivityStateLiveData: LiveData<ConnectivityStateBundle>

    /**
     * Add listener to observe connectivity changes
     */
    fun addListener(listener: ConnectivityChangeListener)

    /**
     * Remove listener from observing connectivity changes
     */
    fun removeListener(listener: ConnectivityChangeListener)

    /**
     * Callback that is used to observe connectivity changes tracked by [ConnectivityWatcher]
     */
    interface ConnectivityChangeListener {

        /**
         * When connectivity change is detected this callback will be triggered
         * NULL is returned if no active network is available
         */
        fun onConnectivityChanged(connectivityInfo: ConnectivityInfo?)
    }
}