package at.rtr.rmbt.android.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivitySignalMeasurementBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.map.wrapper.LatLngW
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HomeViewModel
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.test.DeviceInfo
import at.rmbt.client.control.data.SignalMeasurementType
import at.rtr.rmbt.android.ui.dialog.CoverageSettingsDialog
import at.rtr.rmbt.android.ui.dialog.Dialogs
import at.rtr.rmbt.android.util.formatAccuracy
import at.specure.info.network.NetworkInfo
import at.specure.measurement.coverage.domain.models.CoverageMeasurementData
import at.specure.measurement.coverage.domain.models.state.CoverageMeasurementState
import at.specure.measurement.coverage.presentation.validators.CoverageNetworkValidator
import at.specure.test.toLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineName
import timber.log.Timber
import kotlin.math.roundToInt
import at.rtr.rmbt.android.viewmodel.CoverageResultViewModel
import at.specure.measurement.coverage.data.getFrequencyBand
import at.specure.measurement.coverage.data.getSignalStrengthValue
import at.specure.test.toDeviceInfoLocation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

const val DEFAULT_POSITION_TRACKING_ZOOM_LEVEL = 16.2f
private const val DEFAULT_LAT: Double = ((49.0390742051F + 46.4318173285F) / 2F).toDouble()
private const val DEFAULT_LONG: Double = ((16.9796667823F + 9.47996951665F) / 2F).toDouble()
private const val DEFAULT_ZOOM_LEVEL = 6F
const val DEFAULT_POINT_CLICKED_ZOOM_LEVEL = 16f
const val DEFAULT_MAX_POINTS_TO_DRAW_ON_MAP = 1000

class SignalMeasurementActivity() : BaseActivity(), OnMapReadyCallback, CoverageSettingsDialog.Callback {

    val networkValidator = CoverageNetworkValidator()
    private val viewModel: HomeViewModel by viewModelLazy()
    private val coverageViewModel: CoverageResultViewModel by viewModelLazy()
    private lateinit var binding: ActivitySignalMeasurementBinding
    private var map: GoogleMap? = null
    private var warningSnackbar: Snackbar? = null

    private val mapLocationListener = object : LocationSource {
        private var listener: LocationSource.OnLocationChangedListener? = null

        override fun activate(onLocationChangedListener: LocationSource.OnLocationChangedListener) {
            listener = onLocationChangedListener
        }

        override fun deactivate() {
            listener = null
        }

        fun updateLocation(location: LocationInfo) {
            val latestLocation = location.toLocation()
            listener?.onLocationChanged(latestLocation)
        }
    }

    override fun onFenceOrAccuracyUpdated() {
        coverageViewModel.onCoverageConfigurationChanged()
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_signal_measurement)
        coverageViewModel.onConfigurationChanged()
        binding.isActive = false
        binding.isPaused = false

