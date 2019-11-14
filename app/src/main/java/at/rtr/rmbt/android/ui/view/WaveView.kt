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

import android.animation.Animator
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.R

private const val ANIMATION_SPEED = 1700L
private const val NEXT_VIEW_DELAY = 500L

private const val TAG_WAVE = "WAVE"
private const val TAG_CALM = "CALM"

/**
 * Wave view that runs animation in a loop making wave affect
 */
class WaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var waveEnabled: Boolean = false
        set(value) {
            field = value
            for (i in 0 until childCount) {
                val child = getChildAt(i)
                if (child.tag == TAG_WAVE) {
                    child.visibility = if (value) View.VISIBLE else View.INVISIBLE
                } else {
                    child.visibility = if (value) View.INVISIBLE else View.VISIBLE
                }
            }
        }

    private val animators = mutableSetOf<AnimationRepeater>()
    private var setup = true
    private val isVertical: Boolean
        get() = height > width

    init {
        val waveSprite1 = WaveSpriteView(context).apply {
            waveColor = ContextCompat.getColor(context, R.color.wave_primary)
            tag = TAG_WAVE
            visibility = View.INVISIBLE
        }
        val waveSprite2 = WaveSpriteView(context).apply {
            waveColor = ContextCompat.getColor(context, R.color.wave_secondary)
            isOddWave = false
            tag = TAG_WAVE
            visibility = View.INVISIBLE
        }

        val calmSprite = CalmSpriteView(context).apply {
            calmColor = ContextCompat.getColor(context, R.color.wave_primary)
            tag = TAG_CALM
        }

        addView(waveSprite1)
        addView(waveSprite2)
        addView(calmSprite)

        val layoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (setup) {
                    animators.forEach {
                        it.stop()
                    }
                    animators.clear()

                    for (i in 0 until childCount) {
                        val child = getChildAt(i)
                        if (child.tag == TAG_WAVE) {
                            val params = child.layoutParams as LayoutParams
                            if (isVertical) {
                                params.height = child.height * 2
                            } else {
                                params.width = child.width * 2
                            }
                            child.layoutParams = params
                            animators.add(AnimationRepeater(child, ANIMATION_SPEED - (i * NEXT_VIEW_DELAY))) // random duration for test
                        }
                    }
                    setup = false
                } else {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    startAnimation()
                }
            }
        }
        viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    private fun startAnimation() {
        animators.forEach {
            it.start()
        }
    }

    private inner class AnimationRepeater(val child: View, val delay: Long) : Animator.AnimatorListener {

        override fun onAnimationRepeat(p0: Animator?) {}

        override fun onAnimationEnd(p0: Animator?) {
            child.translationX = 0f
            child.translationY = 0f
            start()
        }

        override fun onAnimationCancel(p0: Animator?) {}

        override fun onAnimationStart(p0: Animator?) {}

        fun start() {
            child.clearAnimation()
            val animator = child.animate()
                .setInterpolator(LinearInterpolator())
                .setDuration(delay)
                .setListener(this)

            if (isVertical) {
                animator.translationY(-child.height.toFloat() / 2f)
            } else {
                animator.translationX(-child.width.toFloat() / 2f)
            }

            animator.start()
        }

        fun stop() {
            child.clearAnimation()
        }
    }
}