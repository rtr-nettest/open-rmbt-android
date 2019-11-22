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

package at.rtr.rmbt.android.mobileData

import androidx.appcompat.widget.AppCompatImageView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import at.rtr.rmbt.android.BaseHomeActivityTest
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.ui.activity.HomeActivity
import junit.framework.Assert.assertTrue
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
    fun checkMobileDataIconIsDisplayed() {
        val drawableIvConnectionType =
            context.findViewById<AppCompatImageView>(R.id.ivSignalLevel).drawable
        val icMobileData1 = context.getDrawable(R.drawable.ic_mobile_1)
        val icMobileData2 = context.getDrawable(R.drawable.ic_mobile_2)
        val icMobileData3 = context.getDrawable(R.drawable.ic_mobile_3)
        val icMobileData4 = context.getDrawable(R.drawable.ic_mobile_4)
        assertTrue(
            (drawableIvConnectionType.bytesEqualTo(icMobileData1) and drawableIvConnectionType.pixelsEqualTo(
                icMobileData1
            )) xor (drawableIvConnectionType.bytesEqualTo(icMobileData2) and drawableIvConnectionType.pixelsEqualTo(
                icMobileData2
            )) xor (drawableIvConnectionType.bytesEqualTo(icMobileData3) and drawableIvConnectionType.pixelsEqualTo(
                icMobileData3
            )) xor (drawableIvConnectionType.bytesEqualTo(icMobileData4) and drawableIvConnectionType.pixelsEqualTo(
                icMobileData4
            ))
        )
    }

    @Test
    fun checkMobileTechnologyIconIsDisplayed() {
        val drawableIvConnectionType =
            context.findViewById<AppCompatImageView>(R.id.ivTechnology).drawable
        val icMobileTech1 = context.getDrawable(R.drawable.ic_2g)
        val icMobileTech2 = context.getDrawable(R.drawable.ic_3g)
        val icMobileTech3 = context.getDrawable(R.drawable.ic_4g)
        assertTrue(
            (drawableIvConnectionType.bytesEqualTo(icMobileTech1) and drawableIvConnectionType.pixelsEqualTo(
                icMobileTech1
            )) xor (drawableIvConnectionType.bytesEqualTo(icMobileTech2) and drawableIvConnectionType.pixelsEqualTo(
                icMobileTech2
            )) xor (drawableIvConnectionType.bytesEqualTo(icMobileTech3) and drawableIvConnectionType.pixelsEqualTo(
                icMobileTech3
            ))
        )
    }
}