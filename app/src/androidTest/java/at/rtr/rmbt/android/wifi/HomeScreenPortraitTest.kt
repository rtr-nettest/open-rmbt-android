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

package at.rtr.rmbt.android.wifi

import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import at.rtr.rmbt.android.BaseHomeActivityTest
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.ui.activity.HomeActivity
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@LargeTest
open class HomeScreenPortraitTest : BaseHomeActivityTest() {

    private lateinit var context: HomeActivity

    @Before
    open fun setUp() {
        context = activityRule.activity
        assertTrue("No internet connection available", isConnected(context))
    }

    @Test
    fun checkWaveIsDisplayed() {
        onView(withId(R.id.waveView)).check(matches(isDisplayed()))
    }

    // waiting for all buttons functionality to be implemented
    /*@Test
    fun checkButtonsAreClickable() {
        for (i in 0 until context.findViewById<LinearLayout>(R.id.llBottomOptions).childCount) {
            onView(nthChildOf(withId(R.id.llBottomOptions), i)).check(matches((isClickable())))
        }
    }*/

    @Test
    fun checkWifiIconIsDisplayed() {
        val drawableIvConnectionType =
            context.findViewById<AppCompatImageView>(R.id.ivSignalLevel).drawable
        val icWifi1 = context.getDrawable(R.drawable.ic_wifi_1)
        val icWifi2 = context.getDrawable(R.drawable.ic_wifi_2)
        val icWifi3 = context.getDrawable(R.drawable.ic_wifi_3)
        val icWifi4 = context.getDrawable(R.drawable.ic_wifi_4)
        assertTrue(
            (drawableIvConnectionType.bytesEqualTo(icWifi1) and drawableIvConnectionType.pixelsEqualTo(
                icWifi1
            )) xor (drawableIvConnectionType.bytesEqualTo(icWifi2) and drawableIvConnectionType.pixelsEqualTo(
                icWifi2
            )) xor (drawableIvConnectionType.bytesEqualTo(icWifi3) and drawableIvConnectionType.pixelsEqualTo(
                icWifi3
            )) xor (drawableIvConnectionType.bytesEqualTo(icWifi4) and drawableIvConnectionType.pixelsEqualTo(
                icWifi4
            ))
        )
    }

    @Test
    fun checkIpv4ButtonIsYellow() {
        val drawableIpv4 =
            context.findViewById<AppCompatImageButton>(R.id.btnIpv4).drawable
        val icIpv4Yellow = context.getDrawable(R.drawable.ic_ipv4_yellow)
        assertTrue(
            drawableIpv4.bytesEqualTo(icIpv4Yellow) and drawableIpv4.pixelsEqualTo(
                icIpv4Yellow
            )
        )
    }

    @Test
    fun checkIpv4ButtonIsClickable() {
        onView(withId(R.id.btnIpv4)).check(matches(isClickable()))
    }

    @Test
    fun checkIpv4PopupIsDisplayed() {
        onView(withId(R.id.btnIpv4)).perform(click())
        onView(withId(R.id.ivIPIcon)).check(matches(isDisplayed()))
    }

    @Test
    fun checkIpv6ButtonIsRed() {
        val waitingTime: Long = 30
        TimeUnit.SECONDS.sleep(waitingTime)
        val drawableIpv6 =
            context.findViewById<AppCompatImageButton>(R.id.btnIpv6).drawable
        val icIpv6Red = context.getDrawable(R.drawable.ic_ipv6_red)
        assertTrue(
            drawableIpv6.bytesEqualTo(icIpv6Red) and drawableIpv6.pixelsEqualTo(
                icIpv6Red
            )
        )
    }
}