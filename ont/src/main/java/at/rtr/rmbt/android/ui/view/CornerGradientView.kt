package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView

class CornerGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    val path = Path()

    private val paintFilled = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        color = Color.RED
        style = Paint.Style.FILL_AND_STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val topArcRadius = measuredWidth.toFloat()  * 0.75f

        val oval = RectF(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
        canvas.rotate(-30f)
        canvas.translate(0.3f * measuredWidth, 0.2f * measuredHeight)
        canvas.drawRoundRect(oval, 73f, 73f, paintFilled)
//        path.moveTo(0f, 0f)
//        path.rQuadTo(0.05f * measuredWidth, measuredHeight.toFloat() / 4, 0.2f * measuredWidth, 0.4f * measuredHeight)
////        path.arcTo(  0f, 0f,topArcRadius, topArcRadius,120f, 60f, true)
//        path.lineTo(0.8f * measuredWidth, 0.85f * measuredHeight )
//
//        path.rQuadTo(0f * measuredWidth.toFloat(), 0.05f * measuredHeight.toFloat(), measuredWidth.toFloat(), measuredHeight.toFloat())
//        path.lineTo(measuredWidth.toFloat(), 0f)
//        path.close()
//        path.rQuadTo(0f, measuredHeight.toFloat(), measuredWidth.toFloat(), measuredHeight.toFloat())

//        path.arcTo(0.8f * measuredWidth, 0.95f * measuredHeight, measuredWidth.toFloat(), measuredHeight.toFloat(), 90f, 30f, true)
//        path.arcTo(0f, 0f, width / 4f, width / 4f, 180f, 45f, false)

//        path.addArc(0f, 0f, width.toFloat(), height.toFloat(), 180f, 45f)
//        canvas?.translate(0f, -topArcRadius / 2)
//        canvas?.drawPath(path, paintFilled)
    }

}