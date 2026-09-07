package at.rtr.rmbt.android.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivitySignalMeasurementTermsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HomeViewModel
import kotlin.math.max

class SignalMeasurementTermsActivity : BaseActivity() {

    private lateinit var binding: ActivitySignalMeasurementTermsBinding
    private val viewModel: HomeViewModel by viewModelLazy()

    // Once true, the background-location permission info screen is being shown (second step). The
    // next "accept" then requests the permission and proceeds.
    private var backgroundInfoShown = false

    // Once true, consent is done and this screen is showing the "waiting for GPS/network" state,
    // continuously re-checking (via the observers below) and starting the measurement the moment the
    // status becomes good - no further user interaction needed. The only action offered is "Abort".
    private var waitingForStatus = false

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
            proceedAfterConsent()
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

        // "Decline" during the terms/background steps, "Abort" during the waiting step - both simply
        // cancel and return to the start screen.
        binding.decline.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.accept.setOnClickListener { onTermsAccepted() }

        // Observe the GPS and network status live. Observing also keeps the GNSS source warm while
        // this full-screen consent flow is shown (the home screen's own observer is paused), so the
        // fix stays fresh and, once consent is done, the measurement can start without a cold GPS
        // re-acquisition delay. While waiting, each change re-checks whether we can start now.
        viewModel.gpsLocationLiveData.listen(this) { maybeStartWhenReady() }
        viewModel.locationStateLiveData.listen(this) { maybeStartWhenReady() }
        viewModel.activeNetworkLiveData.listen(this) { maybeStartWhenReady() }
        viewModel.signalStrengthLiveData.listen(this) { info ->
            // Keep the active-network info (used by isMobileNetworkActive) current on this screen's
            // own view model instance.
            viewModel.state.activeNetworkInfo.set(info?.copy())
            maybeStartWhenReady()
        }

        if (viewModel.shouldShowSignalMeasurementTerms()) {
            // Display the usage-terms page (the default layout) and count it.
            viewModel.signalMeasurementTermsDisplayed()
        } else {
            // The terms have already been shown enough times since installation: skip the terms page
            // and go straight to the post-consent flow (background-permission page if still needed,
            // then waiting-for-status / starting the measurement).
            onTermsAccepted()
        }
    }

    /**
     * The usage terms were accepted (or skipped after being shown enough times). Advances the flow:
     * background-permission info page -> permission request -> proceed to the measurement.
     */
    private fun onTermsAccepted() {
        when {
            !backgroundInfoShown && viewModel.shouldAskForBackgroundPermission(this) -> {
                // Signal (coverage) measurements keep running as a foreground service while the app
                // is in the background, so - exactly as in loop mode - show the background-location
                // permission info screen before requesting the permission.
                showBackgroundPermissionInfo()
            }
            // Reaching the info screen already guarantees the permission is requestable and not yet
            // granted (see shouldAskForBackgroundPermission - which only returns true on Android Q+,
            // where the separate background-location permission exists), so just launch the request
            // and wait for the result before proceeding. The explicit SDK check keeps lint happy
            // about the Q-only permission constant.
            backgroundInfoShown && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                requestBackgroundLocationPermission.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            else -> proceedAfterConsent()
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

    /**
     * The usage terms were accepted and the background-permission step is done. If the GPS/network
     * status is already good, start the measurement immediately; otherwise switch this same screen
     * into its "waiting" state and start automatically once the status becomes good. Keeping it on
     * this full-screen activity (instead of returning to the home screen) means the home screen never
     * flashes behind an alert, and no user interaction is needed once the conditions are met.
     */
    private fun proceedAfterConsent() {
        if (signalStatusReady()) {
            startMeasurement()
        } else {
            showWaitingForStatus()
        }
    }

    private fun signalStatusReady(): Boolean =
        viewModel.isGpsQualitySufficientForSignalMeasurement() && viewModel.isMobileNetworkActive()

    private fun showWaitingForStatus() {
        waitingForStatus = true
        binding.title.text = getString(R.string.signal_measurement_not_possible_dialog_title)
        binding.content.text = getString(R.string.signal_measurement_not_possible_dialog_text)
        binding.scrollView.scrollTo(0, 0)
        // Only "Abort" is offered now; the measurement starts on its own once the status is good.
        binding.accept.visibility = View.GONE
        binding.decline.text = getString(R.string.text_button_abort)
    }

    private fun maybeStartWhenReady() {
        if (waitingForStatus && signalStatusReady()) {
            startMeasurement()
        }
    }

    private fun startMeasurement() {
        setResult(Activity.RESULT_OK)
        SignalMeasurementActivity.start(this)
        finish()
    }

    companion object {

        fun start(context: Context): Intent = Intent(context, SignalMeasurementTermsActivity::class.java)
    }
}