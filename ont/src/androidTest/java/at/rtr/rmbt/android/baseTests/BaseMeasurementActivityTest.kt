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

import androidx.test.rule.ActivityTestRule
import at.rtr.rmbt.android.ui.activity.MeasurementActivity
import org.junit.Rule

open class BaseMeasurementActivityTest : BaseTest() {

    @get:Rule
    open val activityRule = ActivityTestRule(MeasurementActivity::class.java)
}