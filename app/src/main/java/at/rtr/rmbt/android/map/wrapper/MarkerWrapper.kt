package at.rtr.rmbt.android.map.wrapper

import android.content.Context
import androidx.annotation.DrawableRes
import at.rtr.rmbt.android.util.iconFromVector
import com.huawei.hms.maps.model.Marker

interface MarkerWrapper {

    fun remove()

    fun setVectorIcon(context: Context, @DrawableRes iconResId: Int)
}

class HMSMarker(private val marker: Marker) : MarkerWrapper {

    override fun remove() {
        marker.remove()
    }

    override fun setVectorIcon(context: Context, iconResId: Int) {
        marker.iconFromVector(context, iconResId)
    }
}

class GMSMarker(private val marker: com.google.android.gms.maps.model.Marker) : MarkerWrapper {

    override fun remove() {
        marker.remove()
    }

    override fun setVectorIcon(context: Context, iconResId: Int) {
        marker.iconFromVector(context, iconResId)
    }
}

class EmptyMarker : MarkerWrapper {
    override fun remove() {
    }

    override fun setVectorIcon(context: Context, iconResId: Int) {
    }
}