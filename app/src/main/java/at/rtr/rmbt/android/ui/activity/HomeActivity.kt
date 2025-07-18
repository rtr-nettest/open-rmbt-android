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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.navigation.Navigator
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.plusAssign
import androidx.navigation.ui.setupWithNavController
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityHomeBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.dialog.ConfigCheckDialog
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.ConfigCheckViewModel
import at.rtr.rmbt.android.viewmodel.MeasurementViewModel
import at.specure.data.entity.LoopModeState
import timber.log.Timber

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val viewModel: MeasurementViewModel by viewModelLazy()
    private val configCheckViewModel: ConfigCheckViewModel by viewModelLazy()
    private var termsIsShown: Boolean = false

    private fun startHomeRunnable() {
        val accepted = viewModel.isTacAccepted
        if (!accepted) {
            termsIsShown = true
            finish()
            TermsAcceptanceActivity.start(this)
        }
    }
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        when (intent?.extras?.get(FRAGMENT_TO_START_BUNDLE_KEY) ?: HomeNavigationTarget.HOME_FRAGMENT_TO_SHOW) {
            HomeNavigationTarget.HISTORY_FRAGMENT_TO_SHOW -> binding.navView.selectedItemId = R.id.navigation_history
            HomeNavigationTarget.HOME_FRAGMENT_TO_SHOW -> binding.navView.selectedItemId = R.id.navigation_home
            HomeNavigationTarget.STATISTIC_FRAGMENT_TO_SHOW -> binding.navView.selectedItemId = R.id.navigation_statistics
            HomeNavigationTarget.MAP_FRAGMENT_TO_SHOW -> binding.navView.selectedItemId = R.id.navigation_map
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        startHomeRunnable()
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_home)

        setTransparentStatusBar()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.setGraph(R.navigation.mobile_navigation)

        binding.navView.setupWithNavController(navController)

        viewModel.isTestsRunningLiveData.listen(this) { isRunning ->
            Timber.d("Test is running: $isRunning,  Loop mode enabled: ${viewModel.config.loopModeEnabled} Loop mode record status: ${viewModel.state.loopModeRecord.get()?.status}, performed: ${viewModel.state.loopModeRecord.get()?.testsPerformed}")
            if (isRunning) {
                if (viewModel.config.loopModeEnabled) {
                    if (viewModel.state.loopModeRecord.get()?.status != LoopModeState.FINISHED
                        && viewModel.state.loopModeRecord.get()?.status != LoopModeState.CANCELLED
                        && viewModel.state.loopLocalUUID.get() != null
                        && (((viewModel.state.loopModeRecord.get()?.testsPerformed
                            ?: 0) <= viewModel.config.loopModeNumberOfTests && viewModel.config.loopModeNumberOfTests != 0) || viewModel.config.loopModeNumberOfTests == 0)
                    ) {
                        Timber.d("Starting measurement activity because loop measurement is running")
                        MeasurementActivity.start(this)
                    }
                } else {
                    Timber.d("Starting measurement activity because measurement is running")
                    MeasurementActivity.start(this)
                }
            }
        }

        if (savedInstanceState == null) {
            when (intent.extras?.get(FRAGMENT_TO_START_BUNDLE_KEY) ?: HomeNavigationTarget.HOME_FRAGMENT_TO_SHOW) {
                HomeNavigationTarget.HISTORY_FRAGMENT_TO_SHOW -> binding.navView.selectedItemId = R.id.navigation_history
                HomeNavigationTarget.HOME_FRAGMENT_TO_SHOW -> binding.navView.selectedItemId = R.id.navigation_home
                HomeNavigationTarget.STATISTIC_FRAGMENT_TO_SHOW -> binding.navView.selectedItemId = R.id.navigation_statistics
                HomeNavigationTarget.MAP_FRAGMENT_TO_SHOW -> binding.navView.selectedItemId = R.id.navigation_map
            }
        }

        binding.navView.menu.findItem(R.id.navigation_home).setContentDescription(getString(R.string.home_home))
        binding.navView.menu.findItem(R.id.navigation_history).setContentDescription(getString(R.string.home_history))
        binding.navView.menu.findItem(R.id.navigation_statistics).setContentDescription(getString(R.string.home_statistics))
        binding.navView.menu.findItem(R.id.navigation_map).setContentDescription(getString(R.string.home_map))

        configCheckViewModel.incorrectValuesLiveData.listen(this) {
            ConfigCheckDialog.show(supportFragmentManager, it)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                binding.navView.updatePadding(bottom = insets.bottom)
//            WindowInsetsCompat.CONSUMED
                windowInsets
            }
        }
    }

    override fun onBackPressed() {
        if (binding.navView.selectedItemId == R.id.navigation_home) {
            super.onBackPressed()
        } else {
            binding.navView.selectedItemId = R.id.navigation_home
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.attach(this)
        configCheckViewModel.checkConfig()
    }

    override fun onStop() {
        super.onStop()
        viewModel.detach(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CODE_TERMS) {
            if (resultCode != Activity.RESULT_OK) {
                finish()
            }
        }
    }

    companion object {

        enum class HomeNavigationTarget {
            HISTORY_FRAGMENT_TO_SHOW,
            HOME_FRAGMENT_TO_SHOW,
            STATISTIC_FRAGMENT_TO_SHOW,
            MAP_FRAGMENT_TO_SHOW
        }

        const val FRAGMENT_TO_START_BUNDLE_KEY = "FRAGMENT_TO_START_BUNDLE_KEY"

        private const val CODE_TERMS = 1

        fun start(context: Context) = context.startActivity(Intent(context, HomeActivity::class.java))

        fun startWithFragment(context: Context, fragmentToShow: HomeNavigationTarget) {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra(FRAGMENT_TO_START_BUNDLE_KEY, fragmentToShow)
            context.startActivity(intent)
        }
    }
}