        setFullscreen()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (shouldAllowBackPress()) {
                    showStopDialog()
                }
            }
        })

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

        hideDialog()

        coverageViewModel.coverageMeasurementDataLiveData.listen(this) {
            if (it?.state == CoverageMeasurementState.FINISHED_LOOP_CORRECTLY) {
                showMeasurementResults(it)
            } else {
                updateUnfinishedMeasurement(it)
            }
        }

        viewModel.activeSignalMeasurementLiveData.listen(this) {
            binding.isActive = it
        }

        viewModel.pausedSignalMeasurementLiveData.listen(this) {
            binding.isPaused = it
        }

        binding.buttonStart.setOnClickListener {
            viewModel.startSignalMeasurement(SignalMeasurementType.DEDICATED)
        }

        binding.buttonStop.setOnClickListener {
            viewModel.stopSignalMeasurement()
        }

        binding.buttonPause.setOnClickListener {
            viewModel.pauseSignalMeasurement()
        }

        binding.buttonResume.setOnClickListener {
            viewModel.resumeSignalMeasurement()
        }

        binding.fabClose.setOnClickListener {
            if (coverageViewModel.coverageMeasurementDataLiveData.value?.state != CoverageMeasurementState.FINISHED_LOOP_CORRECTLY) {
                showStopDialog()
            } else {
                finish()
            }
        }

        binding.fabSettings.setOnClickListener {
            CoverageSettingsDialog.show(supportFragmentManager)
        }

        binding.fabWarning.setOnClickListener {
            showLocationProblemDialog()
        }

        viewModel.dedicatedSignalMeasurementSessionIdLiveData.listen(this) { sessionId ->
            Timber.d("SessionId loaded: $sessionId")
            coverageViewModel.onCoverageSessionLoaded(sessionId)

        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        coverageViewModel.onConfigurationChanged()
    }
    
    private fun showMeasurementResults(coverageMeasurementData: CoverageMeasurementData) {
        hideWarningButton()
        hideDialog()
        setMyPositionAndButtonVisible(false)
        setMyLocationButtonVisible(false)
        hideNetworkWarningSnackbar()
        setInfoVisible(false)
        setResultTitleVisible(true)
        setSettingsButtonVisible(false)
        coverageViewModel.clearPerformanceImprovementLists()
    }
    
    private fun updateUnfinishedMeasurement(coverageMeasurementData: CoverageMeasurementData?) {

        setSettingsButtonVisible(true)
        setMyLocationButtonVisible(true)
        checkNetwork(coverageMeasurementData?.currentNetworkInfo)
        setInfoVisible(true)
        setResultTitleVisible(false)
        updatePingValue(coverageMeasurementData)
        showCurrentNetworkType(coverageMeasurementData)
        showMeasurementError(coverageMeasurementData)

        // Launch a coroutine to safely update the map
        lifecycleScope.launch {
            map?.awaitMapLoad()

            setMyPositionAndButtonVisible(true)
            updateCurrentLocation(coverageMeasurementData?.currentLocation)
            coverageViewModel.updateMapPoints(
                map,
                coverageMeasurementData?.fences.toCoverageResultItemRecords()
            )
        }
    }

    private fun setSettingsButtonVisible(visible: Boolean) {
        binding.fabSettings.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    suspend fun GoogleMap.awaitMapLoad() = suspendCancellableCoroutine<Unit> { cont ->
        setOnMapLoadedCallback {
            cont.resume(Unit)
        }
    }

    private fun setMyPositionAndButtonVisible(visible: Boolean) {
        binding.fabLocation.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
        try {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                map?.isMyLocationEnabled = visible
            }
        } catch (e: Exception) {
         Timber.e("Unable to deactivate my location on the map  ${e.message}")
        }
    }

    private fun setMyLocationButtonVisible(visible: Boolean) {
        binding.fabLocation.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setResultTitleVisible(visible: Boolean) {
        binding.measurementResultTitle.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setInfoVisible(visible: Boolean) {
        binding.measurementProgressInfo.visibility = if (visible) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun showMeasurementError(coverageMeasurementData: CoverageMeasurementData?) {
        coverageMeasurementData?.signalMeasurementException?.also {
            Dialogs.show(this.applicationContext, getString(R.string.coverage_measurement_error_title), it.message ?: getString(R.string.coverage_measurement_error_unknown))
        }
    }

    private fun showCurrentNetworkType(coverageMeasurementData: CoverageMeasurementData?) {
        val networkType = coverageViewModel.getCurrentNetworkTypeName(coverageMeasurementData?.currentNetworkInfo)
        val frequencyBand = coverageMeasurementData?.currentNetworkInfo?.getFrequencyBand()
        val signal = coverageMeasurementData?.currentNetworkInfo?.getSignalStrengthValue()
        val networkStringRaw = listOfNotNull(networkType, frequencyBand, signal).joinToString(" | ")
        val networkString = networkStringRaw.ifEmpty {
            "-"
        }
        binding.technologyValue.text = networkString
    }

    private fun updatePingValue(coverageMeasurementData: CoverageMeasurementData?) {
        val pingResult = coverageMeasurementData?.currentPingMs
        binding.pingValue.text = if (pingResult != null && pingResult > 0) {
            val mantissa = pingResult - (pingResult.toInt().toDouble())
            if (mantissa > 0 && pingResult < 10.0) {
                this.getString(R.string.measurement_ping_value_1f, pingResult)
            } else {
                this.getString(R.string.measurement_ping_value, pingResult.roundToInt().toString())
            }
        } else {
            if (coverageMeasurementData?.currentPingStatus != null) {
                coverageMeasurementData.currentPingStatus
            } else {
                this.getString(R.string.measurement_dash)
            }
        }
    }
    
    private fun updateCurrentLocation(location: LocationInfo?) {
        Timber.d("New location obtained: $location")
        binding.textSource.text = "${location?.provider} ${location?.accuracy}m"
        if (coverageViewModel.isLocationInfoMeetingQualityCriteria(location.toDeviceInfoLocation())) {
            hideWarningButton()
            if (!viewModel.state.closeDialogDisplayed.get()) {
                hideDialog()
            }
        } else {
            if (!viewModel.state.closeDialogDisplayed.get()) {
                showWarningButton()
                showLocationProblemDialogIfNotSilenced()
            }
        }
        map?.let {
            location?.let { latestLocation ->
                mapLocationListener.updateLocation(
                    latestLocation
                )
            }
        }

        binding.accuracyValue.text = this.getString(R.string.location_dialog_accuracy, (location?.formatAccuracy() ?: "-").toString())

        map?.let { gMap ->
            location?.let { latestLocation ->
                if (!viewModel.state.markerDetailsDisplayed.get()) {
                    gMap.animateCamera(
                        CameraUpdateFactory.newLatLng(
                            latestLocation.toLatLng()
                        )
                    )
                }
            }
        }
//            centerMapOnLocation()
    }

    private fun checkNetwork(networkInfo: NetworkInfo?) {
        if (!networkValidator.isNetworkToBeLogged(networkInfo = networkInfo)) {
            val message = getString(R.string.wrong_network_message, coverageViewModel.getCurrentNetworkTypeName(networkInfo))
            showNetworkWarningIfNotSilenced(binding.root, message)
        } else {
            hideNetworkWarningSnackbar()
        }
    }

    fun showNetworkWarningIfNotSilenced(view: View, message: String) {
        if (!viewModel.state.networkWarningDialogSilenced.get()) {
            showNetworkWarningSnackbar(view, message)
        }
    }

    fun showNetworkWarningSnackbar(view: View, message: String) {
        if (warningSnackbar?.isShownOrQueued == true) {
            // Already visible or scheduled to show
            return
        }

        warningSnackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.snackbar_error_background))
            .setTextColor(ContextCompat.getColor(this, R.color.snackbar_error_text))
            .setAction(R.string.dismiss) {
                // User tapped the button → Snackbar disappears
                viewModel.silenceNetworkWarning()
            }
            .setActionTextColor(ContextCompat.getColor(this, R.color.snackbar_error_text))

        warningSnackbar?.show()
    }

    fun hideNetworkWarningSnackbar() {
        warningSnackbar?.dismiss()
    }

    private fun showWarningButton() {
        binding.fabWarning.hide()
        binding.fabWarning.show()
    }

    private fun hideWarningButton() {
        binding.fabWarning.show()
        binding.fabWarning.hide()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setFullscreen() {
        window.statusBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
            window.insetsController?.hide(WindowInsets.Type.navigationBars())
            window.insetsController?.hide(WindowInsets.Type.displayCutout())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
        }
    }

    private fun shouldAllowBackPress(): Boolean {
        return false
    }

    // Get a handle to the GoogleMap object and display marker.
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        checkLocationAndSetCurrent()
        updateLocationPermissionRelatedUi()
        map?.uiSettings?.isMyLocationButtonEnabled = false
        map?.uiSettings?.isMapToolbarEnabled = false
        map?.isIndoorEnabled = false
        map?.isBuildingsEnabled = false
    }

    override fun onStart() {
        super.onStart()
        viewModel.attach(this)
        lifecycleScope.launch(CoroutineName("Starting signal measurement")) {
            // TODO: maybe a little improve and instead of delay add state variable which listens on onServiceConnected in homeViewModel and start it there, but drawback is that we need to know when we want to continue there
            delay(1000)
            Timber.d("Starting signal measurement")
            viewModel.startSignalMeasurement(SignalMeasurementType.DEDICATED)
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.detach(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        map = null
    }

    @Deprecated("This is for android bellow 33 (android 12 and bellow)")
    override fun onBackPressed() {
        if (shouldAllowBackPress()) {
            super.onBackPressed()
        }
    }

    private fun checkLocationAndSetCurrent() {
        if (!viewModel.state.locationChanged.get()) {
            viewModel.locationLiveData.value?.let {
                with(LatLngW(it.latitude, it.longitude)) {
                    viewModel.state.cameraPositionLiveData.postValue(this)
                    viewModel.state.coordinatesLiveData.postValue(this)
                    map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), DEFAULT_POSITION_TRACKING_ZOOM_LEVEL))
                }
                return
            }
        } else {
            var latitude = DEFAULT_LAT
            var longitude = DEFAULT_LONG
            var zoomLevel = DEFAULT_ZOOM_LEVEL
            viewModel.state.cameraPositionLiveData.value?.let {
                latitude = it.latitude
                longitude = it.longitude
                if (latitude != 0.0 || longitude != 0.0) {
                    zoomLevel = DEFAULT_POSITION_TRACKING_ZOOM_LEVEL
                }
            }
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoomLevel))

