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

package at.rtr.rmbt.android.noConnection

import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import at.rtr.rmbt.android.BaseHomeActivityTest
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.ui.activity.HomeActivity
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
open class HomeScreenPortraitTest : BaseHomeActivityTest() {

    private lateinit var context: HomeActivity
    @Before
    open fun setUp() {
        context = activityRule.activity
        assertFalse("There is internet connection available", isConnected(context))
    }

    @Test
    fun checkNoInternetTextIsDisplayed() {
        onView(withText(R.string.home_no_internet_connection)).check(matches(isDisplayed()))
    }

    // waiting for all buttons functionality to be implemented
    /*@Test
    fun checkButtonsAreNotClickable() {
        for (i in 0 until context.findViewById<LinearLayout>(R.id.llBottomOptions).childCount) {
            onView(nthChildOf(withId(R.id.llBottomOptions), i)).check(matches(not(isClickable())))
        }
    }*/

    @Test
    fun checkNoInternetIconIsDisplayed() {
        val drawableIvConnectionType =
            context.findViewById<AppCompatImageView>(R.id.ivSignalLevel).drawable
        val icNoInternet = context.getDrawable(R.drawable.ic_no_internet)
        assertTrue(
            drawableIvConnectionType.bytesEqualTo(icNoInternet) and drawableIvConnectionType.pixelsEqualTo(
                icNoInternet
            )
        )
    }

    @Test
    fun checkTextColorIsGray() {
        val colorOfTvNetworkNameDisplayed =
            context.findViewById<TextView>(R.id.tvNetworkName).currentTextColor
        val contextColorOfTvNetworkName = context.getColor(R.color.text_dark_gray)
        assertTrue(colorOfTvNetworkNameDisplayed == contextColorOfTvNetworkName)
    }

    @Test
    fun checkIpv4ButtonIsGray() {
        val drawableIpv4 =
            context.findViewById<AppCompatImageButton>(R.id.btnIpv4).drawable
        val icIpv4Gray = context.getDrawable(R.drawable.ic_ipv4_gray)
        assertTrue(
            drawableIpv4.bytesEqualTo(icIpv4Gray) and drawableIpv4.pixelsEqualTo(
                icIpv4Gray
            )
        )
    }

    @Test
    fun checkIpv4ButtonIsNotClickable() {
        onView(withId(R.id.btnIpv4)).check(matches(not(isClickable())))
    }

    @Test
    fun checkIpv6ButtonIsGray() {
        val drawableIpv6 =
            context.findViewById<AppCompatImageButton>(R.id.btnIpv6).drawable
        val icIpv6Gray = context.getDrawable(R.drawable.ic_ipv6_gray)
        assertTrue(
            drawableIpv6.bytesEqualTo(icIpv6Gray) and drawableIpv6.pixelsEqualTo(
                icIpv6Gray
            )
        )
    }

    @Test
    fun checkIpv6ButtonIsNotClickable() {
        onView(withId(R.id.btnIpv6)).check(matches(not(isClickable())))
    }
}