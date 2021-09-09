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

package at.rtr.rmbt.android.util

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.TextView
import androidx.core.content.ContextCompat
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.R
import at.specure.info.TransportType
import at.specure.info.cell.CellNetworkInfo
import at.specure.info.network.NetworkInfo
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

fun HandledException.getStringTitle(context: Context): String {
    return getTitle(context) ?: context.getString(R.string.dialog_title_error)
}

fun Calendar.format(pattern: String): String {
    val simpleDateFormat = SimpleDateFormat(pattern, Locale.US)
    return simpleDateFormat.format(this.time)
}

fun MarkerOptions.iconFromVector(context: Context, vectorResId: Int): MarkerOptions {
    return this.icon(ContextCompat.getDrawable(context, vectorResId)?.run {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        BitmapDescriptorFactory.fromBitmap(bitmap)
    })
}

fun Marker.iconFromVector(context: Context, vectorResId: Int) {
    return setIcon(ContextCompat.getDrawable(context, vectorResId)?.run {
        setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        draw(Canvas(bitmap))
        BitmapDescriptorFactory.fromBitmap(bitmap)
    })
}

fun LatLng.toMapBoxLatLng(): com.mapbox.mapboxsdk.geometry.LatLng {
    return com.mapbox.mapboxsdk.geometry.LatLng(this.latitude, this.longitude)
}

fun Long.timeString(): String {
    val hours = TimeUnit.MILLISECONDS.toHours(this)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this - TimeUnit.HOURS.toMillis(hours))
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this - TimeUnit.MINUTES.toMillis(minutes) - TimeUnit.HOURS.toMillis(hours))

    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

fun TextView.setTechnologyIcon(info: NetworkInfo) {
    val padding = 12
    when (info.type) {
        TransportType.WIFI -> {
            text = context.getString(R.string.label_network_info_wifi)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_wifi, 0, 0, 0)
            compoundDrawablePadding = padding
        }
        TransportType.CELLULAR -> {
            val cellInfo = info as? CellNetworkInfo
            text = cellInfo?.networkType?.displayName
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_signal_cellular, 0, 0, 0)
            compoundDrawablePadding = padding
        }
        TransportType.ETHERNET -> {
            text = context.getString(R.string.label_network_info_ethernet)
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_ethernet, 0, 0, 0)
            compoundDrawablePadding = padding
        }
        else -> {
        }
    }
}

fun Array<out String>.hasLocationPermissions(): Boolean {
    forEach {
        if (it == Manifest.permission.ACCESS_COARSE_LOCATION ||
            it == Manifest.permission.ACCESS_FINE_LOCATION ||
            it == Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) {
            return true
        }
    }
    return false
}

@ExperimentalCoroutinesApi
fun <T> Channel<T>.safeOffer(elem: T) {
    if (!isClosedForSend) {
        offer(elem)
    }
}