package at.rtr.rmbt.android.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import at.specure.test.SignalMeasurementType
import at.specure.test.toLocation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.random.Random

const val DEFAULT_POSITION_TRACKING_ZOOM_LEVEL = 18f
const val DEFAULT_POINT_CLICKED_ZOOM_LEVEL = 16f

class SignalMeasurementActivity : BaseActivity(), OnMapReadyCallback {

    private val viewModel: HomeViewModel by viewModelLazy()
    private lateinit var binding: ActivitySignalMeasurementBinding
    private var map: GoogleMap? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_signal_measurement)
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
            showStopDialog()
        }

        binding.fabWarning.setOnClickListener {
            showLocationProblemDialog()
        }

        viewModel.dedicatedSignalMeasurementSessionIdLiveData.listen(this) { sessionId ->
            Timber.d("SessionId loaded: $sessionId")
            sessionId?.let {
                viewModel.loadSessionPoints(it)
            }
        }

        viewModel.currentSignalMeasurementMapPointsLiveData.listen(this) { points ->
            Timber.d("Points in activity: $points")
            map?.let { currentMap ->
                points.forEach { point ->
                    val latLng = point.location.toLatLng()
                    latLng?.let { markerLatLng ->
                        lifecycleScope.launch {
                            viewModel.getSignalData(point.signalRecordId).observe(this@SignalMeasurementActivity) { signalData ->
                                val options = MarkerOptions()
                                    .position(markerLatLng)
                                    .icon(BitmapDescriptorFactory.defaultMarker(Random.nextFloat() * 360))
                                    .title(signalData?.mobileNetworkType.toString() ?: getString(R.string.noSignal))

                                currentMap.addMarker(options)
                            }
                        }
                    }
                }
//                currentMap.setOnMarkerClickListener { marker ->
//                    marker.ti
//                }
            }
        }

        viewModel.locationLiveData.listen(this) { location ->
            Timber.d("New location obtained: $location")
            binding.textSource.text = "${location?.provider} ${location?.accuracy}m"
            if (viewModel.isLocationInfoMeetingQualityCriteria()) {
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
            map?.let { gMap ->
                location?.let { latestLocation ->
                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latestLocation.toLatLng(), DEFAULT_POSITION_TRACKING_ZOOM_LEVEL))
                }
            }
//            centerMapOnLocation()
        }
    }

    private fun showWarningButton() {
        binding.fabWarning.hide()
        binding.fabWarning.show()
    }

    private fun hideWarningButton() {
        binding.fabWarning.show()
        binding.fabWarning.hide()
    }

    private fun setFullscreen() {
        window.statusBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
            window.insetsController?.hide(WindowInsets.Type.navigationBars())
            window.insetsController?.hide(WindowInsets.Type.displayCutout())
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
    }

    override fun onStart() {
        super.onStart()
        viewModel.attach(this)
        lifecycleScope.launch {
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
                    map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), viewModel.state.zoom))
                }
            }
        } else {
            viewModel.state.cameraPositionLiveData.value?.let {
                map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), viewModel.state.zoom))
            }
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
        }
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
        val hideStopDialogJob = lifecycleScope.launch {
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
            finish()
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
