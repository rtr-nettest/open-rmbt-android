package at.rtr.rmbt.android.ui.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewpager.widget.PagerAdapter
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityLoopInstructionsBinding
import at.rtr.rmbt.android.databinding.ViewLoopModeInstructionBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.viewmodel.LoopConfigurationViewModel
import kotlin.math.max

class LoopInstructionsActivity : BaseActivity(), Callback {

    private lateinit var binding: ActivityLoopInstructionsBinding
    private val viewModel: LoopConfigurationViewModel by viewModelLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_loop_instructions)
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

        binding.title.text = getString(R.string.title_loop_instruction_1)

        binding.pager.adapter = InstructionsAdapter(this, this)
        binding.pager.currentItem = 0

        if (viewModel.shouldShowLoopModeTerms()) {
            // Display the instructions/terms pages and count it.
            viewModel.loopModeTermsDisplayed()
        } else {
            // The instructions have already been shown enough times since installation: skip them and
            // enable loop mode directly, still handling the (separate, once-only) permission steps.
            skipLoopInstructions()
        }
    }

    /**
     * Enables loop mode without showing the instruction/terms pages: shows only the background-location
     * permission rationale page if it still needs to be shown (its own once-only gate), otherwise
     * finishes right away through the normal final-accept path (notification + background permission).
     */
    private fun skipLoopInstructions() {
        if (viewModel.shouldAskForBackgroundPermission(this)) {
            binding.title.text = getString(R.string.title_loop_mode_background_permission)
            binding.pager.setCurrentItem(2, false)
        } else {
            onThirdPageAccepted()
        }
    }

    override fun onDeclined() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onFirstPageAccepted() {
        binding.title.text = getString(R.string.title_loop_instruction_2)
        binding.pager.setCurrentItem(1, true)
    }

    override fun onSecondPageAccepted() {
        if (viewModel.shouldAskForBackgroundPermission(this)) {
            binding.title.text = getString(R.string.title_loop_mode_background_permission)
            binding.pager.setCurrentItem(2, true)
        } else {
            onThirdPageAccepted()
        }
    }

    override fun onThirdPageAccepted() {
        setResult(Activity.RESULT_OK)
        if (isNeedToAskForNotificationPermission()) {
            checkNotificationPermission()
        } else {
            requestBackgroundPermissionThenFinish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATION) {
            requestBackgroundPermissionThenFinish()
        }
    }

    // Keep this instructions screen in the foreground until the background-location permission flow
    // has returned (on Android 11+ that flow is the system settings page), and only close it then.
    // Firing the request and finishing immediately (the previous behaviour) left the settings page
    // buried behind the loop measurement, so it only surfaced after the measurement was already over.
    private val requestBackgroundLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // Remember the outcome so a decline is not re-asked, but a later loss of a granted
            // permission is offered again on the next loop activation.
            viewModel.recordBackgroundPermissionResult(granted)
            finish()
        }

    private fun requestBackgroundPermissionThenFinish() {
        if (viewModel.shouldAskForBackgroundPermission(this)) {
            requestBackgroundLocationPermission.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            finish()
        }
    }

    private fun isNeedToAskForNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasNotificationPermission) {
                false
            } else {
                viewModel.shouldAskForNotificationPermission()
            }
        } else {
            false
        }
    }
    private fun checkNotificationPermission() {
        if (isNeedToAskForNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (viewModel.shouldAskForNotificationPermission()) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION
                )
                viewModel.notificationPermissionsWereAsked()
            }
        }
    }

    inner class InstructionsAdapter(context: Context, private val callback: Callback) : PagerAdapter() {

        private var items = mutableListOf(
            context.getString(R.string.text_loop_instruction_1),
            context.getString(R.string.text_loop_instruction_2)
        ).apply {
            if (viewModel.shouldAskForBackgroundPermission(context)) {
                add(context.getString(R.string.text_loop_mode_background_permission))
            }
        }

        override fun isViewFromObject(view: View, o: Any) = view == o
        override fun getCount() = items.size

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val binding = ViewLoopModeInstructionBinding.inflate(LayoutInflater.from(container.context))

            binding.content.text = items[position]

            // The third page (index 2) is the background-location permission page, and it only exists
            // when we actually ask. Its accept button forwards to the system settings screen rather
            // than granting anything, so label it "Continue" instead of the misleading "Accept".
            if (position == 2) {
                binding.accept.text = container.context.getString(R.string.text_button_continue)
            }

            binding.decline.setOnClickListener { callback.onDeclined() }
            binding.accept.setOnClickListener {
                when (position) {
                    0 -> callback.onFirstPageAccepted()
                    1 -> callback.onSecondPageAccepted()
                    else -> callback.onThirdPageAccepted()
                }
            }
            container.addView(binding.root)
            return binding.root
        }

        override fun destroyItem(container: ViewGroup, position: Int, o: Any) {
            container.removeView(o as View)
        }
    }

    companion object {
        fun start(context: Context): Intent = Intent(context, LoopInstructionsActivity::class.java)
        private const val REQUEST_CODE_NOTIFICATION = 2
    }
}

interface Callback {
    fun onDeclined()
    fun onFirstPageAccepted()
    fun onSecondPageAccepted()
    fun onThirdPageAccepted()
}