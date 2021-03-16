package at.rtr.rmbt.android.ui

import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.util.formatAccuracy
import at.rtr.rmbt.android.util.formatAgeString
import at.rtr.rmbt.android.util.formatAltitude
import at.rtr.rmbt.android.util.formatCoordinate
import at.rtr.rmbt.android.util.formatSpeed
import at.specure.location.LocationInfo

/**
 * A binding adapter that is used for show location position
 */
@BindingAdapter("locationPosition")
fun AppCompatTextView.setLocationPosition(locationInfo: LocationInfo?) {

    locationInfo?.let {
        val formattedLatitude = locationInfo.formatCoordinate(locationInfo.latitude)
        val formattedLongitude = locationInfo.formatCoordinate(locationInfo.longitude)
        val latitudeText = if (locationInfo.latitudeDirection == LocationInfo.LocationCardinalDirections.NORTH)
            context.getString(R.string.location_location_direction_n, formattedLatitude)
        else
            context.getString(R.string.location_location_direction_s, formattedLatitude)

        val longitudeText = if (locationInfo.longitudeDirection == LocationInfo.LocationCardinalDirections.EAST)
            context.getString(R.string.location_location_direction_e, formattedLongitude)
        else
            context.getString(R.string.location_location_direction_w, formattedLongitude)

        text = context.getString(R.string.location_dialog_position, latitudeText, longitudeText)
    }
}

@BindingAdapter("locationAccuracy")
fun AppCompatTextView.setLocationAccuracy(locationInfo: LocationInfo?) {
    locationInfo?.let {
        val formatAccuracy = locationInfo.formatAccuracy()
        text = if (formatAccuracy == null) {
            null
        } else {
            context.getString(R.string.location_dialog_accuracy, formatAccuracy)
        }
    }
}

@BindingAdapter("locationAltitude")
fun AppCompatTextView.setLocationAltitude(locationInfo: LocationInfo?) {
    locationInfo?.let {
        val formatAltitude = locationInfo.formatAltitude()
        text = if (formatAltitude == null) {
            null
        } else {
            context.getString(R.string.location_dialog_accuracy, formatAltitude)
        }
    }
}

@BindingAdapter("locationSpeed")
fun AppCompatTextView.setLocationSpeed(locationInfo: LocationInfo?) {
    locationInfo?.let {
        val formatSpeed = locationInfo.formatSpeed()
        text = if (formatSpeed == null) {
            null
        } else {
            context.getString(R.string.location_dialog_speed, formatSpeed)
        }
    }
}

@BindingAdapter("locationAge")
fun AppCompatTextView.setLocationAge(locationInfo: LocationInfo?) {
    locationInfo?.let {
        val formatAge = locationInfo.formatAgeString()
        text = context.getString(R.string.location_dialog_age, formatAge)
    }
}

/**
 * A binding adapter that is used for show location position
 */
@BindingAdapter("locationProvider")
fun AppCompatTextView.setLocationProvider(locationProvider: String?) {
    text = locationProvider ?: context.getString(R.string.location_dialog_not_available)
}