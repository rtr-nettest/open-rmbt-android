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

package at.rtr.rmbt.android

import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import at.rtr.rmbt.android.ui.activity.HomeActivity
import org.junit.Rule

open class BaseHomeActivityTest : BaseTest() {

    @get:Rule
    var permissionRuleLocation: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)
    @get:Rule
    var permissionRulePhone: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.READ_PHONE_STATE)

    @get:Rule
    open val activityRule = ActivityTestRule(HomeActivity::class.java)
}