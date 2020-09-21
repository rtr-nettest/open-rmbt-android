package at.rtr.rmbt.android.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.core.content.ContextCompat.checkSelfPermission
import at.rmbt.client.control.IpProtocol
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentHomeBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.LoopConfigurationActivity
import at.rtr.rmbt.android.ui.activity.LoopInstructionsActivity
import at.rtr.rmbt.android.ui.activity.MeasurementActivity
import at.rtr.rmbt.android.ui.activity.PreferenceActivity
import at.rtr.rmbt.android.ui.activity.SignalMeasurementTermsActivity
import at.rtr.rmbt.android.ui.dialog.IpInfoDialog
import at.rtr.rmbt.android.ui.dialog.LocationInfoDialog
import at.rtr.rmbt.android.ui.dialog.MessageDialog
import at.rtr.rmbt.android.ui.dialog.NetworkInfoDialog
import at.rtr.rmbt.android.ui.dialog.OpenGpsSettingDialog
import at.rtr.rmbt.android.ui.dialog.OpenLocationPermissionDialog
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.util.InfoWindowStatus
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HomeViewModel
import at.specure.location.LocationState
import at.specure.measurement.MeasurementService
import at.specure.util.toast

class HomeFragment : BaseFragment() {

    private val homeViewModel: HomeViewModel by viewModelLazy()
    private val binding: FragmentHomeBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_home

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.state = homeViewModel.state
        updateTransparentStatusBarHeight(binding.statusBarStub)

        homeViewModel.isConnected.listen(this) {
            activity?.window?.changeStatusBarColor(if (it) ToolbarTheme.BLUE else ToolbarTheme.GRAY)
        }

        homeViewModel.signalStrengthLiveData.listen(this) {
            homeViewModel.state.signalStrength.set(it)
        }

        homeViewModel.activeNetworkLiveData.listen(this) {
            homeViewModel.state.activeNetworkInfo.set(it)
        }

        homeViewModel.locationStateLiveData.listen(this) {
            homeViewModel.state.isLocationEnabled.set(it)
        }

        homeViewModel.ipV4ChangeLiveData.listen(this) {
            homeViewModel.state.ipV4Info.set(it)
        }

        homeViewModel.ipV6ChangeLiveData.listen(this) {
            homeViewModel.state.ipV6Info.set(it)
        }

        binding.btnSetting.setOnClickListener {
            startActivity(Intent(requireContext(), PreferenceActivity::class.java))
        }
        binding.tvInfo.setOnClickListener {
            homeViewModel.state.infoWindowStatus.set(InfoWindowStatus.GONE)
        }

        binding.btnIpv4.setOnClickListener {
            IpInfoDialog.instance(IpProtocol.V4).show(activity)
        }

        binding.btnIpv6.setOnClickListener {
            IpInfoDialog.instance(IpProtocol.V6).show(activity)
        }

        binding.btnLocation.setOnClickListener {

            context?.let {
                homeViewModel.state.isLocationEnabled.get()?.let {
                    when (it) {
                        LocationState.ENABLED -> LocationInfoDialog.instance().show(activity)
                        LocationState.DISABLED_APP -> OpenLocationPermissionDialog.instance().show(activity)
                        LocationState.DISABLED_DEVICE -> OpenGpsSettingDialog.instance().show(activity)
                    }
                }
            }
        }

        binding.ivSignalLevel.setOnClickListener {
            if (homeViewModel.isConnected.value == true) {
                if (!homeViewModel.clientUUID.value.isNullOrEmpty()) {
                    if (homeViewModel.state.isLoopModeActive.get()) {
                        LoopConfigurationActivity.start(requireContext())
                    } else {
                        MeasurementService.startTests(requireContext())
                        MeasurementActivity.start(requireContext())
                    }
                } else {
                    MessageDialog.instance(R.string.client_not_registered).show(activity)
                }
            } else {
                MessageDialog.instance(R.string.home_no_internet_connection).show(activity)
            }
        }

        binding.btnUpload.setOnClickListener {
            homeViewModel.activeSignalMeasurementLiveData.value?.let { active ->
                if (!active) {
                    val intent = SignalMeasurementTermsActivity.start(requireContext())
                    startActivityForResult(intent, CODE_SIGNAL_MEASUREMENT_TERMS)
                } else {
                    homeViewModel.toggleSignalMeasurementService()
                }
            }
        }

