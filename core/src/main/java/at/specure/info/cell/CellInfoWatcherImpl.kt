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
import at.specure.info.network.MobileNetworkType
import at.specure.info.strength.SignalStrengthInfo
import at.specure.util.filter5GCells
import at.specure.util.filterOnlyPrimaryActiveDataCell
import at.specure.util.filterOnlySecondaryActiveDataCell
import at.specure.util.isCoarseLocationPermitted
import at.specure.util.isReadPhoneStatePermitted
import at.specure.util.mobileNetworkType
import at.specure.util.toCellNetworkInfo
import at.specure.util.toSignalStrengthInfo
import cz.mroczis.netmonster.core.INetMonster
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.model.cell.CellLte
import cz.mroczis.netmonster.core.model.cell.CellNr
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
    private var _secondaryActiveCellNetworks: List<CellNetworkInfo?> = mutableListOf()
    private var _secondary5GActiveCellNetworks: List<CellNetworkInfo?> = mutableListOf()
    private var _signalStrengthInfo: SignalStrengthInfo? = null
    private var _secondaryActiveCellSignalStrengthInfos: List<SignalStrengthInfo?> = mutableListOf()
    private var _secondary5GActiveCellSignalStrengthInfos: List<SignalStrengthInfo?> =
        mutableListOf()

    override val activeNetwork: CellNetworkInfo?
        get() = _activeNetwork

    override val secondaryActiveCellNetworks: List<CellNetworkInfo?>
        get() = _secondaryActiveCellNetworks

    override val secondary5GActiveCellNetworks: List<CellNetworkInfo?>
        get() = _secondary5GActiveCellNetworks

    override val signalStrengthInfo: SignalStrengthInfo?
        get() = _signalStrengthInfo

    override val secondaryActiveCellSignalStrengthInfos: List<SignalStrengthInfo?>
        get() = _secondaryActiveCellSignalStrengthInfos

    override val secondary5GActiveCellSignalStrengthInfos: List<SignalStrengthInfo?>
        get() = _secondary5GActiveCellSignalStrengthInfos

    override fun updateInfo() = io {

        if (context.isCoarseLocationPermitted() && context.isReadPhoneStatePermitted()) {
            try {
                var cells: List<ICell>? = null

                cells = netMonster.getCells()

                val dataSubscriptionId = subscriptionManager.getCurrentDataSubscriptionId()

                val primaryCells = cells?.filterOnlyPrimaryActiveDataCell(dataSubscriptionId)
                val secondaryCells = cells?.filterOnlySecondaryActiveDataCell(dataSubscriptionId)
                val secondary5GCells = secondaryCells.filter5GCells()

                Timber.d("size ${primaryCells.size}")
                primaryCells.forEach {
                    Timber.d("size ${primaryCells.size} primaryCells: $it}")
                }

                // in some cases for 5G NSA there are 2 primary connections reported - 1. for 4G and 2. for 5G cell,
                // so we need to move that 5G from primary connection to secondary list
                var primaryCellsCorrected = mutableListOf<ICell>()
                var secondaryCellsCorrected = mutableListOf<ICell>()
                var secondary5GCellsCorrected = mutableListOf<ICell>()
                when (primaryCells.size) {
                    2 -> {
                        if (primaryCells[0] is CellNr && primaryCells[0].mobileNetworkType(
                                netMonster
                            ) == MobileNetworkType.NR_NSA && primaryCells[1] is CellLte
                        ) {
                            secondary5GCellsCorrected = secondary5GCells as MutableList<ICell>
                            secondary5GCellsCorrected.add(primaryCells[0])
                            primaryCellsCorrected.add(primaryCells[1])
                        } else if (primaryCells[1] is CellNr && primaryCells[1].mobileNetworkType(
                                netMonster
                            ) == MobileNetworkType.NR_NSA && primaryCells[0] is CellLte
                        ) {
                            secondary5GCellsCorrected = secondary5GCells as MutableList<ICell>
                            secondary5GCellsCorrected.add(primaryCells[1])
                            primaryCellsCorrected.add(primaryCells[0])
                        }
                        secondaryCellsCorrected = secondaryCells as MutableList<ICell>
                    }
                    else -> {
                        primaryCellsCorrected = primaryCells as MutableList<ICell>
                        secondaryCellsCorrected = secondaryCells as MutableList<ICell>
                        secondary5GCellsCorrected = secondary5GCells as MutableList<ICell>
                    }
                }

                when (primaryCellsCorrected.size) {
                    0 -> {
                        _activeNetwork = null
                        _signalStrengthInfo = null
                        clearLists()
                    }
                    1 -> {
                        _activeNetwork = primaryCellsCorrected[0].toCellNetworkInfo(
                            connectivityManager.activeNetworkInfo?.extraInfo,
                            telephonyManager.getCorrectDataTelephonyManager(subscriptionManager),
                            NetMonsterFactory.getTelephony(
                                context,
                                primaryCellsCorrected[0].subscriptionId
                            ),
                            netMonster
                        )
                        _signalStrengthInfo =
                            primaryCellsCorrected[0].signal?.toSignalStrengthInfo(System.nanoTime())

                        clearLists()
                        if (config.developer5GSimulationEnabled) {
                            val cellInfo5G =
                                Network5GSimulator.fromInfo(true, false, "5G simulator")

                            (_secondary5GActiveCellNetworks as MutableList).add(cellInfo5G)
                            _signalStrengthInfo?.let {
                                val signalStrength5G =
                                    Network5GSimulator.signalStrength(_signalStrengthInfo!!)
                                (_secondary5GActiveCellSignalStrengthInfos as MutableList).add(
                                    signalStrength5G
                                )
                            }
                            Timber.d("Simulated 5G: ${cellInfo5G.providerName}  ${_secondary5GActiveCellNetworks.size} ${_secondary5GActiveCellSignalStrengthInfos.size}")
                        } else {
                            secondary5GCellsCorrected.forEach {
                                val cellInfo5G = it.toCellNetworkInfo(
                                    connectivityManager.activeNetworkInfo?.extraInfo,
                                    telephonyManager.getCorrectDataTelephonyManager(
                                        subscriptionManager
                                    ),
                                    NetMonsterFactory.getTelephony(context, it.subscriptionId),
                                    netMonster
                                )
                                (_secondary5GActiveCellNetworks as MutableList).add(cellInfo5G)
                                val signal5G = it.signal?.toSignalStrengthInfo(System.nanoTime())
                                (_secondary5GActiveCellSignalStrengthInfos as MutableList).add(
                                    signal5G
                                )
                            }
                        }
                        secondaryCellsCorrected.forEach {
                            val cellInfoSecondary = it.toCellNetworkInfo(
                                connectivityManager.activeNetworkInfo?.extraInfo,
                                telephonyManager.getCorrectDataTelephonyManager(
                                    subscriptionManager
                                ),
                                NetMonsterFactory.getTelephony(context, it.subscriptionId),
                                netMonster
                            )
                            (_secondaryActiveCellNetworks as MutableList).add(cellInfoSecondary)
                            val signalSecondary = it.signal?.toSignalStrengthInfo(System.nanoTime())
                            (_secondaryActiveCellSignalStrengthInfos as MutableList).add(
                                signalSecondary
                            )
                        }
                    }
                    else -> {
                        clearLists()
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

    private fun clearLists() {
        (_secondary5GActiveCellNetworks as MutableList).clear()
        (_secondary5GActiveCellSignalStrengthInfos as MutableList).clear()
        (_secondaryActiveCellNetworks as MutableList).clear()
        (_secondaryActiveCellSignalStrengthInfos as MutableList).clear()
    }
}