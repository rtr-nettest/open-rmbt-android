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

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import at.specure.info.NetworkCapability
import at.specure.info.TransportType
import at.specure.util.id
import at.specure.util.synchronizedForEach
import timber.log.Timber
import java.util.Collections

/**
 * Basic implementation of [ConnectivityWatcher] to track connectivity changes using [android.net.ConnectivityManager]
 */
class ConnectivityWatcherImpl(private val connectivityManager: ConnectivityManager) : ConnectivityWatcher {

    private val listeners = Collections.synchronizedSet(mutableSetOf<ConnectivityWatcher.ConnectivityChangeListener>())
    private val _connectivityStateLiveData = MutableLiveData<ConnectivityStateBundle>()

    private var availableNetworkId: Int? = null
    private var _activeNetwork: ConnectivityInfo? = null

    override val activeNetwork: ConnectivityInfo?
        get() = _activeNetwork

    override val connectivityStateLiveData: LiveData<ConnectivityStateBundle>
        get() = _connectivityStateLiveData

    override val network: Network?
        get() = connectivityManager.activeNetwork

    private val callback = object : ConnectivityManager.NetworkCallback() {

        override fun onCapabilitiesChanged(network: Network?, networkCapabilities: NetworkCapabilities?) {
            Timber.d("onCapabilitiesChanged ${network?.id()}")
            postConnectivityState(ConnectivityState.ON_CAPABILITIES_CHANGED, network)
            if (network != null && network.id() == availableNetworkId && networkCapabilities != null) {
                _activeNetwork = ConnectivityInfo(
                    netId = network.id(),
                    transportType = TransportType.fromNetworkCapability(networkCapabilities),
                    capabilities = NetworkCapability.fromNetworkCapability(networkCapabilities),
                    linkDownstreamBandwidthKbps = networkCapabilities.linkDownstreamBandwidthKbps,
                    linkUpstreamBandwidthKbps = networkCapabilities.linkUpstreamBandwidthKbps
                )
                notifyListeners()
            }
        }

        override fun onLost(network: Network?) {
            Timber.d("onLost ${network?.id()}")
            postConnectivityState(ConnectivityState.ON_LOST, network)
            if (availableNetworkId == network?.id()) {
                availableNetworkId = null
                _activeNetwork = null
                notifyListeners()
            }
        }

        override fun onLinkPropertiesChanged(network: Network?, linkProperties: LinkProperties?) {
            postConnectivityState(ConnectivityState.ON_LINK_PROPERTIES_CHANGED, network)
            linkProperties?.linkAddresses
            Timber.d("onLinkPropertiesChanged ${network?.id()}")
        }

        override fun onUnavailable() {
            postConnectivityState(ConnectivityState.ON_UNAVAILABLE, null)
            Timber.d("onUnavailable")
            availableNetworkId = null
            _activeNetwork = null
            notifyListeners()
        }

        override fun onLosing(network: Network?, maxMsToLive: Int) {
            postConnectivityState(ConnectivityState.ON_LOSING, network)
            Timber.d("onLosing ${network?.id()}")
        }

        @Suppress("DEPRECATION")
        override fun onAvailable(network: Network?) {
            postConnectivityState(ConnectivityState.ON_AVAILABLE, network)
            availableNetworkId = network?.id()

            // on android versions prior to 8 onCapabilityChanged callback does not called after registering a listener
            // This code forces to get information about network from deprecated methods
            if (network != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                connectivityManager.activeNetworkInfo?.let {
                    val transportType = it.transportType()
                    if (transportType != null) {
                        _activeNetwork = ConnectivityInfo(
                            netId = network.id(),
                            transportType = transportType,
                            capabilities = listOf(),
                            linkDownstreamBandwidthKbps = 0,
                            linkUpstreamBandwidthKbps = 0
                        )
                        notifyListeners()
                    }
                }
            }
            Timber.d("onAvailable ${network?.id()}")
        }
    }

    private fun postConnectivityState(state: ConnectivityState, network: Network?) {
        _connectivityStateLiveData.postValue(ConnectivityStateBundle(state, System.nanoTime(), network?.id().toString()))
    }

    override fun addListener(listener: ConnectivityWatcher.ConnectivityChangeListener) {
        if (connectivityManager.activeNetwork == null) {
            _activeNetwork = null
        }
        listeners.add(listener)
        listener.onConnectivityChanged(_activeNetwork, connectivityManager.activeNetwork)
        if (listeners.size == 1) {
            registerCallbacks()
        }
    }

    override fun removeListener(listener: ConnectivityWatcher.ConnectivityChangeListener) {
        listeners.remove(listener)
        if (listeners.isEmpty()) {
            unregisterCallbacks()
        }
    }

    private fun notifyListeners() {
        listeners.synchronizedForEach { it.onConnectivityChanged(_activeNetwork, connectivityManager.activeNetwork) }
    }

    private fun registerCallbacks() {
        val request = NetworkRequest.Builder()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(callback)
        } else {
            connectivityManager.registerNetworkCallback(request.build(), callback)
        }
    }

    private fun unregisterCallbacks() {
        connectivityManager.unregisterNetworkCallback(callback)
    }

    @Suppress("DEPRECATION")
    private fun NetworkInfo.transportType(): TransportType? {
        return when (type) {
            ConnectivityManager.TYPE_MOBILE -> TransportType.CELLULAR
            ConnectivityManager.TYPE_WIFI -> TransportType.WIFI
            ConnectivityManager.TYPE_BLUETOOTH -> TransportType.BLUETOOTH
            ConnectivityManager.TYPE_ETHERNET -> TransportType.ETHERNET
            ConnectivityManager.TYPE_VPN -> TransportType.VPN
            else -> null
        }
    }
}