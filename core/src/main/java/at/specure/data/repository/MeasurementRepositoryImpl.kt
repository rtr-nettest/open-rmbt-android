package at.specure.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import at.specure.config.Config
import at.specure.info.TransportType
import at.specure.info.cell.CellInfoWatcher
import at.specure.info.cell.mccCompat
import at.specure.info.cell.mncCompat
import at.specure.info.network.ActiveNetworkWatcher
import at.specure.info.wifi.WifiInfoWatcher
import at.specure.util.hasPermission
import at.specure.util.isReadPhoneStatePermitted
import at.specure.util.permission.PermissionsWatcher
import java.text.DecimalFormat
import javax.inject.Inject

private const val ACCEPT_WIFI_RSSI_MIN = -113

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
        val type = activeNetworkWatcher.currentNetworkInfo?.type
        val isDualSim = telephonyManager.phoneCount > 1
        val isDualByMobile = type == TransportType.CELLULAR && isDualSim

        var operatorName: String? = null
        var networkSimOperator: String? = null
        var networkCountry: String? = null
        val simCount: Int

        if (context.isReadPhoneStatePermitted() && isDualByMobile) {
            val subscription = subscriptionManager.activeSubscriptionInfoList.firstOrNull()
            simCount = if (subscription != null) subscriptionManager.activeSubscriptionInfoCount else 2
            subscription?.let {
                operatorName = subscription.carrierName.toString()
                networkSimOperator = when {
                    subscription.mccCompat() == null -> null
                    subscription.mncCompat() == null -> null
                    else -> "${subscription.mccCompat()}-${DecimalFormat("00").format(subscription.mncCompat())}"
                }
                networkCountry = subscription.countryIso
            }
        } else {
            simCount = 1
            operatorName = telephonyManager.networkOperatorName
            networkSimOperator = telephonyManager.networkOperator.fixOperatorName()
            networkCountry = telephonyManager.networkCountryIso
        }

        val networkInfo = cellInfoWatcher.activeNetwork
        val simCountry = telephonyManager.simCountryIso.fixOperatorName()
        val simOperatorName = try { // hack for Motorola Defy (#594)
            telephonyManager.simOperatorName
        } catch (ex: SecurityException) {
            ex.printStackTrace()
            "s.exception"
        }
        val phoneType = telephonyManager.phoneType.toString()
        val dataState = try {
            telephonyManager.dataState.toString()
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