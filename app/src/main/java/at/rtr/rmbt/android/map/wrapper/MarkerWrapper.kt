package at.rtr.rmbt.android.map.wrapper

import android.content.Context
import androidx.annotation.DrawableRes
import at.rtr.rmbt.android.util.iconFromVector

interface MarkerWrapper {

    fun remove()

    fun setVectorIcon(context: Context, @DrawableRes iconResId: Int)
}

class GMSMarker(private val marker: com.google.android.gms.maps.model.Marker?) : MarkerWrapper {

    override fun remove() {
        marker?.remove()
    }

    override fun setVectorIcon(context: Context, iconResId: Int) {
        marker?.iconFromVector(context, iconResId)
    }
}

class EmptyMarker : MarkerWrapper {
    override fun remove() {
    }

    override fun setVectorIcon(context: Context, iconResId: Int) {
    }
}