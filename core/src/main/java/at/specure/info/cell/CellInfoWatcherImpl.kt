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
import android.net.ConnectivityManager
import android.telephony.CellInfo
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import at.specure.info.network.NRConnectionState
import at.specure.util.permission.LocationAccess
import at.specure.util.permission.PhoneStateAccess
import at.specure.util.synchronizedForEach
import timber.log.Timber
import java.util.Collections

/**
 * Default implementation of [CellInfoWatcher] that is using to track Cellular network information
 */
class CellInfoWatcherImpl(
    private val telephonyManager: TelephonyManager,
    private val locationAccess: LocationAccess,
    private val phoneStateAccess: PhoneStateAccess,
    private val connectivityManager: ConnectivityManager,
    private val activeDataCellInfoExtractor: ActiveDataCellInfoExtractor
) : CellInfoWatcher,
    LocationAccess.LocationAccessChangeListener,
    PhoneStateAccess.PhoneStateAccessChangeListener {

    private val listeners = Collections.synchronizedSet(mutableSetOf<CellInfoWatcher.CellInfoChangeListener>())

    private var _activeNetwork: CellNetworkInfo? = null
    private var callbacksRegistered = false
    private var _cellInfo: CellInfo? = null
    private var _allCellInfo = Collections.synchronizedList(mutableListOf<CellNetworkInfo>())
    private var _nrConnectionState = NRConnectionState.NOT_AVAILABLE

    override val cellInfo: CellInfo?
        get() = _cellInfo

    override val allCellInfo: List<CellNetworkInfo>
        get() = _allCellInfo

    override val nrConnectionState: NRConnectionState
        get() = _nrConnectionState

    init {
        locationAccess.addListener(this)
        phoneStateAccess.addListener(this)
    }

    private val infoListener = object : PhoneStateListener() {

        @SuppressLint("MissingPermission")
        override fun onCellInfoChanged(cellInfos: MutableList<CellInfo>?) {
            processCellInfos(cellInfos)
        }
    }

    private fun processCellInfos(cellInfos: MutableList<CellInfo>?) {
        cellInfos ?: return

        val activeDataCellInfo = activeDataCellInfoExtractor.extractActiveCellInfo(cellInfos)
        activeDataCellInfo.activeDataNetworkCellInfo?.let {
            _cellInfo = it
        }

        activeDataCellInfo.activeDataNetwork?.let {
            _activeNetwork = it
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
                activeDataCellInfo.dualSimDecision
            )
            _allCellInfo.add(info)
            Timber.v("cell: ${info.networkType.displayName} ${info.mnc} ${info.mcc} ${info.cellUUID}")
        }

        notifyListeners()
    }

    @SuppressLint("MissingPermission")
    override fun forceUpdate() {
        try {
            val cells = telephonyManager.allCellInfo
            if (!cells.isNullOrEmpty()) {
                infoListener.onCellInfoChanged(cells)
            }
        } catch (ex: Exception) {
            Timber.w(ex, "Failed to update cell info")
        }
    }

    override val activeNetwork: CellNetworkInfo?
        get() {
            if (_activeNetwork == null) {
                return connectivityManager.cellNetworkInfoCompat(telephonyManager.networkOperatorName)
            }
            return _activeNetwork
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
        if (locationAccess.isAllowed && phoneStateAccess.isAllowed) {
            infoListener.onCellInfoChanged(telephonyManager.allCellInfo)
            telephonyManager.listen(infoListener, PhoneStateListener.LISTEN_CELL_INFO)
            callbacksRegistered = true
        }
    }

    private fun unregisterCallbacks() {
        telephonyManager.listen(infoListener, PhoneStateListener.LISTEN_NONE)
        callbacksRegistered = false
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
}