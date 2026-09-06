package at.rtr.rmbt.android.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.R

/**
 * The location-access status shown on the settings screen. This describes the granted location
 * PERMISSION granularity only - it is deliberately independent of whether the device's location
 * service is currently on. Each carries a user-facing label and a colour (reusing the home-screen
 * location colours: red = none, yellow = approximate, green = precise).
 */
enum class LocationPermissionStatus(@StringRes val labelRes: Int, @ColorRes val colorRes: Int) {
    DENIED(R.string.location_status_denied, R.color.location_button_red),
    COARSE(R.string.location_status_coarse, R.color.location_button_yellow),
    FINE(R.string.location_status_fine, R.color.location_button_green),
    BACKGROUND(R.string.location_status_background, R.color.location_button_green)
}

/**
 * Computes the current [LocationPermissionStatus] from the granted permissions:
 *  - no location permission at all -> [LocationPermissionStatus.DENIED]
 *  - otherwise the granularity actually granted: BACKGROUND (background allowed) > FINE (precise,
 *    foreground only) > COARSE (approximate).
 */
fun locationPermissionStatus(context: Context): LocationPermissionStatus {
    val hasFine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    // Below Android Q there is no separate background permission - foreground location already
    // allows background access - so treat it as granted there.
    val hasBackground = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    return when {
        !hasFine && !hasCoarse -> LocationPermissionStatus.DENIED
        hasFine && hasBackground -> LocationPermissionStatus.BACKGROUND
        hasFine -> LocationPermissionStatus.FINE
        else -> LocationPermissionStatus.COARSE
    }
}
