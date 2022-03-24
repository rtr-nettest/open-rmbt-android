package at.specure.util

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import timber.log.Timber

/**
 * @return true if location services are enabled, false otherwise
 */
fun Context.isLocationServiceEnabled(): Boolean {
    lateinit var lm: LocationManager
    var gpsEnabled = false
    var networkEnabled = false

    try {
        lm = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    } catch (e: Exception) {
        Timber.e(e)
    }
    return gpsEnabled || networkEnabled
}

/**
 * @return true if [Manifest.permission.ACCESS_FINE_LOCATION] permissions
 * are granted
 */
fun Context.isFineLocationPermitted() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * @return true if [Manifest.permission.ACCESS_FINE_LOCATION] or [Manifest.permission.ACCESS_COARSE_LOCATION] permissions
 * are granted
 */
fun Context.isCoarseLocationPermitted() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * @return true if [Manifest.permission.READ_PHONE_STATE] permission is granted
 */
fun Context.isReadPhoneStatePermitted() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED

/**
 * Copies text into the system clipboard
 */
fun Context.copyToClipboard(text: String?) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(null, text)
    if (clip != null) {
        clipboard.setPrimaryClip(clip)
    }
}

/**
 * Opens Current application system settings
 */
fun Context.openAppSettings() {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    startActivity(intent)
}

/**
 * Checks that runtime permission is allowed by user
 */
fun Context.hasPermission(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * Shows toast message with [Toast.LENGTH_SHORT] duration
 */
fun Context.toast(stringRes: Int) = Toast.makeText(this, stringRes, Toast.LENGTH_SHORT).show()

/**
 * Checks if current device is running in dual sim mode or single sim mode
 */
fun Context.isDualSim(telephonyManager: TelephonyManager, subscriptionManager: SubscriptionManager): Boolean {
    return if (PermissionChecker.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PermissionChecker.PERMISSION_GRANTED) {
        subscriptionManager.activeSubscriptionInfoCount > 1
    } else {
        telephonyManager.phoneCount > 1
    }
}