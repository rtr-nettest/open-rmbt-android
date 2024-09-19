/*
 *
 *  Licensed under the Apache License, Version 2.0 (the “License”);
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an “AS IS” BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.R
import at.specure.data.Classification

class ResultBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    View(context, attrs, defStyleAttr) {

    var squareSize: Float = 0f
        set(value) {
            field = value * 2f * resources.displayMetrics.density
        }

    var progress: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
        setWillNotDraw(false)
        squareSize = 270f / 128
    }

    private var emptySquarePaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.classification_undefined)
        style = Paint.Style.FILL_AND_STROKE
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        isAntiAlias = true
    }

    private var progressPaint = Paint().apply {
        color = classColor
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    }

    /**
     * Defines count of squares in graph
     */
    private var verticalCount = 2
    private var horizontalCount = 12
    private var classColor = ContextCompat.getColor(context, R.color.classification_undefined)

    private var currentCanvas: Canvas? = null
    private var bitmap: Bitmap? = null
    private var classification: Classification = Classification.NONE

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val width = SQUARE_MULTIPLIER * squareSize * (horizontalCount - 1) + squareSize
        val height = SQUARE_MULTIPLIER * squareSize * (verticalCount - 1) + squareSize
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width.toInt(), MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(height.toInt(), MeasureSpec.AT_MOST)

        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        if (changed) {
            createBitmap()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
    }

    private fun createBitmap() {
        if (measuredHeight > 0 && measuredWidth > 0) {
            with(Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)) {
                bitmap = this
                val canvas = Canvas(this)
                currentCanvas = canvas
                drawBackground(canvas)
                currentCanvas?.apply {
                    drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    drawBackground(this)
                    progressPaint = Paint().apply {
                        color = classColor
                        style = Paint.Style.FILL
                        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
                    }
                    drawRect(0f, 0f, (measuredWidth * progress / 100).toFloat(), measuredHeight.toFloat(), progressPaint)
                }
            }
        }
    }

    /**
     * Draw gray squares for the background
     */
    private fun drawBackground(canvas: Canvas) {

        var y = 0.0f
        for (i in 0 until verticalCount) {
            var x = 0.0f
            for (j in 0 until horizontalCount) {
                canvas.drawRect(x, y, x + squareSize, y + squareSize, emptySquarePaint)
                x += squareSize * SQUARE_MULTIPLIER
            }
            y += squareSize * SQUARE_MULTIPLIER
        }
    }

    /**
     * Calculate the size of filled graph of part and draw it
     */
    fun updateClassification(percentage: Int, classification: Classification) {
        classColor = ContextCompat.getColor(
            context, when (classification) {
                Classification.NONE -> R.color.classification_undefined
                Classification.BAD -> R.color.classification_red
                Classification.NORMAL -> R.color.classification_yellow
                Classification.GOOD -> R.color.classification_green
                Classification.EXCELLENT -> R.color.classification_green_dark
                else -> R.color.classification_undefined
            }
        )
        contentDescription = ContextCompat.getString(
            context, when (classification) {
                Classification.NONE -> R.string.classification_undefined
                Classification.BAD -> R.string.classification_bad
                Classification.NORMAL -> R.string.classification_normal
                Classification.GOOD -> R.string.classification_good
                Classification.EXCELLENT -> R.string.classification_excellent
                else -> R.string.classification_undefined
            }
        )
        progress = percentage

        createBitmap()
    }

    companion object {
        private const val SQUARE_MULTIPLIER = 1.75f
    }
}