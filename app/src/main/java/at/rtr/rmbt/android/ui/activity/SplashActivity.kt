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

import android.os.Handler

class SplashActivity : BaseActivity() {

    private val startHomeRunnable = Runnable {
        finish()
        HomeActivity.start(this)
    }

    private val delayedStartHandler = Handler()

    override fun onStart() {
        super.onStart()
        delayedStartHandler.postDelayed(startHomeRunnable, SPLASH_DISPLAY_TIME_MS)
    }

    override fun onStop() {
        super.onStop()
        delayedStartHandler.removeCallbacks(startHomeRunnable)
    }

    companion object {
        private const val SPLASH_DISPLAY_TIME_MS: Long = 2000
    }
}