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
 * Wave that draws wave
 */
class WaveSpriteView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        this.color = Color.BLACK
    }

    private var path: Path? = null

    /**
     * Color of wave. [Color.BLACK] will be used by default
     */
    var waveColor: Int
        get() = paint.color
        set(value) {
            paint.color = value
        }

    /**
     * If is odd it will start from 0 y coordinate like SIN, otherwise - from 1 as COS
     */
    var isOddWave: Boolean = true

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
        val width = this@WaveSpriteView.width.toFloat()
        val height = this@WaveSpriteView.height.toFloat()
        val midHeight = height / 2

        moveTo(0f, midHeight)

        val heightOffset = height / 5

        var x = 0f
        val count = 4 // this count should be calculated that image self repeats once like a tile
        val length = width / count

        val start = if (isOddWave) 1 else 0
        val end = if (isOddWave) count + 1 else count

        for (i in start..end) {
            if (i % 2 == 0) {
                quadTo(x + (length / 2), 0f + heightOffset, x + length, midHeight)
            } else {
                quadTo(x + (length / 2), height - heightOffset, x + length, midHeight)
            }

            x += length
        }

        lineTo(width, height)
        lineTo(0f, height)

        close()
    }

    private fun verticalPath() = Path().apply {
        val width = this@WaveSpriteView.width.toFloat()
        val height = this@WaveSpriteView.height.toFloat()
        val midWidth = width / 2

        moveTo(midWidth, 0f)

        val widthOffset = width / 5

        var y = 0f
        val count = 4
        val length = height / count

        val start = if (isOddWave) 1 else 0
        val end = if (isOddWave) count + 1 else count

        for (i in start..end) {
            if (i % 2 == 0) {
                quadTo(0f + widthOffset, y + (length / 2), midWidth, y + length)
            } else {
                quadTo(width - widthOffset, y + (length / 2), midWidth, y + length)
            }
            y += length
        }

        lineTo(width, height)
        lineTo(width, 0f)

        close()
    }
}