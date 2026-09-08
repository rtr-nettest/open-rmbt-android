package at.rtr.rmbt.android.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import kotlin.math.roundToInt

class SignalMeasurementTermsActivity : BaseActivity() {

    private lateinit var binding: ActivitySignalMeasurementTermsBinding
    private val viewModel: HomeViewModel by viewModelLazy()

    // Once true, the background-location permission info screen is being shown (second step). The
    // next "accept" then requests the permission and proceeds.
    private var backgroundInfoShown = false

    // Once true, consent is done and this screen is showing the "waiting for GPS/network" state. The
    // foreground service (started on consent) holds GPS and decides when to begin recording; this
    // screen only shows live status and hands off to the measurement UI once recording begins. The
    // only action offered is "Abort".
    private var waitingForStatus = false

    // True once we have bound (and started) the signal-measurement service, so onDestroy knows to
    // release our binding.
    private var serviceBound = false

    // Guards against handing off to the measurement screen more than once (the active observer can
    // fire again on re-resume).
    private var handedOff = false

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

        // "Decline" during the terms/background steps, "Abort" during the waiting step - both cancel
        // and return to the start screen. During waiting we must also stop the service we started.
        binding.decline.setOnClickListener {
            if (waitingForStatus && !handedOff) {
                viewModel.stopSignalMeasurement()
            }
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.accept.setOnClickListener { onTermsAccepted() }

        // Observe the GPS and network status live to keep the on-screen readiness rows current. The
        // actual GPS acquisition during waiting is owned by the foreground service (screen-off safe);
        // these observers are only for the visible status while this screen is in the foreground.
        viewModel.gpsLocationLiveData.listen(this) { updateReadinessStatus() }
        viewModel.locationStateLiveData.listen(this) { updateReadinessStatus() }
        viewModel.activeNetworkLiveData.listen(this) { updateReadinessStatus() }
        viewModel.signalStrengthLiveData.listen(this) { info ->
            // Keep the active-network info (used by isMobileNetworkActive) current on this screen's
            // own view model instance.
            viewModel.state.activeNetworkInfo.set(info?.copy())
            updateReadinessStatus()
        }

        // Once the service transitions from "preparing" to actually recording, hand off to the
        // measurement UI. LiveData re-delivers the latest value on resume, so if recording began while
        // the screen was off, the hand-off happens as soon as this screen comes back to the foreground.
        viewModel.activeSignalMeasurementLiveData.listen(this) { active ->
            if (active && waitingForStatus) handOffToMeasurement()
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
     * The usage terms were accepted and the background-permission step is done. Start the signal
     * measurement service (which owns GPS acquisition the robust, screen-off-safe way and waits until
     * GPS + mobile network are good before it begins recording) and show the live "waiting" status.
     * The hand-off to the measurement UI happens from the active-state observer once recording begins.
     */
    private fun proceedAfterConsent() {
        showWaitingForStatus()
        // Start the foreground service in its "preparing" phase. From here on the service - not this
        // screen - holds GPS and decides when conditions are good enough to begin recording, so the
        // wait survives the screen being turned off and there is no cold GPS restart at hand-off.
        viewModel.startSignalMeasurementService(this)
        serviceBound = true
    }

    private fun showWaitingForStatus() {
        waitingForStatus = true
        binding.title.text = getString(R.string.signal_measurement_not_possible_dialog_title)
        binding.content.text = getString(R.string.signal_measurement_not_possible_dialog_text)
        binding.statusContainer.visibility = View.VISIBLE
        binding.scrollView.scrollTo(0, 0)
        // Only "Abort" is offered now; the measurement begins on its own once conditions are good.
        binding.accept.visibility = View.GONE
        binding.decline.text = getString(R.string.text_button_abort)
        updateReadinessStatus()
    }

    /**
     * Updates the two live readiness rows (GPS accuracy, network type): a green check + green text
     * when the criterion is met, a red X + red text (with the actual vs. required value) when not.
     */
    private fun updateReadinessStatus() {
        if (!waitingForStatus) return

        // Three distinct GPS states, text and color derived from the SAME facts so they never disagree:
        //  - stale / no usable fix  -> "GPS: stale" (red)
        //  - fresh but too inaccurate -> the accuracy value vs. the limit (red)
        //  - fresh and within the limit -> "GPS: ok" (green)
        // The green case is exactly the GPS half of the start criterion.
        val threshold = viewModel.signalMeasurementAccuracyThresholdMeters
        val accuracy = viewModel.currentGpsAccuracyMeters()
        val gpsOk: Boolean
        val gpsText: String
        when {
            accuracy == null || !viewModel.isGpsFixFresh() -> {
                gpsOk = false
                gpsText = getString(R.string.signal_readiness_gps_stale)
            }
            accuracy.roundToInt() > threshold -> {
                gpsOk = false
                gpsText = getString(R.string.signal_readiness_gps_accuracy, accuracy.roundToInt(), threshold)
            }
            else -> {
                gpsOk = true
                gpsText = getString(R.string.signal_readiness_gps_ok)
            }
        }
        applyStatusRow(binding.gpsStatusText, gpsOk, gpsText)

        // Network type: WiFi (or no network) is not acceptable for a signal measurement.
        val networkOk = viewModel.isMobileNetworkActive()
        val networkName = viewModel.currentNetworkTypeName() ?: getString(R.string.noSignal)
        applyStatusRow(
            binding.networkStatusText,
            networkOk,
            getString(R.string.signal_readiness_network, networkName)
        )
    }

    private fun applyStatusRow(text: TextView, ok: Boolean, message: String) {
        // Icon as a start compound drawable (24dp intrinsic bounds), color matching the pass/fail state.
        text.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (ok) R.drawable.ic_status_ok else R.drawable.ic_status_fail, 0, 0, 0
        )
        text.text = message
        text.setTextColor(
            ContextCompat.getColor(
                this,
                if (ok) R.color.classification_green else R.color.classification_red
            )
        )
    }

    private fun handOffToMeasurement() {
        if (handedOff) return
        handedOff = true
        setResult(Activity.RESULT_OK)
        SignalMeasurementActivity.start(this)
        finish()
    }

    override fun onDestroy() {
        // Release our binding to the service. When handing off to the measurement screen the service
        // keeps running (it is a started foreground service), so unbinding here does not stop it; on
        // an abort it was already stopped in the decline handler.
        if (serviceBound) {
            viewModel.detach(this)
            serviceBound = false
        }
        super.onDestroy()
    }

    companion object {

        fun start(context: Context): Intent = Intent(context, SignalMeasurementTermsActivity::class.java)
    }
}