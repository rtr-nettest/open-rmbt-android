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

import android.animation.Animator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.plusAssign
import androidx.navigation.ui.setupWithNavController
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityHomeBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.dialog.ConfigCheckDialog
import at.rtr.rmbt.android.ui.dialog.NetworkInfoDialog
import at.rtr.rmbt.android.util.KeepStateNavigator
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.util.listenNonNull
import at.rtr.rmbt.android.util.setTechnologyIcon
import at.rtr.rmbt.android.viewmodel.ConfigCheckViewModel
import at.rtr.rmbt.android.viewmodel.MeasurementViewModel
import at.specure.data.entity.LoopModeState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding

    private val viewModel: MeasurementViewModel by viewModelLazy()
    private val configCheckViewModel: ConfigCheckViewModel by viewModelLazy()

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
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_home)

        if (viewModel.config.analyticsEnabled) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        } else {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
        }
        Timber.d("Firebase crashlytics enabled: ${viewModel.config.analyticsEnabled}")

        setTransparentStatusBar()

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        val navigator = KeepStateNavigator(this, navHostFragment.childFragmentManager, R.id.navHostFragment)
        navController.navigatorProvider += navigator
        navController.setGraph(R.navigation.mobile_navigation)

        binding.navView.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            checkBasicNetworkInfoVisibility()
        }

        viewModel.isTestsRunningLiveData.listen(this) { isRunning ->
            if (isRunning) {
                if (viewModel.config.loopModeEnabled) {
                    if (viewModel.state.loopModeRecord.get()?.status != LoopModeState.FINISHED && viewModel.state.loopModeRecord.get()?.status != LoopModeState.CANCELLED && viewModel.state.loopLocalUUID.get() != null && (viewModel.state.loopModeRecord.get()?.testsPerformed != viewModel.config.loopModeNumberOfTests)) {
                        MeasurementActivity.start(this)
                    }
                } else {
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

        configCheckViewModel.incorrectValuesLiveData.listenNonNull(this) {
            ConfigCheckDialog.show(supportFragmentManager, it)
        }


        viewModel.activeNetworkLiveData.listenNonNull(this) { info ->
            binding.textNetworkName.text = info.name
            binding.textNetworkType.setTechnologyIcon(info)
        }

        binding.basicNetworkInfo.setOnClickListener {
            binding.basicNetworkInfo.animate()
                .scaleX(SCALE_BIG)
                .scaleY(SCALE_BIG)
                .setListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animation: Animator?) {
                        NetworkInfoDialog
                            .show(supportFragmentManager) {
                                binding.basicNetworkInfo.animate().scaleX(SCALE_DEFAULT).scaleY(SCALE_DEFAULT).start()
                            }
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        binding.basicNetworkInfo.animate().setListener(null)
                    }

                    override fun onAnimationCancel(animation: Animator?) {}

                    override fun onAnimationRepeat(animation: Animator?) {}
                })
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

    private fun checkBasicNetworkInfoVisibility() {
        viewModel.connectivityInfoLiveData.listen(this) { info ->
            binding.root.post {
                if (info == null) {
                    binding.basicNetworkInfo.visibility = View.GONE

                } else {
                    binding.basicNetworkInfo.visibility = if (binding.navView.selectedItemId == R.id.navigation_home) View.VISIBLE else View.GONE
                }
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

        private const val SCALE_DEFAULT = 1f
        private const val SCALE_BIG = 1.1f

        fun start(context: Context) = context.startActivity(Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        })

        fun startWithFragment(context: Context, fragmentToShow: HomeNavigationTarget) {
            val intent = Intent(context, HomeActivity::class.java)
            intent.putExtra(FRAGMENT_TO_START_BUNDLE_KEY, fragmentToShow)
            context.startActivity(intent)
        }
    }
}