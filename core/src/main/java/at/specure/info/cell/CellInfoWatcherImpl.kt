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
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoTdscdma
import android.telephony.CellInfoWcdma
import android.telephony.PhoneStateListener
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import at.specure.info.network.MobileNetworkType
import at.specure.util.permission.LocationAccess
import at.specure.util.permission.PhoneStateAccess
import at.specure.util.synchronizedForEach
import timber.log.Timber
import java.util.Collections
import java.util.UUID

private const val INVALID_SUBSCRIPTION_ID = -1
private val DUAL_SIM_METHOD_API = "api_" + Build.VERSION.SDK_INT

/**
 * Default implementation of [CellInfoWatcher] that is using to track Cellular network information
 */
class CellInfoWatcherImpl(
    private val telephonyManager: TelephonyManager,
    private val subscriptionManager: SubscriptionManager,
    private val locationAccess: LocationAccess,
    private val phoneStateAccess: PhoneStateAccess,
    private val connectivityManager: ConnectivityManager
) : CellInfoWatcher,
    LocationAccess.LocationAccessChangeListener,
    PhoneStateAccess.PhoneStateAccessChangeListener {

    private val listeners = Collections.synchronizedSet(mutableSetOf<CellInfoWatcher.CellInfoChangeListener>())

    private var _activeNetwork: CellNetworkInfo? = null
    private var callbacksRegistered = false
    private var _cellInfo: CellInfo? = null
    private var _allCellInfo = mutableListOf<CellNetworkInfo>()

    override val cellInfo: CellInfo?
        get() = _cellInfo

    override val allCellInfo: List<CellNetworkInfo>
        get() = _allCellInfo

    init {
        locationAccess.addListener(this)
        phoneStateAccess.addListener(this)
    }

    private val infoListener = object : PhoneStateListener() {

        @SuppressLint("MissingPermission")
        override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>?) {
            cellInfo ?: return

            val dataSimSubscriptionId = subscriptionManager.getCurrentDataSubscriptionId()
            var dualSimDecisionLog = ""
            var dualSimDecision = ""

            _activeNetwork = null
            if (dataSimSubscriptionId != INVALID_SUBSCRIPTION_ID) {
                val registeredInfoList = cellInfo.filter { it.isRegistered }

                val subscriptions = subscriptionManager.activeSubscriptionInfoList
                subscriptions?.forEachIndexed { index, it ->
                    // TODO this is not proved solution, need to find another way to connect CellInfo and SubscriptionInfo
                    if (dataSimSubscriptionId == it.subscriptionId && (registeredInfoList.size > index || registeredInfoList.size == 1)) {

                        val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            val manager = telephonyManager.createForSubscriptionId(dataSimSubscriptionId)
                            manager.dataNetworkType
                        } else {
                            // Todo: problem if operators are the same for both SIM cards (e.g. roaming network), but solving problems with different Networks (if user has no restriction on the usage of the network type for data or voice sim then it should use the same)
                            val networkTypeCheck =
                                connectivityManager.cellNetworkInfoCompat(telephonyManager.networkOperatorName)?.networkType
                                    ?: MobileNetworkType.UNKNOWN
                            if (networkTypeCheck == MobileNetworkType.UNKNOWN) {
                                telephonyManager.networkType
                            } else {
                                networkTypeCheck.ordinal
                            }
                        }

                        val mobileNetworkType = MobileNetworkType.fromValue(networkType)
                        val dataCellTechnology = CellTechnology.fromMobileNetworkType(mobileNetworkType)

                        // single sim
                        if (subscriptions.size == 1) {
                            _cellInfo = registeredInfoList[0]
                        } else {
                            // dual sim handling
                            it.displayName
                            dualSimDecision =
                                "$DUAL_SIM_METHOD_API\nDATA_SIM: slotIndex: ${it.simSlotIndex} carrierName: ${it.carrierName} displayName: ${it.displayName}\n"
                            // we need to check which of the registered cells uses same type of the network as data sim
                            var dualSimRegistered = registeredInfoList.filter { cellInfo ->
                                var sameNetworkType = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    when (cellInfo) {
                                        // 5G connections
                                        is CellInfoNr ->
                                            sameNetworkType = CellTechnology.CONNECTION_5G == dataCellTechnology
                                        // 3G connections
                                        is CellInfoTdscdma -> {
                                            sameNetworkType = CellTechnology.CONNECTION_3G == dataCellTechnology
                                        }
                                    }
                                }
                                if (sameNetworkType) {
                                    sameNetworkType
                                } else {
                                    when (cellInfo) {
                                        // 4G connections
                                        is CellInfoLte -> {
                                            CellTechnology.CONNECTION_4G == dataCellTechnology
                                        }
                                        // 3G connections
                                        is CellInfoWcdma -> {
                                            CellTechnology.CONNECTION_3G == dataCellTechnology
                                        }
                                        // 2G connections
                                        is CellInfoCdma -> {
                                            CellTechnology.CONNECTION_2G == dataCellTechnology
                                        }
                                        is CellInfoGsm -> {
                                            CellTechnology.CONNECTION_2G == dataCellTechnology
                                        }
                                        else -> false
                                    }
                                }
                            }
                            val countAfterNetworkTypeFilter = dualSimRegistered.size
                            if (registeredInfoList.size > dualSimRegistered.size) {
                                dualSimDecisionLog += "DSD - filtered according to same network type from ${registeredInfoList.size} to $countAfterNetworkTypeFilter\n"
                                dualSimDecision += "CELL_INFO: filtered according to: same network type from ${registeredInfoList.size} to $countAfterNetworkTypeFilter\n"
                            }
                            // if there is still more than one we can try filter it according to network operator name
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                if (dualSimRegistered.size > 1) {
                                    dualSimRegistered = dualSimRegistered.filter { cellInfo ->
                                        val networkOperator = it.carrierName.toString()
                                        when (cellInfo) {
                                            is CellInfoLte -> networkOperator.contentEquals(cellInfo.cellIdentity.operatorAlphaLong.toString()) || networkOperator.contentEquals(
                                                cellInfo.cellIdentity.operatorAlphaShort.toString()
                                            )
                                            is CellInfoWcdma -> networkOperator.contentEquals(cellInfo.cellIdentity.operatorAlphaLong.toString()) || networkOperator.contentEquals(
                                                cellInfo.cellIdentity.operatorAlphaShort.toString()
                                            )
                                            is CellInfoCdma -> networkOperator.contentEquals(cellInfo.cellIdentity.operatorAlphaLong.toString()) || networkOperator.contentEquals(
                                                cellInfo.cellIdentity.operatorAlphaShort.toString()
                                            )
                                            is CellInfoGsm -> networkOperator.contentEquals(cellInfo.cellIdentity.operatorAlphaLong.toString()) || networkOperator.contentEquals(
                                                cellInfo.cellIdentity.operatorAlphaShort.toString()
                                            )
                                            else -> false
                                        }
                                    }
                                }
                            }

                            if (countAfterNetworkTypeFilter > dualSimRegistered.size) {
                                dualSimDecisionLog += "DSD - filtered according to same network operator name as in data subscription info $countAfterNetworkTypeFilter to ${dualSimRegistered.size}\n"
                                dualSimDecision += "CELL_INFO: filtered according to: same network operator name as in data subscription info $countAfterNetworkTypeFilter to ${dualSimRegistered.size}\n"
                            }

                            if (dualSimRegistered.size == 1) {
                                _cellInfo = dualSimRegistered[0]
                                dualSimDecisionLog += "DSD - SUCCESS! \n Filtered this: \n\n$_cellInfo\n\n\n"
                                dualSimDecision += "CELL_INFO: SUCCESS! $_cellInfo"
                            } else {
                                dualSimDecisionLog += "DSD - FAILED! \n Unable to select one data cell info!"
                                dualSimDecision += "CELL_INFO: FAILED!"
                            }
                            Timber.v(dualSimDecisionLog)
                        }

                        _activeNetwork = CellNetworkInfo.from(
                            _cellInfo,
                            it,
                            MobileNetworkType.fromValue(networkType),
                            true,
                            connectivityManager.activeNetworkInfo?.isRoaming ?: false,
                            connectivityManager.activeNetworkInfo?.extraInfo,
                            if (subscriptions.size > 1) dualSimDecisionLog else null
                        )
                    }
                }
            }

            _allCellInfo.clear()
            cellInfo.forEach {
                val info = CellNetworkInfo.from(
                    it,
                    null,
                    _activeNetwork?.cellUUID == it.uuid(),
                    connectivityManager.activeNetworkInfo?.isRoaming ?: false,
                    connectivityManager.activeNetworkInfo?.extraInfo,
                    dualSimDecision
                )
                _allCellInfo.add(info)
                Timber.v("cell: ${info.networkType.displayName} ${info.mnc} ${info.mcc} ${info.cellUUID}")
            }

            notifyListeners()
        }
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

