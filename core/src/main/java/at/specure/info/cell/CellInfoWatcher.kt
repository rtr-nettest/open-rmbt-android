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

import at.specure.info.strength.SignalStrengthInfo
import cz.mroczis.netmonster.core.model.cell.ICell

/**
 * Watcher that is responsible for tracking cellular connection info
 */
interface CellInfoWatcher {

    /**
     * Currently active data cellular network
     * NULL if there is no active cellular network
     */
    val activeNetwork: CellNetworkInfo?

    val signalStrengthInfo: SignalStrengthInfo?

    /**
     * list of all secondary cells available for current data subscription from which is active
     */
    val secondaryActiveCellNetworks: List<CellNetworkInfo?>

    /**
     * list of all cells available for all subscriptions
     */
    val allCellInfos: List<ICell>

    /**
     * list of all 5G cells from secondary cells available for current data subscription from which is active
     */
    val secondary5GActiveCellNetworks: List<CellNetworkInfo?>

    /**
     * list of all signals from secondary cells available for current data subscription from which is active
     */
    val secondaryActiveCellSignalStrengthInfos: List<SignalStrengthInfo?>

    /**
     * list of all 5G signals from secondary cells available for current data subscription from which is active
     */
    val secondary5GActiveCellSignalStrengthInfos: List<SignalStrengthInfo?>

    fun updateInfo()
}