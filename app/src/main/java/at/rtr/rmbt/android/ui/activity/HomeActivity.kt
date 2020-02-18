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
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.plusAssign
import androidx.navigation.ui.setupWithNavController
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityHomeBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.util.KeepStateNavigator
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.MeasurementViewModel

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val viewModel: MeasurementViewModel by viewModelLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_home)

        setTransparentStatusBar()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navigator = KeepStateNavigator(this, navHostFragment.childFragmentManager, R.id.navHostFragment)
        navController.navigatorProvider += navigator
        navController.setGraph(R.navigation.mobile_navigation)

        binding.navView.setupWithNavController(navController)

        viewModel.isTestsRunningLiveData.listen(this) { isRunning ->
            if (isRunning) {
                MeasurementActivity.start(this)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.attach(this)

        viewModel.tacAcceptanceLiveData.listen(this) {
            if (!it) {
                TermsAcceptanceActivity.start(this, CODE_TERMS)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.detach(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CODE_TERMS) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.updateTermsAcceptance(true)
            } else {
                finish()
            }
        }
    }

    companion object {

        private const val CODE_TERMS = 1

        fun start(context: Context) = context.startActivity(Intent(context, HomeActivity::class.java))
    }
}