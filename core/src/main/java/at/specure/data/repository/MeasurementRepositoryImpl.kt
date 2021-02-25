package at.specure.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import at.rmbt.client.control.getCorrectDataTelephonyManager
import at.specure.config.Config
import at.specure.info.TransportType
import at.specure.info.cell.CellInfoWatcher
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.cell.mccCompat
import at.specure.info.cell.mncCompat
import at.specure.info.network.ActiveNetworkWatcher
import at.specure.info.wifi.WifiInfoWatcher
import at.specure.util.hasPermission
import at.specure.util.isDualSim
import at.specure.util.isReadPhoneStatePermitted
import at.specure.util.permission.PermissionsWatcher
import java.text.DecimalFormat
import javax.inject.Inject

private const val ACCEPT_WIFI_RSSI_MIN = -113
private const val INVALID_SUBSCRIPTION_ID = -1

class MeasurementRepositoryImpl @Inject constructor(
    private val context: Context,
    private val telephonyManager: TelephonyManager,
    private val subscriptionManager: SubscriptionManager,
    private val activeNetworkWatcher: ActiveNetworkWatcher,
    private val cellInfoWatcher: CellInfoWatcher,
    private val repository: TestDataRepository,
    private val wifiInfoWatcher: WifiInfoWatcher,
    private val config: Config,
    private val permissionsWatcher: PermissionsWatcher
) : MeasurementRepository {

    override fun savePermissionsStatus(uuid: String) {
        val permissions = permissionsWatcher.allPermissions
        permissions.forEach { permission ->
            val permissionGranted = context.hasPermission(permission)
            repository.savePermissionStatus(uuid, permission, permissionGranted)
        }
    }

    override fun saveCapabilities(uuid: String) {
        repository.saveCapabilities(
            uuid,
            config.capabilitiesRmbtHttp,
            config.capabilitiesQosSupportsInfo,
            config.capabilitiesClassificationCount
        )
    }

    override fun saveWlanInfo(uuid: String) {
        val wifiInfo = wifiInfoWatcher.activeWifiInfo
        if (wifiInfo?.ssid != null && wifiInfo.bssid != null && wifiInfo.rssi >= ACCEPT_WIFI_RSSI_MIN) {
            repository.saveWlanInfo(uuid, wifiInfo)
        }
    }

    @SuppressLint("MissingPermission")
    override fun saveTelephonyInfo(uuid: String) {
        // TODO: all these fields should be moved to some watcher to be updated regularly at one place (maybe activeNetworkWatcherImpl)
        val type = activeNetworkWatcher.currentNetworkInfo?.type
        val isDualSim = context.isDualSim(telephonyManager, subscriptionManager)
        val isDualByMobile = type == TransportType.CELLULAR && isDualSim

        val localTelephonyManager = telephonyManager.getCorrectDataTelephonyManager(subscriptionManager)

        var operatorName: String? = null
        var networkSimOperator: String? = null
        val simCount: Int
        var simCountry: String? = null
        var simOperatorName: String? = null

        val networkInfo = cellInfoWatcher.activeNetwork

        if (context.isReadPhoneStatePermitted() && isDualByMobile) {
            val subscription = subscriptionManager.activeSubscriptionInfoList.firstOrNull()
            simCount = if (subscription != null) subscriptionManager.activeSubscriptionInfoCount else 2

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val defaultDataSubscriptionId = SubscriptionManager.getDefaultDataSubscriptionId()
                if (defaultDataSubscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    // TODO: this execution branch needs to be tested
                    operatorName = activeNetworkWatcher.currentNetworkInfo?.name
                    networkSimOperator = localTelephonyManager.simOperator.fixOperatorName()

                    subscriptionManager.activeSubscriptionInfoList.forEach {
                        val checkNetworkSimOperator = when {
                            it.mccCompat() == null -> null
                            it.mncCompat() == null -> null
                            else -> "${it.mccCompat()}-${DecimalFormat("00").format(it.mncCompat())}"
                        }
                        if (checkNetworkSimOperator.equals(networkSimOperator)) {
                            simCountry = it.countryIso
                            simOperatorName = it.displayName?.toString()
                        }
                    }
                } else {
                    val subscriptionInfo = subscriptionManager.getActiveSubscriptionInfo(defaultDataSubscriptionId)
                    subscriptionInfo?.let {
                        operatorName = subscriptionInfo.carrierName.toString()
                        networkSimOperator = when {
                            subscriptionInfo.mccCompat() == null -> null
                            subscriptionInfo.mncCompat() == null -> null
                            else -> "${subscriptionInfo.mccCompat()}-${DecimalFormat("00").format(subscriptionInfo.mncCompat())}"
                        }
                        simCountry = subscriptionInfo.countryIso
                        simOperatorName = subscriptionInfo.displayName?.toString()
                    }
                }
            } else {
                operatorName = activeNetworkWatcher.currentNetworkInfo?.name
                networkSimOperator = localTelephonyManager.simOperator.fixOperatorName()

                subscriptionManager.activeSubscriptionInfoList.forEach {
                    val checkNetworkSimOperator = when {
                        it.mccCompat() == null -> null
                        it.mncCompat() == null -> null
                        else -> "${it.mccCompat()}-${DecimalFormat("00").format(it.mncCompat())}"
                    }
                    if (checkNetworkSimOperator.equals(networkSimOperator)) {
                        simCountry = it.countryIso
                        simOperatorName = it.displayName.toString()
                    }
                }
            }
        } else {
            // dual sim but we have no granted READ_PHONE_STATE_PERMISSIONS
            simCount = if (isDualByMobile) {
                2
            } else {
                // single sim
                1
            }
            operatorName = extractNetworkOperatorNameForSingleSim(localTelephonyManager, networkInfo)
            networkSimOperator = localTelephonyManager.simOperator.fixOperatorName()
            simCountry = localTelephonyManager.simCountryIso.fixOperatorName()
            simOperatorName = try { // hack for Motorola Defy (#594)
                localTelephonyManager.simOperatorName
            } catch (ex: SecurityException) {
                ex.printStackTrace()
                "s.exception"
            }
        }
        val networkCountry: String? = localTelephonyManager.networkCountryIso

        val phoneType = localTelephonyManager.phoneType.toString()
        val dataState = try {
            localTelephonyManager.dataState.toString()
        } catch (ex: SecurityException) {
            ex.printStackTrace()
            "s.exception"
        }

        repository.saveTelephonyInfo(
            uuid,
            networkInfo,
            operatorName,
            networkSimOperator,
            networkCountry,
            simCountry,
            simOperatorName,
            phoneType,
            dataState,
            simCount
        )
    }

    private fun extractNetworkOperatorNameForSingleSim(
        telephonyManager: TelephonyManager,
        networkInfo: CellNetworkInfo?
    ): String? {
        var operatorName: String? = null
        // for CDMA getting network operator name is not reliable (according to api documentation)
        operatorName = if (telephonyManager.phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
            telephonyManager.networkOperatorName
        } else {
            networkInfo?.providerName // from cell info
        }
        return operatorName
    }

    private fun String?.fixOperatorName(): String? {
        return if (this == null) {
            null
        } else if (length >= 5 && !contains("-")) {
            "${substring(0, 3)}-${substring(3)}"
        } else {
            this
        }
    }
}