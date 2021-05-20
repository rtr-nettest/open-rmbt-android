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

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.telephony.CellInfo
import android.telephony.PhoneStateListener
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import at.rmbt.client.control.getCorrectDataTelephonyManager
import at.rmbt.client.control.getCurrentDataSubscriptionId
import at.specure.info.network.NRConnectionState
import at.specure.util.filterOnlyActiveDataCell
import at.specure.util.isCoarseLocationPermitted
import at.specure.util.isReadPhoneStatePermitted
import at.specure.util.permission.LocationAccess
import at.specure.util.permission.PhoneStateAccess
import at.specure.util.synchronizedForEach
import at.specure.util.toCellNetworkInfo
import cz.mroczis.netmonster.core.INetMonster
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import cz.mroczis.netmonster.core.feature.merge.CellSource
import cz.mroczis.netmonster.core.model.cell.ICell
import timber.log.Timber
import java.util.Collections
import java.util.Timer
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

private const val INVALID_SUBSCRIPTION_ID = -1

/**
 * Default implementation of [CellInfoWatcher] that is using to track Cellular network information
 */
class CellInfoWatcherImpl(
    private val context: Context,
    private val telephonyManager: TelephonyManager,
    private val locationAccess: LocationAccess,
    private val phoneStateAccess: PhoneStateAccess,
    private val connectivityManager: ConnectivityManager,
    private val activeDataCellInfoExtractor: ActiveDataCellInfoExtractor,
    private val netMonster: INetMonster,
    private val subscriptionManager: SubscriptionManager
) : CellInfoWatcher,
    LocationAccess.LocationAccessChangeListener,
    PhoneStateAccess.PhoneStateAccessChangeListener {

    private var timer: Timer? = null
    private val listeners = Collections.synchronizedSet(mutableSetOf<CellInfoWatcher.CellInfoChangeListener>())

    private var _activeNetwork: CellNetworkInfo? = null
    private var callbacksRegistered = false
    private var _cellInfo: CellInfo? = null
    private var _allCellInfo = Collections.synchronizedList(mutableListOf<CellNetworkInfo>())
    private var _rawAllCellInfo = Collections.synchronizedList(mutableListOf<CellInfo>())
    private var _nrConnectionState = NRConnectionState.NOT_AVAILABLE

    override val cellInfo: CellInfo?
        get() = _cellInfo

    override val allCellInfo: List<CellNetworkInfo>
        get() = _allCellInfo

    override val rawAllCellInfo: List<CellInfo>
        get() = _rawAllCellInfo

    override val nrConnectionState: NRConnectionState
        get() = _nrConnectionState

    init {
        locationAccess.addListener(this)
        phoneStateAccess.addListener(this)
    }

    private val infoListener = object : PhoneStateListener() {

        override fun onCellInfoChanged(cellInfos: MutableList<CellInfo>?) {
            processCellInfos(cellInfos, "callback")
        }
    }

    override fun updateCellNetworkInfo(): CellNetworkInfo? {

        if (context.isCoarseLocationPermitted() && context.isReadPhoneStatePermitted()) {
            try {
                var cells: List<ICell>? = null

                cells = netMonster.getCells(CellSource.NEIGHBOURING_CELLS, CellSource.ALL_CELL_INFO, CellSource.CELL_LOCATION)

                val dataSubscriptionId = subscriptionManager.getCurrentDataSubscriptionId()

                val primaryCells = cells?.filterOnlyActiveDataCell(dataSubscriptionId)

                if (primaryCells.size == 1) {
                    _activeNetwork = primaryCells[0].toCellNetworkInfo(
                        connectivityManager.activeNetworkInfo?.extraInfo,
                        telephonyManager.getCorrectDataTelephonyManager(subscriptionManager),
                        NetMonsterFactory.getTelephony(context, primaryCells[0].subscriptionId),
                        netMonster
                    )
                    // dual sims
                } else {
                    Timber.e("NM network type unable to detect because of dual sim")
                }

                return _activeNetwork

            } catch (e: SecurityException) {
                Timber.e("SecurityException: Not able to read telephonyManager.allCellInfo")
            } catch (e: IllegalStateException) {
                Timber.e("IllegalStateException: Not able to read telephonyManager.allCellInfo")
            } catch (e: NullPointerException) {
                Timber.e("NullPointerException: Not able to read telephonyManager.allCellInfo from other reason")
            }
        }
        return null
    }

    @Synchronized
    private fun processCellInfos(cellInfos: MutableList<CellInfo>?, source: String) {
        /*   Timber.d("CellInfosChanged: is null? ${cellInfos.isNullOrEmpty()} empty? ${cellInfos?.isEmpty()} from $source")
           cellInfos ?: return
           _rawAllCellInfo = cellInfos
           try {
               val activeDataCellInfo = activeDataCellInfoExtractor.extractActiveCellInfo(cellInfos)

               Timber.d("CellInfosChanged: is null? activeDataNetworkCellInfo? ${activeDataCellInfo.activeDataNetworkCellInfo == null} inconstent ${!activeDataCellInfo.isConsistent}")
               if (!activeDataCellInfo.isConsistent) return

               activeDataCellInfo.activeDataNetworkCellInfo?.let {
                   _cellInfo = it
               }

               activeDataCellInfo.activeDataNetwork?.let {
                   _activeNetwork = it
                   Timber.d("Active Network ${it.cellUUID} ${it.networkType}")
               }
               _nrConnectionState = activeDataCellInfo.nrConnectionState

               _allCellInfo.clear()
               cellInfos.forEach {
                   val info = CellNetworkInfo.from(
                       it,
                       null,
                       activeDataCellInfo.activeDataNetwork?.cellUUID == it.uuid(),
                       connectivityManager.activeNetworkInfo?.isRoaming ?: false,
                       connectivityManager.activeNetworkInfo?.extraInfo,
                       activeDataCellInfo.dualSimDecision,
                       _nrConnectionState
                   )
                   _allCellInfo.add(info)
                   Timber.v("cell: ${info.networkType.displayName} ${info.mnc} ${info.mcc} ${info.cellUUID}")
               }
           } catch (e: SecurityException) {
               Timber.e("SecurityException: Not able to read telephonyManager.allCellInfo")
           } catch (e: IllegalStateException) {
               Timber.e("IllegalStateException: Not able to read telephonyManager.allCellInfo")
           } catch (e: NullPointerException) {
               Timber.e("NullPointerException: Not able to read telephonyManager.allCellInfo from other reason")
           }

           notifyListeners()*/
    }

    @SuppressLint("MissingPermission")
    override fun forceUpdate() {
        /* try {
             if (context.isCoarseLocationPermitted() && phoneStateAccess.isAllowed) {
                 val cells = telephonyManager.getCorrectDataTelephonyManager(subscriptionManager).allCellInfo
                 if (!cells.isNullOrEmpty()) {
                     infoListener.onCellInfoChanged(cells)
                 }
             }
         } catch (ex: Exception) {
             Timber.w(ex, "Failed to update cell info")
         }*/
    }

    override val activeNetwork: CellNetworkInfo?
        get() {
            return updateCellNetworkInfo()
//            if (_activeNetwork == null) {
//                return if (isDualSim() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//                    try {
//                        val dataSubscriptionId = subscriptionManager.getCurrentDataSubscriptionId()
//                        if (dataSubscriptionId != INVALID_SUBSCRIPTION_ID) {
//                            connectivityManager.cellNetworkInfoCompat(
//                                telephonyManager.createForSubscriptionId(dataSubscriptionId).networkOperatorName,
//                                _nrConnectionState
//                            )
//                        } else {
//                            connectivityManager.cellNetworkInfoCompat(
//                                telephonyManager.getCorrectDataTelephonyManager(subscriptionManager).networkOperatorName,
//                                _nrConnectionState
//                            )
//                        }
//                    } catch (e: Exception) {
//                        Timber.e("problem to obtain correct subscription ID when active network obtaining")
//                        connectivityManager.cellNetworkInfoCompat(
//                            telephonyManager.getCorrectDataTelephonyManager(subscriptionManager).networkOperatorName,
//                            _nrConnectionState
//                        )
//                    }
//                } else {
//                    connectivityManager.cellNetworkInfoCompat(
//                        telephonyManager.getCorrectDataTelephonyManager(subscriptionManager).networkOperatorName,
//                        _nrConnectionState
//                    )
//                }
//            }
//            return _activeNetwork
        }

    private fun isDualSim(): Boolean {
        return telephonyManager.getCorrectDataTelephonyManager(subscriptionManager).phoneCount > 1
    }

    private fun notifyListeners() {
        listeners.synchronizedForEach {
            it.onCellInfoChanged(_activeNetwork)
        }
    }

    override fun addListener(listener: CellInfoWatcher.CellInfoChangeListener) {
        listeners.add(listener)
        if (listeners.size == 1) {
            registerCallbacks()
        } else {
            listener.onCellInfoChanged(activeNetwork)
        }
    }

    override fun removeListener(listener: CellInfoWatcher.CellInfoChangeListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            unregisterCallbacks()
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerCallbacks() {
        try {
            if (locationAccess.isAllowed && phoneStateAccess.isAllowed) {
                if (context.isCoarseLocationPermitted()) {
                    Timber.d("CellInfosChanged: is null? registration ${telephonyManager.getCorrectDataTelephonyManager(subscriptionManager).allCellInfo == null}")
                    infoListener.onCellInfoChanged(telephonyManager.getCorrectDataTelephonyManager(subscriptionManager).allCellInfo)
                    telephonyManager.getCorrectDataTelephonyManager(subscriptionManager).listen(infoListener, PhoneStateListener.LISTEN_CELL_INFO)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        val cellInfoCallback = object : TelephonyManager.CellInfoCallback() {

                            // this can return an empty list or error in case of null so we need to handle error situations
                            override fun onCellInfo(cellInfo: MutableList<CellInfo>) {
                                if (cellInfo.isNotEmpty()) {
                                    processCellInfos(cellInfo, "original callback")
                                }
                            }

                            override fun onError(errorCode: Int, detail: Throwable?) {
                                super.onError(errorCode, detail)
                                Timber.d("CellInfosChanged: is null? true errorCode: $errorCode detail: $detail")
                            }
                        }

                        if (timer != null) {
                            removeNextTimerUpdates()
                        }
                        timer = Timer()
                        timer?.scheduleAtFixedRate(
                            timerTask {
                                telephonyManager.getCorrectDataTelephonyManager(subscriptionManager)
                                    .requestCellInfoUpdate({ command -> Thread(command).start() }, cellInfoCallback)
                            },
                            TimeUnit.SECONDS.toMillis(REFRESH_CELL_INFO_INTERVAL_SECONDS),
                            TimeUnit.SECONDS.toMillis(REFRESH_CELL_INFO_INTERVAL_SECONDS)
                        )
                    }
                    callbacksRegistered = true
                }
            }
        } catch (ex: Exception) {
            Timber.w(ex, "Failed to register callback for update cell info")
        }
    }

    private fun unregisterCallbacks() {
        telephonyManager.getCorrectDataTelephonyManager(subscriptionManager).listen(infoListener, PhoneStateListener.LISTEN_NONE)
        removeNextTimerUpdates()
        callbacksRegistered = false
    }

    private fun removeNextTimerUpdates() {
        timer?.cancel()
        timer?.purge()
        timer = null
    }

    override fun onLocationAccessChanged(isAllowed: Boolean) {
        if (listeners.isNotEmpty() && isAllowed && !callbacksRegistered) {
            registerCallbacks()
        }
    }

    override fun onPhoneStateAccessChanged(isAllowed: Boolean) {
        if (listeners.isNotEmpty() && isAllowed && !callbacksRegistered) {
            registerCallbacks()
        }
    }

    companion object {
        private const val REFRESH_CELL_INFO_INTERVAL_SECONDS: Long = 5
    }
}