//            visiblePosition = RecyclerView.NO_POSITION
//            onCloseMarkerDetails()
//            if (isMarkersAvailable()) {
//                viewModel.state.coordinatesLiveData.value?.let {
//                    viewModel.loadMarkers(
//                        it.latitude,
//                        it.longitude,
//                        mapW().currentCameraZoom().toInt()
//                    )
//                }
//            }
            return
        }
        val latitude = DEFAULT_LAT
        val longitude = DEFAULT_LONG
        val zoomLevel = DEFAULT_ZOOM_LEVEL
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoomLevel))
    }
    private fun showLocationProblemDialogIfNotSilenced() {
        if (!viewModel.state.locationWarningDialogSilenced.get()) {
            showLocationProblemDialog()
        }
    }


    private fun showLocationProblemDialog() {
        binding.warningMessageTitle.text =
            ContextCompat.getString(this, R.string.loop_mode_no_gps)
        binding.warningMessageContent.text =
            ContextCompat.getString(this, R.string.no_gps_or_insufficient_precision)
        binding.warningMessageAction.visibility = View.GONE
        binding.warningMessageCancel.text = ContextCompat.getString(this, R.string.confirm)

        binding.warningMessageCancel.setOnClickListener {
            viewModel.silenceLocationDialogWarning()
            hideDialog()
        }
        binding.warningMessage.visibility = View.VISIBLE
    }

    private fun showStopDialog() {
        val hideStopDialogJob = lifecycleScope.launch(CoroutineName("Hiding stop dialog")) {
            delay(5000L)
            hideDialog()
        }
        viewModel.setIsCloseDialogShown(true)
        binding.warningMessageTitle.text = ContextCompat.getString(this, R.string.stop_signal_measurement_title)
        binding.warningMessageContent.text = ContextCompat.getString(this, R.string.stop_signal_measurement_text)
        binding.warningMessageAction.text = ContextCompat.getString(this, R.string.text_stop_measurement)
        binding.warningMessageAction.visibility = View.VISIBLE
        binding.warningMessageCancel.text = ContextCompat.getString(this, R.string.text_continue_measurement)

        binding.warningMessageAction.setOnClickListener {
            hideStopDialogJob.cancel()
            viewModel.stopSignalMeasurement()
            hideDialog()
        }
        binding.warningMessageCancel.setOnClickListener {
            hideStopDialogJob.cancel()
            binding.warningMessage.visibility = View.GONE
        }
        binding.warningMessage.visibility = View.VISIBLE
    }

    private fun hideDialog() {
        viewModel.setIsCloseDialogShown(false)
        binding.warningMessage.visibility = View.GONE
    }

    private fun updateLocationPermissionRelatedUi() {
        viewModel.locationStateLiveData.listen(this) { state ->
            this.applicationContext?.let {
                if (ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                    == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                        it,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    map?.isMyLocationEnabled = state == LocationState.ENABLED
                }
            }

            binding.fabLocation.setOnClickListener {
                if (state == LocationState.ENABLED) {
                    viewModel.locationLiveData.value?.let { info ->
                        Timber.d("New location obtained in fabLocation: $info")
                        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(info.latitude, info.longitude), 16F))
                    }
                }
            }
        }
    }

    companion object {

        fun start(context: Context) = context.startActivity(Intent(context, SignalMeasurementActivity::class.java))
    }
}

private fun DeviceInfo.Location?.toLatLng(): LatLng? {
    this?.let {
        return LatLng(it.lat, it.long)
    }
    return null
}