private fun SubscriptionManager.getCurrentDataSubscriptionId(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        SubscriptionManager.getDefaultDataSubscriptionId()
    } else {
        val clazz = this::class.java
        try {
            val method = clazz.getMethod("getDefaultDataSubId")
            method.invoke(this) as Int
        } catch (ex: Throwable) {
            Timber.e(ex)
            -1
        }
    }
}

private fun ConnectivityManager.cellNetworkInfoCompat(operatorName: String?): CellNetworkInfo? {
    val info = activeNetworkInfo
    Timber.i("type: ${info?.type}")
    Timber.i("typeName: ${info?.typeName}")
    Timber.i("subtype: ${info?.subtype}")
    Timber.i("subtypeName: ${info?.subtypeName}")
    Timber.i("detailed state ${info?.detailedState?.name}")

    return if (info == null || !info.isConnected || info.type != ConnectivityManager.TYPE_MOBILE) {
        null
    } else {
        CellNetworkInfo(
            providerName = operatorName ?: "",
            band = null,
            networkType = MobileNetworkType.fromValue(info.subtype),
            cellUUID = UUID.nameUUIDFromBytes(info.detailedState.name.toByteArray()).toString(),
            mnc = null,
            mcc = null,
            locationId = null,
            areaCode = null,
            scramblingCode = null,
            isActive = true,
            isRegistered = true,
            isRoaming = info.isRoaming,
            apn = info.extraInfo,
            signalStrength = null,
            dualSimDetectionMethod = null
        )
    }
}