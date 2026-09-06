package at.rtr.rmbt.android.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivitySignalMeasurementTermsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.viewmodel.HomeViewModel
import kotlin.math.max

class SignalMeasurementTermsActivity : BaseActivity() {

    private lateinit var binding: ActivitySignalMeasurementTermsBinding
    private val viewModel: HomeViewModel by viewModelLazy()

    // Once true, the background-location permission info screen is being shown (second step). The
    // next "accept" then requests the permission and proceeds to the measurement.
    private var backgroundInfoShown = false

    // We keep this terms screen in the foreground until the background-location permission flow has
    // returned (on Android 11+ that flow is the system settings page), and only THEN start the
    // measurement. Otherwise the settings page would be launched and immediately buried behind the
    // measurement screen, surfacing to the user only after the measurement is already over.
    private val requestBackgroundLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Remember whether the user granted it (so a decline is not re-asked, but a later loss of
            // a granted permission is), then proceed regardless of the choice: without the permission
            // the measurement simply cannot keep recording location once the app leaves the foreground.
            viewModel.recordBackgroundPermissionResult(granted)
            startMeasurementAndFinish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_signal_measurement_terms)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
                val insetsSystemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val insetsDisplayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
                val topSafe = max(insetsSystemBars.top, insetsDisplayCutout.top)
                val leftSafe = max(insetsSystemBars.left, insetsDisplayCutout.left)
                val rightSafe = max(insetsSystemBars.right, insetsDisplayCutout.right)
                val bottomSafe = max(insetsSystemBars.bottom, insetsDisplayCutout.bottom)

                v.updatePadding(
                    right = rightSafe,
                    left = leftSafe,
                    top = topSafe,
                    bottom = bottomSafe
                )
                WindowInsetsCompat.CONSUMED
            }
        }

        window?.changeStatusBarColor(ToolbarTheme.WHITE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
                val insetsSystemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                val insetsDisplayCutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout())
                val topSafe = max(insetsSystemBars.top, insetsDisplayCutout.top)
                val leftSafe = max(insetsSystemBars.left, insetsDisplayCutout.left)
                val rightSafe = max(insetsSystemBars.right, insetsDisplayCutout.right)
                val bottomSafe = max(insetsSystemBars.bottom, insetsDisplayCutout.bottom)

                v.updatePadding(
                    right = rightSafe,
                    left = leftSafe,
                    top = topSafe,
                    bottom = bottomSafe
                )
                WindowInsetsCompat.CONSUMED
            }
        }

        binding.decline.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.accept.setOnClickListener {
            when {
                !backgroundInfoShown && viewModel.shouldAskForBackgroundPermission(this) -> {
                    // Signal (coverage) measurements keep running as a foreground service while the
                    // app is in the background, so - exactly as in loop mode - show the
                    // background-location permission info screen before requesting the permission.
                    showBackgroundPermissionInfo()
                }
                // Reaching the info screen already guarantees the permission is requestable and not
                // yet granted (see shouldAskForBackgroundPermission), so just launch the request and
                // wait for the result before starting the measurement.
                backgroundInfoShown ->
                    requestBackgroundLocationPermission.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                else -> startMeasurementAndFinish()
            }
        }
    }

    private fun showBackgroundPermissionInfo() {
        backgroundInfoShown = true
        binding.title.text = getString(R.string.title_signal_measurement_background_permission)
        binding.content.text = getString(R.string.text_signal_measurement_background_permission)
        // "Accept" would be misleading here: this button does not grant the permission, it forwards
        // the user to the system settings screen where they grant it. Label it "Continue".
        binding.accept.text = getString(R.string.text_button_continue)
        binding.scrollView.scrollTo(0, 0)
    }

    private fun startMeasurementAndFinish() {
        setResult(Activity.RESULT_OK)
        SignalMeasurementActivity.start(this)
        finish()
    }

    companion object {

        fun start(context: Context): Intent = Intent(context, SignalMeasurementTermsActivity::class.java)
    }
}