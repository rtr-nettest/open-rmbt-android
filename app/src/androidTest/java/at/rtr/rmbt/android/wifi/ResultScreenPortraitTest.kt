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

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.baseTests.BaseHomeActivityTest
import at.rtr.rmbt.android.ui.activity.HomeActivity
import at.rtr.rmbt.android.ui.activity.ResultsActivity
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
@LargeTest
open class ResultScreenPortraitTest : BaseHomeActivityTest() {

    private lateinit var context: HomeActivity

    @Before
    open fun setUp() {
        context = activityRule.activity
        assertTrue("No internet connection available", isConnected(context))
        onView(withId(R.id.ivSignalLevel)).perform(click())
        val resultsActivityLocalClassName = ResultsActivity::class.java.toString().split(" ")[1]
        val timeoutSeconds = 180
        var timeCounter = 0
        while (((if (getCurrentActivity() != null) getCurrentActivity()?.localClassName else "noActivity") != resultsActivityLocalClassName) and (timeCounter < timeoutSeconds)) {
            TimeUnit.SECONDS.sleep(1)
            timeCounter++
        }
        assertTrue("Timeout for measurement", timeCounter < timeoutSeconds)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Test
    fun checkDetailedResultsScreenElementsAreDisplayed() {
        onView(withId(R.id.titleOtherDetails)).check(matches(isDisplayed()))
        onView(withId(R.id.qualityContainer)).check(matches(isDisplayed()))
        onView(withId(R.id.buttonBack)).check(matches(isDisplayed()))
        onView(withId(R.id.testTime)).check(matches(isDisplayed()))
        onView(withId(R.id.networkTypeIcon)).check(matches(isDisplayed()))
        onView(withId(R.id.labelResultBottomDownload)).check(matches(isDisplayed()))
        onView(withId(R.id.labelResultBottomUpload)).check(matches(isDisplayed()))
        onView(withId(R.id.labelResultBottomPing)).check(matches(isDisplayed()))
        onView(withId(R.id.labelResultBottomSignal)).check(matches(isDisplayed()))
    }
}