        homeViewModel.activeSignalMeasurementLiveData.listen(this) {
            homeViewModel.state.isSignalMeasurementActive.set(it)
        }

        binding.btnLoop.setOnClickListener {
            if (this.isResumed) {
                if (binding.btnLoop.isChecked) {
                    val intent = LoopInstructionsActivity.start(requireContext())
                    startActivityForResult(intent, CODE_LOOP_INSTRUCTIONS)
                } else {
                    homeViewModel.state.isLoopModeActive.set(false)
                    binding.btnLoop.isChecked = false
                }
            }
        }

        homeViewModel.newsLiveData.listen(this) {
            it?.forEach { newItem ->
                val latestNewsShown: Long = homeViewModel.getLatestNewsShown() ?: -1
                newItem.text?.let { text ->
                    newItem.title?.let { title ->
                        if (newItem.uid ?: -1 > latestNewsShown) {
                            SimpleDialog.Builder()
                                .messageText(text)
                                .titleText(title)
                                .positiveText(android.R.string.ok)
                                .cancelable(false)
                                .show(this.childFragmentManager, CODE_DIALOG_NEWS)
                        }
                    }
                }
                homeViewModel.setNewsShown(newItem)
            }
        }

        binding.tvFrequency.setOnClickListener {
            if (homeViewModel.isExpertModeOn) {
                NetworkInfoDialog.show(childFragmentManager)
            }
        }

        binding.tvSignal.setOnClickListener {
            if (homeViewModel.isExpertModeOn) {
                NetworkInfoDialog.show(childFragmentManager)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.signalStrengthLiveData.listen(this) {
            homeViewModel.state.signalStrength.set(it)
        }

        homeViewModel.activeNetworkLiveData.listen(this) {
            homeViewModel.state.activeNetworkInfo.set(it)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            CODE_LOOP_INSTRUCTIONS -> {
                if (resultCode == Activity.RESULT_OK) {
                    homeViewModel.state.isLoopModeActive.set(true)
                    binding.btnLoop.isChecked = true
                } else {
                    homeViewModel.state.isLoopModeActive.set(false)
                    binding.btnLoop.isChecked = false
                }
            }
            CODE_SIGNAL_MEASUREMENT_TERMS -> {
                if (resultCode == Activity.RESULT_OK) {
                    homeViewModel.toggleSignalMeasurementService()
                    requireContext().toast(R.string.toast_signal_measurement_enabled)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        homeViewModel.permissionsWatcher.notifyPermissionsUpdated()
        homeViewModel.getNews() // displaying news after permissions were/were not granted
    }

    override fun onStart() {
        super.onStart()
        homeViewModel.attach(requireContext())

        checkPermissions()
        startTimerForInfoWindow()
        homeViewModel.state.checkConfig()
    }

    private fun checkPermissions() {
        val permissions = homeViewModel.permissionsWatcher.requiredPermissions.toMutableSet()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasForegroundLocationPermission =
                checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasForegroundLocationPermission) {
                val hasBackgroundLocationPermission = checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasBackgroundLocationPermission) {
                    permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            } else {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }

        if (permissions.isNotEmpty() && homeViewModel.shouldAskForPermission()) {
            requestPermissions(permissions.toTypedArray(), PERMISSIONS_REQUEST_CODE)
            homeViewModel.permissionsWereAsked()
        } else {
            homeViewModel.getNews()
        }
    }

    override fun onStop() {
        super.onStop()
        homeViewModel.detach(requireContext())
    }

    /**
     * If user not doing any action within 2 second, information window display
     */
    private fun startTimerForInfoWindow() {
        Handler().postDelayed({

            if (homeViewModel.state.infoWindowStatus.get() == InfoWindowStatus.NONE) {
                homeViewModel.state.infoWindowStatus.set(InfoWindowStatus.VISIBLE)
            }
        }, INFO_WINDOW_TIME_MS)
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE: Int = 10
        private const val INFO_WINDOW_TIME_MS: Long = 2000
        private const val CODE_SIGNAL_MEASUREMENT_TERMS = 12
        private const val CODE_LOOP_INSTRUCTIONS = 13
        private const val CODE_DIALOG_NEWS = 14
    }
}