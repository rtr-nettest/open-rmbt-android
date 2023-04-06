package at.rtr.rmbt.android.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.checkSelfPermission
import at.rmbt.client.control.IpProtocol
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentHomeBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.*
import at.rtr.rmbt.android.ui.dialog.IpInfoDialog
import at.rtr.rmbt.android.ui.dialog.LocationInfoDialog
import at.rtr.rmbt.android.ui.dialog.OpenGpsSettingDialog
import at.rtr.rmbt.android.ui.dialog.OpenLocationPermissionDialog
import at.rtr.rmbt.android.ui.dialog.MessageDialog
import at.rtr.rmbt.android.ui.dialog.NetworkInfoDialog
import at.rtr.rmbt.android.ui.dialog.SimpleDialog
import at.rtr.rmbt.android.util.*
import at.rtr.rmbt.android.viewmodel.HomeViewModel
import at.specure.info.network.WifiNetworkInfo
import at.specure.location.LocationState
import at.specure.measurement.MeasurementService
import at.specure.util.hasPermission
import at.specure.util.openAppSettings
import at.specure.util.toast
import timber.log.Timber
import java.lang.IndexOutOfBoundsException

class HomeFragment : BaseFragment() {

    private val homeViewModel: HomeViewModel by viewModelLazy()
    private val binding: FragmentHomeBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_home

