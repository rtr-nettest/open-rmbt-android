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

package at.rtr.rmbt.android.locationEnabled

import androidx.appcompat.widget.AppCompatImageButton
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.baseTests.BaseHomeActivityTest
import at.rtr.rmbt.android.ui.activity.HomeActivity
import junit.framework.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
open class HomeScreenPortraitTest : BaseHomeActivityTest() {

    private lateinit var context: HomeActivity

    @Test
    fun checkLocationIconGreen() {
        context = activityRule.activity
        val drawableIvLocationButton =
            context.findViewById<AppCompatImageButton>(R.id.btnLocation).drawable
        val icLocationEnabled = context.getDrawable(R.drawable.ic_location)
        Assert.assertTrue(
            (drawableIvLocationButton.bytesEqualTo(icLocationEnabled) and drawableIvLocationButton.pixelsEqualTo(
                icLocationEnabled
            ))
        )
    }

    @Test
    fun checkLocationDialogIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(R.id.btnLocation)).perform(click())
        Espresso.onView(ViewMatchers.withText(R.string.location_dialog_label_title)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(R.string.location_dialog_label_position)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}