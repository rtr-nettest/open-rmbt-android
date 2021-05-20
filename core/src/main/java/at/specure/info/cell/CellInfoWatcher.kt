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

package at.specure.info.cell

import android.telephony.CellInfo
import at.specure.info.network.NRConnectionState

/**
 * Watcher that is responsible for tracking cellular connection info
 */
interface CellInfoWatcher {

    /**
     * Currently active data cellular network
     * NULL if there is no active cellular network
     */
    val activeNetwork: CellNetworkInfo?

    val cellInfo: CellInfo?

    val allCellInfo: List<CellNetworkInfo>

    val rawAllCellInfo: List<CellInfo>

    /**
     * NR connection type default should be [NRConnectionState.NOT_AVAILABLE]
     */
    val nrConnectionState: NRConnectionState

    /**
     * Add listener to observe cell network changes
     */
    fun addListener(listener: CellInfoChangeListener)

    /**
     * Remove listener from observing cell network changes
     */
    fun removeListener(listener: CellInfoChangeListener)

    /**
     * Try update cellular network info without callbacks
     */
    fun forceUpdate()

    /**
     * updating current cell info from netmonster library and extracting information
     */
    fun updateCellNetworkInfo(): CellNetworkInfo?

    /**
     * Callback that is used to observe cellular network changes tracked by [CellInfoWatcher]
     */
    interface CellInfoChangeListener {

        /**
         * When cellular network change is detected this callback will be triggered
         * if no active network or cell network is available null will be returned
         */
        fun onCellInfoChanged(activeNetwork: CellNetworkInfo?)
    }
}