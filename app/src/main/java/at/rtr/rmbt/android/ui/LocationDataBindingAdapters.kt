package at.rtr.rmbt.android.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
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
 * A binding adapter that shows the latitude with its cardinal direction (e.g. "N 48° 11.543'").
 */
@BindingAdapter("locationLatitude")
fun AppCompatTextView.setLocationLatitude(locationInfo: LocationInfo?) {
    locationInfo?.let {
        val formatted = it.formatCoordinate(it.latitude)
        text = if (it.latitudeDirection == LocationInfo.LocationCardinalDirections.NORTH)
            context.getString(R.string.location_location_direction_n, formatted)
        else
            context.getString(R.string.location_location_direction_s, formatted)
    }
}

/**
 * A binding adapter that shows the longitude with its cardinal direction (e.g. "E 16° 17.639'").
 */
@BindingAdapter("locationLongitude")
fun AppCompatTextView.setLocationLongitude(locationInfo: LocationInfo?) {
    locationInfo?.let {
        val formatted = it.formatCoordinate(it.longitude)
        text = if (it.longitudeDirection == LocationInfo.LocationCardinalDirections.EAST)
            context.getString(R.string.location_location_direction_e, formatted)
        else
            context.getString(R.string.location_location_direction_w, formatted)
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

/**
 * Renders the per-constellation baseband C/N0 summary as one row per system (best first), each shown
 * as a "Signal (<System>)" label with a "<value> dB-Hz" value. The container is hidden when empty.
 */
@BindingAdapter("gnssSignals")
fun LinearLayout.setGnssSignals(locationInfo: LocationInfo?) {
    removeAllViews()
    val signals = locationInfo?.gnssSignals.orEmpty()
    visibility = if (signals.isEmpty()) View.GONE else View.VISIBLE
    val inflater = LayoutInflater.from(context)
    signals.forEach { signal ->
        val row = inflater.inflate(R.layout.item_location_signal, this, false)
        row.findViewById<AppCompatTextView>(R.id.labelSignal).text =
            context.getString(R.string.location_dialog_label_signal, signal.constellationName)
        row.findViewById<AppCompatTextView>(R.id.textSignal).text =
            context.getString(R.string.location_dialog_signal_value, signal.cn0DbHz)
        addView(row)
    }
}