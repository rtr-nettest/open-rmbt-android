package at.rtr.rmbt.android.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivitySignalMeasurementBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.map.wrapper.LatLngW
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.HomeViewModel
import at.specure.location.LocationInfo
import at.specure.location.LocationState
import at.specure.test.SignalMeasurementType
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.LocationSource
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng


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

        fun udpateLocation(location: LocationInfo) {
            val latestLocation = Location(location.provider)
            latestLocation.latitude = location.latitude
            latestLocation.longitude = location.longitude
            latestLocation.time = location.time
            latestLocation.accuracy = location.accuracy
            latestLocation.bearing = location.bearing
            latestLocation.bearingAccuracyDegrees = location.bearingAccuracy
            latestLocation.elapsedRealtimeNanos = location.elapsedRealtimeNanos
            latestLocation.speed = location.speed
            listener?.onLocationChanged(latestLocation)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_signal_measurement)
        binding.isActive = false
        binding.isPaused = false

        window.statusBarColor = android.graphics.Color.TRANSPARENT

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


//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            window.insetsController?.hide(WindowInsets.Type.statusBars())
//            window.insetsController?.hide(WindowInsets.Type.navigationBars())
//        } else {
//            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
//            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
//                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            window?.decorView?.systemUiVisibility = 0
//        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)

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
        viewModel.locationLiveData.listen(this) { location ->
            binding.textSource.text = location?.provider ?: " - "
            map?.let {
                location?.let { latestLocation ->
                    mapLocationListener.udpateLocation(
                        latestLocation
                    )
                }
            }
        }
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
    }

    override fun onStop() {
        super.onStop()
        viewModel.detach(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        map = null
    }

    private fun checkLocationAndSetCurrent() {
        if (!viewModel.state.locationChanged.get()) {
            viewModel.locationLiveData.listen(this) {
                if (it != null) {
                    with(LatLngW(it.latitude, it.longitude)) {
                        viewModel.state.cameraPositionLiveData.postValue(this)
                        viewModel.state.coordinatesLiveData.postValue(this)
                        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), viewModel.state.zoom))
                        viewModel.locationLiveData.removeObservers(this@SignalMeasurementActivity)
                    }
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
                    viewModel.locationLiveData.listen(this) { info ->
                        if (info != null) {
                            viewModel.locationLiveData.removeObservers(this)
                            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(info.latitude, info.longitude), 16F))
                        }
                    }
                }
            }
        }
    }

}