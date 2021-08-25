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

package at.rtr.rmbt.android.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver

/**
 * Half part of this view is drawn by primary wave color
 * for horizontal orientation bottom part will be filled with [calmColor]
 * for vertical orientation right part will be filled with [calmColor]
 */
class CalmSpriteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        this.color = Color.BLACK
    }

    private var path: Path? = null

    /**
     * Primary color. Part of view will be filled with this color
     * [Color.BLACK] is using by default
     */
    var calmColor: Int
        get() = paint.color
        set(value) {
            paint.color = value
        }

    init {

        val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            path = if (width >= height) {
                horizontalPath()
            } else {
                verticalPath()
            }
        }
        viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        path?.let {
            canvas?.drawPath(it, paint)
        }
    }

    private fun horizontalPath() = Path().apply {
        val width = this@CalmSpriteView.width.toFloat()
        val height = this@CalmSpriteView.height.toFloat()
        val midHeight = height / 2

        moveTo(0f, midHeight)
        lineTo(width, midHeight)
        lineTo(width, height)
        lineTo(0f, height)

        close()
    }

    private fun verticalPath() = Path().apply {
        val width = this@CalmSpriteView.width.toFloat()
        val height = this@CalmSpriteView.height.toFloat()
        val midWidth = width / 2

        moveTo(midWidth, 0f)
        lineTo(midWidth, height)
        lineTo(width, height)
        lineTo(width, 0f)

        close()
    }
}