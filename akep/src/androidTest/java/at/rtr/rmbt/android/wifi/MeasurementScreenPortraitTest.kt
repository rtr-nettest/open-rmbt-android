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

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.baseTests.BaseMeasurementActivityTest
import at.rtr.rmbt.android.ui.activity.MeasurementActivity
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@LargeTest
open class MeasurementScreenPortraitTest : BaseMeasurementActivityTest() {
    private lateinit var context: MeasurementActivity

    @Before
    open fun setUp() {
        context = activityRule.activity
        while (!isConnected(context)) {
            TimeUnit.MILLISECONDS.sleep(500)
        }
        TimeUnit.SECONDS.sleep(2)
        assertTrue("No internet connection available", isConnected(context))
    }

    @Test
    fun checkSignalStrengthBarIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(R.id.strength))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun checkMeasurementBottomViewIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(R.id.measurementBottomView))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun checkMeasurementCurveLayoutIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(R.id.curve_layout))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun checkNetworkNameIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(R.id.network_name))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}