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

import android.content.Context
import android.net.ConnectivityManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import at.rmbt.client.control.getCorrectDataTelephonyManager
import at.rmbt.client.control.getCurrentDataSubscriptionId
import at.rmbt.util.io
import at.specure.config.Config
import at.specure.info.Network5GSimulator
import at.specure.info.strength.SignalStrengthInfo
import at.specure.util.*
import cz.mroczis.netmonster.core.INetMonster
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.feature.merge.CellSource
import cz.mroczis.netmonster.core.model.cell.ICell
import timber.log.Timber

private const val INVALID_SUBSCRIPTION_ID = -1

/**
 * Default implementation of [CellInfoWatcher] that is using to track Cellular network information
 */
class CellInfoWatcherImpl(
    private val context: Context,
    private val telephonyManager: TelephonyManager,
    private val connectivityManager: ConnectivityManager,
    private val netMonster: INetMonster,
    private val subscriptionManager: SubscriptionManager,
    private val config: Config
) : CellInfoWatcher {

    private var _activeNetwork: CellNetworkInfo? = null
    private var _secondaryNetworks: List<CellNetworkInfo?> = mutableListOf()
    private var _signalStrengthInfo: SignalStrengthInfo? = null
    private var _secondarySignalStrengthInfos: List<SignalStrengthInfo?> = mutableListOf()

    override val activeNetwork: CellNetworkInfo?
        get() = _activeNetwork

    override val secondaryNetworks: List<CellNetworkInfo?>
        get() = _secondaryNetworks

    override val signalStrengthInfo: SignalStrengthInfo?
        get() = _signalStrengthInfo

    override val secondarySignalStrengthInfos: List<SignalStrengthInfo?>
        get() = _secondarySignalStrengthInfos

    override fun updateInfo() = io {

        if (context.isCoarseLocationPermitted() && context.isReadPhoneStatePermitted()) {
            try {
                var cells: List<ICell>? = null

                cells = netMonster.getCells(
                    CellSource.NEIGHBOURING_CELLS,
                    CellSource.ALL_CELL_INFO,
                    CellSource.CELL_LOCATION
                )

                val dataSubscriptionId = subscriptionManager.getCurrentDataSubscriptionId()

                val primaryCells = cells?.filterOnlyPrimaryActiveDataCell(dataSubscriptionId)
                val secondaryCells = cells?.filterOnlySecondaryActiveDataCell(dataSubscriptionId)
                val secondary5GCells = secondaryCells.filter5GCells()

                when (primaryCells.size) {
                    0 -> {
                        _activeNetwork = null
                        _signalStrengthInfo = null
                        (_secondaryNetworks as MutableList).clear()
                        (_secondarySignalStrengthInfos as MutableList).clear()
                    }
                    1 -> {
                        _activeNetwork = primaryCells[0].toCellNetworkInfo(
                            connectivityManager.activeNetworkInfo?.extraInfo,
                            telephonyManager.getCorrectDataTelephonyManager(subscriptionManager),
                            NetMonsterFactory.getTelephony(context, primaryCells[0].subscriptionId),
                            netMonster
                        )
                        _signalStrengthInfo =
                            primaryCells[0].signal?.toSignalStrengthInfo(System.nanoTime())

                        (_secondaryNetworks as MutableList).clear()
                        (_secondarySignalStrengthInfos as MutableList).clear()
                        if (config.developer5GSimulationEnabled) {
                            val cellInfo5G =
                                Network5GSimulator.fromInfo(true, false, "5G simulator")

                            (_secondaryNetworks as MutableList).add(cellInfo5G)
                            _signalStrengthInfo?.let {
                                val signalStrength5G =
                                    Network5GSimulator.signalStrength(_signalStrengthInfo!!)
                                (_secondarySignalStrengthInfos as MutableList).add(signalStrength5G)
                            }
                            Timber.d("Simulated 5G: ${cellInfo5G.providerName}  ${_secondaryNetworks.size} ${_secondarySignalStrengthInfos.size}")
                        } else {
                            secondary5GCells.forEach {
                                val cellInfo5G = it.toCellNetworkInfo(
                                    connectivityManager.activeNetworkInfo?.extraInfo,
                                    telephonyManager.getCorrectDataTelephonyManager(
                                        subscriptionManager
                                    ),
                                    NetMonsterFactory.getTelephony(context, it.subscriptionId),
                                    netMonster
                                )
                                (_secondaryNetworks as MutableList).add(cellInfo5G)
                                val signal5G = it.signal?.toSignalStrengthInfo(System.nanoTime())
                                (_secondarySignalStrengthInfos as MutableList).add(signal5G)
                            }
                        }
                    }
                    else -> {
                        (_secondaryNetworks as MutableList).clear()
                        (_secondarySignalStrengthInfos as MutableList).clear()
                        // ignore, inconsistent state
                    }
                }
            } catch (e: SecurityException) {
                Timber.e("SecurityException: Not able to read telephonyManager.allCellInfo")
            } catch (e: IllegalStateException) {
                Timber.e("IllegalStateException: Not able to read telephonyManager.allCellInfo")
            } catch (e: NullPointerException) {
                Timber.e("NullPointerException: Not able to read telephonyManager.allCellInfo from other reason")
            }
        }
    }
}