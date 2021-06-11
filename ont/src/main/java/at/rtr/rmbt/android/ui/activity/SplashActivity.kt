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

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.viewmodel.SplashViewModel
import at.specure.worker.WorkLauncher

class SplashActivity : BaseActivity() {

    private val viewModel: SplashViewModel by viewModelLazy()

    private val startHomeRunnable = Runnable {
        val accepted = viewModel.isTacAccepted()
        if (!accepted) {
            termsIsShown = true
            TermsAcceptanceActivity.start(this, CODE_TERMS)
        } else {
            finishAffinity()
            if (viewModel.shouldAskForPermission() && hasDeniedPermissions()) {
                PermissionsActivity.start(this)
            } else {
                HomeActivity.start(this)
            }
        }
    }

    private val delayedStartHandler = Handler()

    private var termsIsShown: Boolean = false

    override fun onStart() {
        super.onStart()
        if (!termsIsShown) {
            delayedStartHandler.postDelayed(startHomeRunnable, SPLASH_DISPLAY_TIME_MS)
        }
    }

    override fun onStop() {
        super.onStop()
        delayedStartHandler.removeCallbacks(startHomeRunnable)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CODE_TERMS) {
            termsIsShown = false
            if (resultCode == Activity.RESULT_OK) {
                viewModel.updateTermsAcceptance(true)
                WorkLauncher.enqueueSettingsRequest(this)
                finishAffinity()
                HomeActivity.start(this)
            } else {
                finish()
            }
        }
    }

    private fun hasDeniedPermissions() = viewModel.permissionsWatcher.requiredPermissions.any {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_DENIED
    }

    companion object {
        private const val SPLASH_DISPLAY_TIME_MS: Long = 2000
        private const val CODE_TERMS = 1
    }
}