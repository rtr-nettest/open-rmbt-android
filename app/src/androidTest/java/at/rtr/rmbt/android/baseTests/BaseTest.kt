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

package at.rtr.rmbt.android.baseTests

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.view.View
import android.view.ViewGroup
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import java.io.ByteArrayOutputStream
import java.util.Arrays

open class BaseTest {

    protected fun isConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    protected fun nthChildOf(parentMatcher: Matcher<View>, childPosition: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("with $childPosition child view of type parentMatcher")
            }

            override fun matchesSafely(view: View): Boolean {
                if (view.parent !is ViewGroup) {
                    return parentMatcher.matches(view.parent)
                }
                val group = view.parent as ViewGroup
                return parentMatcher.matches(view.parent) && group.getChildAt(childPosition) == view
            }
        }
    }

    protected fun <T : Drawable> T.bytesEqualTo(t: T?) =
        toBitmap().bytesEqualTo(t?.toBitmap(), true)

    protected fun <T : Drawable> T.pixelsEqualTo(t: T?) =
        toBitmap().pixelsEqualTo(t?.toBitmap(), true)

    private fun Bitmap.bytesEqualTo(otherBitmap: Bitmap?, shouldRecycle: Boolean = false) =
        otherBitmap?.let { other ->
            if (width == other.width && height == other.height) {
                val res = toBytes().contentEquals(other.toBytes())
                if (shouldRecycle) {
                    doRecycle().also { otherBitmap.doRecycle() }
                }
                res
            } else false
        } ?: kotlin.run { false }

    private fun Bitmap.pixelsEqualTo(otherBitmap: Bitmap?, shouldRecycle: Boolean = false) =
        otherBitmap?.let { other ->
            if (width == other.width && height == other.height) {
                val res = Arrays.equals(toPixels(), other.toPixels())
                if (shouldRecycle) {
                    doRecycle().also { otherBitmap.doRecycle() }
                }
                res
            } else false
        } ?: kotlin.run { false }

    private fun Bitmap.doRecycle() {
        if (!isRecycled) recycle()
    }

    private fun <T : Drawable> T.toBitmap(): Bitmap {
        if (this is BitmapDrawable) return bitmap

        val drawable: Drawable = this
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun Bitmap.toBytes(): ByteArray = ByteArrayOutputStream().use { stream ->
        compress(Bitmap.CompressFormat.JPEG, 100, stream)
        stream.toByteArray()
    }

    private fun Bitmap.toPixels() =
        IntArray(width * height).apply { getPixels(this, 0, width, 0, 0, width, height) }
}