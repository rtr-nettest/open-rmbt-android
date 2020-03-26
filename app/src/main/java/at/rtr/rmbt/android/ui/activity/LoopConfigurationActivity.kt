package at.rtr.rmbt.android.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityLoopConfigurationBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.dialog.InputSettingDialog
import at.rtr.rmbt.android.ui.dialog.MessageDialog
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.util.onDone
import at.rtr.rmbt.android.util.onTextChanged
import at.rtr.rmbt.android.viewmodel.LoopConfigurationViewModel
import at.specure.measurement.MeasurementService
import timber.log.Timber

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
            binding.count.setSelection(binding.count.text?.length ?: 0)
            binding.accept.isEnabled = it.isNotBlank()
        }

        binding.accept.setOnClickListener {
            if (checkNumber()) {
                if (viewModel.isConnected.value == true) {
                    finish()
                    MeasurementService.startTests(this)
                    MeasurementActivity.start(this)
                } else {
                    MessageDialog.instance(R.string.home_no_internet_connection).show(this)
                }
            }
        }

        viewModel.isConnected.listen(this) {
            Timber.i("Has connection: $it")
        }

        checkBackgroundLocationPermission()
    }

    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasForegroundLocationPermission = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasForegroundLocationPermission) {
                val hasBackgroundLocationPermission = ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasBackgroundLocationPermission) {
                    // handle location update
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), REQUEST_CODE_BACKGROUND
                    )
                }
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ), REQUEST_CODE_BACKGROUND
                )
            }
        }
    }

    override fun onSelected(value: String, requestCode: Int) {
        when (requestCode) {
            CODE_WAITING_TIME -> {
                if (!viewModel.isWaitingTimeValid(
                        value.toInt(),
                        viewModel.config.loopModeMinWaitingTimeMin,
                        viewModel.config.loopModeMaxWaitingTimeMin
                    )
                ) {
                    SimpleDialog.Builder()
                        .messageText(
                            String.format(
                                getString(R.string.loop_mode_max_delay_invalid),
                                viewModel.config.loopModeMinWaitingTimeMin,
                                viewModel.config.loopModeMaxWaitingTimeMin
                            )
                        )
                        .positiveText(android.R.string.ok)
                        .cancelable(false)
                        .show(supportFragmentManager, CODE_DIALOG_INVALID)
                }
            }
            CODE_DISTANCE -> {
                if (!viewModel.isDistanceValid(
                        value.toInt(),
                        viewModel.config.loopModeMinDistanceMeters,
                        viewModel.config.loopModeMaxDistanceMeters
                    )
                ) {
                    SimpleDialog.Builder()
                        .messageText(
                            String.format(
                                getString(R.string.loop_mode_max_delay_invalid),
                                viewModel.config.loopModeMinDistanceMeters,
                                viewModel.config.loopModeMaxDistanceMeters
                            )
                        )
                        .positiveText(android.R.string.ok)
                        .cancelable(false)
                        .show(supportFragmentManager, CODE_DIALOG_INVALID)
                }
            }
        }
    }

    private fun checkNumber(): Boolean {
        if (!viewModel.isNumberValid(
                binding.count.text.toString().toInt(),
                viewModel.config.loopModeMinTestsNumber,
                viewModel.config.loopModeMaxTestsNumber
            )
        ) {
            SimpleDialog.Builder()
                .messageText(
                    String.format(
                        getString(R.string.loop_mode_max_delay_invalid),
                        viewModel.config.loopModeMinTestsNumber,
                        viewModel.config.loopModeMaxTestsNumber
                    )
                )
                .positiveText(android.R.string.ok)
                .cancelable(false)
                .show(supportFragmentManager, CODE_DIALOG_INVALID)
            return false
        }
        return true
    }

    companion object {

        private const val CODE_WAITING_TIME: Int = 1
        private const val CODE_DISTANCE: Int = 2
        private const val CODE_DIALOG_INVALID = 3
        private const val REQUEST_CODE_BACKGROUND = 1

        fun start(context: Context) = context.startActivity(Intent(context, LoopConfigurationActivity::class.java))
    }
}
