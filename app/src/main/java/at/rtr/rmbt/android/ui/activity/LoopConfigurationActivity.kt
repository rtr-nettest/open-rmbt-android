package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityLoopConfigurationBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.dialog.Dialogs
import at.rtr.rmbt.android.ui.dialog.InputSettingDialog
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.onDone
import at.rtr.rmbt.android.util.onTextChanged
import at.rtr.rmbt.android.viewmodel.LoopConfigurationViewModel
import at.specure.measurement.MeasurementService

class LoopConfigurationActivity : BaseActivity(), InputSettingDialog.Callback {

    private lateinit var binding: ActivityLoopConfigurationBinding
    private val viewModel: LoopConfigurationViewModel by viewModelLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_loop_configuration)
        binding.state = viewModel.state
        window.changeStatusBarColor(ToolbarTheme.WHITE)

        binding.iconClose.setOnClickListener { finish() }

        binding.loopModeWaitingTime.frameLayoutRoot.setOnClickListener {
            InputSettingDialog.instance(
                getString(R.string.preferences_loop_mode_min_delay),
                binding.loopModeWaitingTime.value.toString(),
                requestCode = CODE_WAITING_TIME
            ).show(this)
        }

        binding.loopModeDistanceMeters.frameLayoutRoot.setOnClickListener {
            InputSettingDialog.instance(
                getString(R.string.preferences_loop_mode_max_movement),
                binding.loopModeDistanceMeters.value.toString(),
                requestCode = CODE_DISTANCE
            )
                .show(this)
        }

        binding.count.setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) checkNumber() }
        binding.count.onDone { checkNumber() }

        binding.count.onTextChanged {
            binding.accept.isEnabled = it.isNotBlank()
        }

        binding.accept.setOnClickListener {
            if (checkNumber()) {
                finishAffinity()
                MeasurementService.startTests(this)
                MeasurementActivity.start(this)
            }
        }
    }

    override fun onSelected(value: String, requestCode: Int) {
        when (requestCode) {
            CODE_WAITING_TIME -> {
                if (!viewModel.isWaitingTimeValid(value.toInt(), MIN_WAITING_TIME, MAX_WAITING_TIME)) {
                    Dialogs.show(
                        this,
                        getString(R.string.value_invalid),
                        String.format(getString(R.string.loop_mode_max_delay_invalid), MIN_WAITING_TIME, MAX_WAITING_TIME)
                    )
                }
            }
            CODE_DISTANCE -> {
                if (!viewModel.isDistanceValid(value.toInt(), MIN_DISTANCE, MAX_DISTANCE)) {
                    Dialogs.show(
                        this,
                        getString(R.string.value_invalid),
                        String.format(getString(R.string.loop_mode_max_movement_invalid), MIN_DISTANCE, MAX_DISTANCE)
                    )
                }
            }
        }
    }

    private fun checkNumber(): Boolean {
        if (!viewModel.isNumberValid(binding.count.text.toString().toInt(), MIN_COUNT, MAX_COUNT)) {
            Dialogs.show(
                this,
                getString(R.string.value_invalid),
                String.format(getString(R.string.loop_mode_max_delay_invalid), MIN_COUNT, MAX_COUNT)
            )
            return false
        }
        return true
    }

    companion object {

        private const val CODE_WAITING_TIME: Int = 1
        private const val CODE_DISTANCE: Int = 2

        private const val MIN_WAITING_TIME: Int = 15
        private const val MAX_WAITING_TIME: Int = 1440
        private const val MIN_DISTANCE: Int = 50
        private const val MAX_DISTANCE: Int = 10000
        private const val MIN_COUNT: Int = 1
        private const val MAX_COUNT: Int = 100

        fun start(context: Context) = context.startActivity(Intent(context, LoopConfigurationActivity::class.java))
    }
}
