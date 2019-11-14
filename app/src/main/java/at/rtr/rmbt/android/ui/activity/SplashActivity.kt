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

package at.rtr.rmbt.android.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHomeActivity()
    }

    /*
        This is temporary function for wait 2 second,
        and open "HomeActivity". This function will remove
        when actual code implement.
     */
    private fun showHomeActivity() {
        Handler().postDelayed({
            finish()
            startActivity(Intent(this, HomeActivity::class.java))
        }, SPLASH_DISPLAY_TIME_MS)
    }

    companion object {
        private const val SPLASH_DISPLAY_TIME_MS: Long = 2000
    }
}