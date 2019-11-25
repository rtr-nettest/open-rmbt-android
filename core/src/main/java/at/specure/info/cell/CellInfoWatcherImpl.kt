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

    init {
        locationAccess.addListener(this)
        phoneStateAccess.addListener(this)
    }

    private val infoListener = object : PhoneStateListener() {

        @SuppressLint("MissingPermission")
        override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>?) {
            cellInfo ?: return

            val dataSimSubscriptionId = subscriptionManager.getCurrentDataSubscriptionId()

            _activeNetwork = null
            if (dataSimSubscriptionId != INVALID_SUBSCRIPTION_ID) {
                val registeredInfoList = cellInfo.filter { it.isRegistered }

                val subscriptions = subscriptionManager.activeSubscriptionInfoList
                subscriptions?.forEachIndexed { index, it ->
                    // TODO this is not proved solution, need to find another way to connect CellInfo and SubscriptionInfo
                    if (dataSimSubscriptionId == it.subscriptionId && registeredInfoList.size > index) {

                        val networkType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            val manager = telephonyManager.createForSubscriptionId(dataSimSubscriptionId)
                            manager.dataNetworkType
                        } else {
                            telephonyManager.networkType
                        }

                        _activeNetwork = CellNetworkInfo.from(registeredInfoList[index], it, MobileNetworkType.fromValue(networkType))
                    }
                    Timber.d("carrier name: ${it.carrierName}")
                    Timber.d("display name: ${it.displayName}")
                    Timber.d("number: ${it.number}")
                    Timber.d("sim slot: ${it.simSlotIndex}")
                    Timber.d("subscription id: ${it.subscriptionId}")
                    Timber.d("subscription iccId: ${it.iccId}")
                    Timber.d("------------------")
                }
            }

            notifyListeners()
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
            registerCallbacks() // TODO check this
        }
    }

    override fun onPhoneStateAccessChanged(isAllowed: Boolean) {
        if (listeners.isNotEmpty() && isAllowed && !callbacksRegistered) {
            registerCallbacks() // TODO check this
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
            cellUUID = UUID.randomUUID().toString()
        )
    }
}