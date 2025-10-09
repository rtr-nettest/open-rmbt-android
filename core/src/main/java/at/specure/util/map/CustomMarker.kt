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

class CustomMarker(private val context: Context) {

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