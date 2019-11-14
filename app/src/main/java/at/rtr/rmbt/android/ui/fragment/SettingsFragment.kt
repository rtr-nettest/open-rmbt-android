package at.rtr.rmbt.android.ui.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import at.rtr.rmbt.android.BuildConfig
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentSettingsBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.dialog.Dialogs
import at.rtr.rmbt.android.ui.dialog.InputSettingDialog
import at.rtr.rmbt.android.ui.dialog.OpenGpsSettingDialog
import at.rtr.rmbt.android.ui.dialog.OpenLocationPermissionDialog
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.SettingsViewModel
import at.specure.location.LocationProviderState
import at.specure.util.copyToClipboard
import at.specure.util.openAppSettings
import timber.log.Timber

class SettingsFragment : BaseFragment() {

    private val settingsViewModel: SettingsViewModel by viewModelLazy()
    private val binding: FragmentSettingsBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_settings

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.state = settingsViewModel.state

        binding.loopModeWaitingTime.frameLayoutRoot.setOnClickListener {

            InputSettingDialog.instance(
                getString(R.string.preferences_loop_mode_min_delay),
                binding.loopModeWaitingTime.value.toString(), this,
                KEY_REQUEST_CODE_LOOP_MODE_WAITING_TIME
            )
                .show(activity)
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE)
        }

        binding.loopModeDistanceMeters.frameLayoutRoot.setOnClickListener {

            InputSettingDialog.instance(
                getString(R.string.preferences_loop_mode_max_movement),
                binding.loopModeDistanceMeters.value.toString(), this,
                KEY_REQUEST_CODE_LOOP_MODE_DISTANCE
            )
                .show(activity)
        }

        binding.clientUUID.frameLayoutRootKeyValue.setOnClickListener {
            val clientUUIDExists = !binding.clientUUID.value.isNullOrEmpty()
            if (clientUUIDExists) {
                context?.copyToClipboard(binding.clientUUID.value)
                Toast.makeText(context, R.string.about_client_uuid_copied, Toast.LENGTH_SHORT).show()
            }
        }

        settingsViewModel.state.clientUUID.liveData.listen(this) {
            binding.clientUUID.value = it
        }

        settingsViewModel.locationStateLiveData.listen(this) {
            settingsViewModel.state.isLocationEnabled.set(it)
            settingsViewModel.state.canManageLocationSettings.set(it == LocationProviderState.ENABLED)
            Timber.d("LocationStateLiveData Fragment  : $it")
        }

        binding.switchLocations.switchButton.isClickable = false
        binding.switchLocations.rootView.setOnClickListener {
            settingsViewModel.state.isLocationEnabled.get()?.let {
                when (it) {
                    LocationProviderState.ENABLED -> requireContext().openAppSettings()
                    LocationProviderState.DISABLED_APP -> OpenLocationPermissionDialog.instance().show(activity)
                    LocationProviderState.DISABLED_DEVICE -> OpenGpsSettingDialog.instance().show(activity)
                }
            }
        }

        binding.version.value = BuildConfig.VERSION_NAME
        binding.commitHash.value = BuildConfig.COMMIT_HASH
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {

            val value: String? = data?.extras?.getString(InputSettingDialog.KEY_VALUE)
            value?.let {
                when (requestCode) {
                    KEY_REQUEST_CODE_LOOP_MODE_WAITING_TIME -> {

                        if (!settingsViewModel.isLoopModeWaitingTimeValid(
                                value.toInt(),
                                MIN_LOOP_MODE_WAITING_TIME, MAX_LOOP_MODE_WAITING_TIME
                            )
                        ) {

                            activity?.let { it1 ->
                                Dialogs.show(
                                    it1, getString(R.string.value_invalid),
                                    String.format(
                                        getString(R.string.loop_mode_max_delay_invalid),
                                        MIN_LOOP_MODE_WAITING_TIME, MAX_LOOP_MODE_WAITING_TIME
                                    )
                                )
                            }
                        }
                    }
                    KEY_REQUEST_CODE_LOOP_MODE_DISTANCE -> {

                        if (!settingsViewModel.isLoopModeDistanceMetersValid(
                                value.toInt(),
                                MIN_LOOP_MODE_DISTANCE, MAX_LOOP_MODE_DISTANCE
                            )
                        ) {

                            activity?.let { it1 ->
                                Dialogs.show(
                                    it1, getString(R.string.value_invalid),
                                    String.format(
                                        getString(R.string.loop_mode_max_movement_invalid),
                                        MIN_LOOP_MODE_DISTANCE, MAX_LOOP_MODE_DISTANCE
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val KEY_REQUEST_CODE_LOOP_MODE_WAITING_TIME: Int = 1
        private const val KEY_REQUEST_CODE_LOOP_MODE_DISTANCE: Int = 2
        private const val MIN_LOOP_MODE_WAITING_TIME: Int = 15
        private const val MAX_LOOP_MODE_WAITING_TIME: Int = 1440
        private const val MIN_LOOP_MODE_DISTANCE: Int = 50
        private const val MAX_LOOP_MODE_DISTANCE: Int = 10000

        fun newInstance() = SettingsFragment()
    }
}