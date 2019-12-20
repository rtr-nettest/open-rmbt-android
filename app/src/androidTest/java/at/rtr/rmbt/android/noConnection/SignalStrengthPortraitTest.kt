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

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import at.rtr.rmbt.android.baseTests.BaseHomeActivityTest
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.ui.activity.HomeActivity
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@LargeTest
open class SignalStrengthPortraitTest : BaseHomeActivityTest() {
    private lateinit var context: HomeActivity

    @Before
    open fun setUp() {
        context = activityRule.activity
        while (isConnected(context)) {
            TimeUnit.MILLISECONDS.sleep(500)
        }
        TimeUnit.SECONDS.sleep(2)
        assertTrue("There is a connection available", !isConnected(context))
    }

    @Test
    fun checkSignalStrengthIsNull() {
        val signalStrength = "-"
        Espresso.onView(ViewMatchers.withId(R.id.tvSignal))
            .check(ViewAssertions.matches(ViewMatchers.withText(signalStrength)))
    }
}