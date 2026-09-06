package at.rtr.rmbt.android.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.config.AppConfig

/**
 * Whether to show the background-location permission info screen and request the permission.
 *
 * Returns true only when ALL of the following hold:
 *  - the feature is enabled ([AppConfig.shouldRequestBackgroundLocation]),
 *  - the OS has a separate background-location permission (Android Q+),
 *  - the foreground (fine) location permission is already granted - a precondition for being able to
 *    grant background location at all,
 *  - the background-location permission is NOT currently granted, and
 *  - the user has not previously declined it inside the app.
 *
 * Self-heals: whenever the permission is observed as granted, any earlier in-app decline is cleared.
 * That way, if the permission is later lost (revoked by the user or the OS), the user is offered it
 * again - we only stay silent when the user themselves declined it in-app and it is still not granted.
 */
fun shouldAskForBackgroundPermission(config: AppConfig, context: Context): Boolean {
    if (!config.shouldRequestBackgroundLocation) return false
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false

    val fineGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!fineGranted) return false

    val backgroundGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (backgroundGranted) {
        if (config.backgroundLocationPermissionDeclinedInApp) {
            config.backgroundLocationPermissionDeclinedInApp = false
        }
        return false
    }

    return !config.backgroundLocationPermissionDeclinedInApp
}

/**
 * Records the outcome of an in-app background-location permission request: a decline is remembered
 * (so the user is not nagged on the next measurement), while a grant clears the remembered decline
 * (so a later loss of the permission re-enables the offer).
 */
fun recordBackgroundPermissionResult(config: AppConfig, granted: Boolean) {
    config.backgroundLocationPermissionDeclinedInApp = !granted
}