    private val getSignalMeasurementResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                homeViewModel.toggleSignalMeasurementService()
                requireContext().toast(R.string.toast_signal_measurement_enabled)
            }
        }

    private val getLoopModeInstructionsResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                homeViewModel.state.isLoopModeActive.set(true)
                binding.btnLoop.isChecked = true
            } else {
                homeViewModel.state.isLoopModeActive.set(false)
                binding.btnLoop.isChecked = false
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.state = homeViewModel.state

        homeViewModel.isConnected.listen(this) {
            activity?.window?.changeStatusBarColor(if (it) ToolbarTheme.BLUE else ToolbarTheme.GRAY)
        }

        homeViewModel.signalStrengthLiveData.listen(this) {
            homeViewModel.state.signalStrength.set(it?.signalStrengthInfo)
            homeViewModel.state.activeNetworkInfo.set(it)
            homeViewModel.state.secondary5GActiveNetworkInfo.set(
                if (it?.secondary5GActiveCellNetworks?.isNotEmpty() == true) {
                    it.secondary5GActiveCellNetworks?.get(0)
                } else {
                    null
                }
            )
            homeViewModel.state.secondary5GSignalStrength.set(
                if (it?.secondary5GActiveSignalStrengthInfos?.isNotEmpty() == true) {
                    it.secondary5GActiveSignalStrengthInfos?.get(0)
                } else {
                    null
                }
            )
            if (it?.networkInfo is WifiNetworkInfo) {
                (it.networkInfo as WifiNetworkInfo).signal = it.signalStrengthInfo?.value
            }
        }

        homeViewModel.locationStateLiveData.listen(this) {
            homeViewModel.state.isLocationEnabled.set(it)
            checkInformationAvailability()
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
                    getSignalMeasurementResult.launch(intent)
                } else {
                    homeViewModel.toggleSignalMeasurementService()
                }
            }
        }

        homeViewModel.activeSignalMeasurementLiveData.listen(this) {
            homeViewModel.state.isSignalMeasurementActive.set(it)
            checkInformationAvailability()
        }

        binding.btnLoop.setOnClickListener {
            if (this.isResumed) {
                if (binding.btnLoop.isChecked) {
                    val intent = LoopInstructionsActivity.start(requireContext())
                    getLoopModeInstructionsResult.launch(intent)
                } else {
                    homeViewModel.state.isLoopModeActive.set(false)
                    binding.btnLoop.isChecked = false
                }
            }
            checkInformationAvailability()
        }

        homeViewModel.newsLiveData.listen(this) {
            it?.forEach { newItem ->
                val latestNewsShown: Long = homeViewModel.getLatestNewsShown() ?: -1
                newItem.text?.let { text ->
                    newItem.title?.let { title ->
                        SimpleDialog.Builder()
                            .messageText(text)
                            .titleText(title)
                            .positiveText(android.R.string.ok)
                            .cancelable(false)
                            .show(this.childFragmentManager, CODE_DIALOG_NEWS)
                    }
                }
                homeViewModel.setNewsShown(newItem)
            }
        }

        binding.tvFrequency.setOnClickListener {
            if (homeViewModel.shouldDisplayNetworkDetails()) {
                NetworkInfoDialog.show(childFragmentManager)
            }
        }

        binding.tvSignal.setOnClickListener {
            if (homeViewModel.shouldDisplayNetworkDetails()) {
                NetworkInfoDialog.show(childFragmentManager)
            }
        }

        binding.panelPermissionsProblems.drawableHelp.setOnClickListener {
            PermissionsExplanationActivity.start(this)
        }

        homeViewModel.state.informationAccessProblem.addOnPropertyChanged { problem ->
            problem.get()?.let { updateProblemUI(it) }
        }
    }

    override fun onResume() {
        super.onResume()
        homeViewModel.signalStrengthLiveData.listen(this) {
            val networkInfo = it?.copy()
            homeViewModel.state.signalStrength.set(networkInfo?.signalStrengthInfo)
            homeViewModel.state.activeNetworkInfo.set(networkInfo)
            homeViewModel.state.secondary5GActiveNetworkInfo.set(
                try {
                    if (networkInfo?.secondary5GActiveCellNetworks?.isNotEmpty() == true) {
                        networkInfo.secondary5GActiveCellNetworks?.get(0)
                    } else {
                        null
                    }
                } catch (ex: IndexOutOfBoundsException) {
                    Timber.e(ex)
                    null
                }
            )
            homeViewModel.state.secondary5GSignalStrength.set(
                try{
                    if (networkInfo?.secondary5GActiveSignalStrengthInfos?.isNotEmpty() == true) {
                        networkInfo.secondary5GActiveSignalStrengthInfos?.get(0)
                    } else {
                        null
                    }
                } catch (ex: IndexOutOfBoundsException) {
                    Timber.e(ex)
                    null
                }
            )
        }
        checkInformationAvailability()
        homeViewModel.state.informationAccessProblem.get()?.let { updateProblemUI(it) }
    }

    private fun checkInformationAvailability() {
        val phonePermissionsGranted =
            context?.hasPermission(Manifest.permission.READ_PHONE_STATE) == true
        val locationPermissionsGranted =
            context?.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) == true || context?.hasPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == true
        val precisePermissionsGranted = context?.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) == true
        val backgroundLocationPermissionsGranted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context?.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == true
            } else {
                true
            }
        val problem = when {
            homeViewModel.state.isLocationEnabled.get() == LocationState.DISABLED_DEVICE -> {
                homeViewModel.state.informationAccessProblem.set(InformationAccessProblem.MISSING_LOCATION_ENABLED)
            }
            !locationPermissionsGranted -> {
                homeViewModel.state.informationAccessProblem.set(InformationAccessProblem.MISSING_LOCATION_PERMISSION)
            }
            !phonePermissionsGranted -> {
                homeViewModel.state.informationAccessProblem.set(InformationAccessProblem.MISSING_READ_PHONE_STATE_PERMISSION)
            }
            !precisePermissionsGranted -> {
                homeViewModel.state.informationAccessProblem.set(InformationAccessProblem.MISSING_PRECISE_LOCATION_PERMISSION)
                Timber.e("MISSING_PRECISE_LOCATION_PERMISSION")
            }
            (!backgroundLocationPermissionsGranted) && (homeViewModel.state.isLoopModeActive.get() || homeViewModel.activeSignalMeasurementLiveData.value == true) -> {
                homeViewModel.state.informationAccessProblem.set(
                    InformationAccessProblem.MISSING_BACKGROUND_LOCATION_PERMISSION
                )
            }
            else -> homeViewModel.state.informationAccessProblem.set(InformationAccessProblem.NO_PROBLEM)
        }
    }

    private fun updateProblemUI(problem: InformationAccessProblem) {
        if (this.isAdded && problem != InformationAccessProblem.NO_PROBLEM) {
            binding.panelPermissionsProblems.labelPermissionDisabled.text = problem.let {
                this.getText(
                    it.titleID
                )
            }
            binding.panelPermissionsProblems.textPermissionExplanation.text =
                problem.let {
                    this.getText(
                        it.descriptionId
                    )
                }
        }
        when (problem) {
            InformationAccessProblem.MISSING_LOCATION_ENABLED -> {
                binding.panelPermissionsProblems.cardPP.setOnClickListener {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }
            InformationAccessProblem.MISSING_LOCATION_PERMISSION,
            InformationAccessProblem.MISSING_READ_PHONE_STATE_PERMISSION,
            InformationAccessProblem.MISSING_PRECISE_LOCATION_PERMISSION,
            InformationAccessProblem.MISSING_BACKGROUND_LOCATION_PERMISSION -> {
                binding.panelPermissionsProblems.cardPP.setOnClickListener {
                    requireContext().openAppSettings()
                }
            }
            InformationAccessProblem.NO_PROBLEM -> {
                // do nothing
            }
        }
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
            } else {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPostNotificationPermission =
                checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!hasPostNotificationPermission) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissions.isNotEmpty() && homeViewModel.shouldAskForPermission()) {
            resultRequestPermissions.launch(permissions.toTypedArray())
            homeViewModel.permissionsWereAsked()
        } else {
            homeViewModel.getNews()
        }
    }

    private val resultRequestPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        homeViewModel.permissionsWatcher.notifyPermissionsUpdated()
        if (it.hasLocationPermissions()) {
            locationViewModel.updateLocationPermissions()
        }
        homeViewModel.getNews() // displaying news after permissions were/were not granted
    }

    override fun onStop() {
        super.onStop()
        homeViewModel.detach(requireContext())
    }

    /**
     * If user not doing any action within 2 second, information window display
     */
    private fun startTimerForInfoWindow() {
        Handler(Looper.getMainLooper()).postDelayed({

            if (homeViewModel.state.infoWindowStatus.get() == InfoWindowStatus.NONE) {
                homeViewModel.state.infoWindowStatus.set(InfoWindowStatus.VISIBLE)
            }
        }, INFO_WINDOW_TIME_MS)
    }

    companion object {
        private const val INFO_WINDOW_TIME_MS: Long = 2000
        private const val CODE_DIALOG_NEWS = 14
    }
}