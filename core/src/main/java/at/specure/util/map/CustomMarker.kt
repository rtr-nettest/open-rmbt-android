package at.specure.util.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.DrawableCompat
import at.specure.info.network.MobileNetworkType
import javax.inject.Singleton

@Singleton
class CustomMarker(private val context: Context) {

    val markers: Map<MobileNetworkType, Bitmap>

    init {
        markers = mapOf(
            MobileNetworkType.UNKNOWN to createCustomShapeBitmap(MobileNetworkType.UNKNOWN),
            MobileNetworkType.NR_AVAILABLE to createCustomShapeBitmap(MobileNetworkType.NR_AVAILABLE),
            MobileNetworkType.NR_NSA to createCustomShapeBitmap(MobileNetworkType.NR_NSA),
            MobileNetworkType.NR_SA to createCustomShapeBitmap(MobileNetworkType.NR_SA),

            MobileNetworkType.LTE to createCustomShapeBitmap(MobileNetworkType.LTE),
            MobileNetworkType.LTE_CA to createCustomShapeBitmap(MobileNetworkType.LTE_CA),
            MobileNetworkType.IWLAN to createCustomShapeBitmap(MobileNetworkType.IWLAN),

            MobileNetworkType.GPRS to createCustomShapeBitmap(MobileNetworkType.GPRS),
            MobileNetworkType.EDGE to createCustomShapeBitmap(MobileNetworkType.EDGE),
            MobileNetworkType.CDMA to createCustomShapeBitmap(MobileNetworkType.CDMA),
            MobileNetworkType._1xRTT to createCustomShapeBitmap(MobileNetworkType._1xRTT),
            MobileNetworkType.IDEN to createCustomShapeBitmap(MobileNetworkType.IDEN),
            MobileNetworkType.GSM to createCustomShapeBitmap(MobileNetworkType.GSM),
            // 3G family
            MobileNetworkType.UMTS to createCustomShapeBitmap(MobileNetworkType.UMTS),
            MobileNetworkType.EVDO_0 to createCustomShapeBitmap(MobileNetworkType.EVDO_0),
            MobileNetworkType.EVDO_A to createCustomShapeBitmap(MobileNetworkType.EVDO_A),
            MobileNetworkType.EVDO_B to createCustomShapeBitmap(MobileNetworkType.EVDO_B),
            MobileNetworkType.HSDPA to createCustomShapeBitmap(MobileNetworkType.HSDPA),
            MobileNetworkType.HSUPA to createCustomShapeBitmap(MobileNetworkType.HSUPA),
            MobileNetworkType.HSPA to createCustomShapeBitmap(MobileNetworkType.HSPA),
            MobileNetworkType.EHRPD to createCustomShapeBitmap(MobileNetworkType.EHRPD),
            MobileNetworkType.TD_SCDMA to createCustomShapeBitmap(MobileNetworkType.TD_SCDMA),
            MobileNetworkType.HSPAP to createCustomShapeBitmap(MobileNetworkType.HSPAP)
        )
    }

    fun getMarker(mobileNetworkType: MobileNetworkType): Bitmap {
        return markers[mobileNetworkType] ?: createCustomShapeBitmap(mobileNetworkType)
    }


    fun createTintedVectorMarker(
        vectorResId: Int,
        technology: MobileNetworkType,
        size: Int = 48
    ): Bitmap {
        val drawable = ContextCompat.getDrawable(context, vectorResId)?.mutate()
            ?: throw IllegalArgumentException("Drawable not found")

        // Tint the drawable
        DrawableCompat.setTint(drawable, technology.getMarkerColorInt())

        // Convert to bitmap
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)

        return bitmap
    }

    fun createCustomShapeBitmap(mobileTechnology: MobileNetworkType, size: Int = 48): Bitmap {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            color = mobileTechnology.getMarkerColorInt()
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val radius = (size - 8) / 2f
        canvas.drawCircle(size / 2f, size / 2f, radius, paint)

        // Add white border
        paint.apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }

        val borderRadius = (size - 8) / 2f
        canvas.drawCircle(size / 2f, size / 2f, borderRadius, paint)

        return bitmap
    }
}