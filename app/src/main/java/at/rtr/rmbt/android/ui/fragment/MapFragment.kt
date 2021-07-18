package at.rtr.rmbt.android.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import at.bluesource.choicesdk.core.MobileService
import at.bluesource.choicesdk.maps.common.*
import at.bluesource.choicesdk.maps.common.Map
import at.bluesource.choicesdk.maps.common.options.MarkerOptions
import at.bluesource.choicesdk.maps.common.options.TileOverlay
import at.rmbt.client.control.data.MapPresentationType
import at.rmbt.client.control.data.MapStyleType
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentMapWrapperBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.ShowWebViewActivity
import at.rtr.rmbt.android.ui.adapter.MapMarkerDetailsAdapter
import at.rtr.rmbt.android.ui.addTileOverlayPatched
import at.rtr.rmbt.android.ui.dialog.MapFiltersDialog
import at.rtr.rmbt.android.ui.dialog.MapLayersDialog
import at.rtr.rmbt.android.ui.dialog.MapSearchDialog
import at.rtr.rmbt.android.ui.loadMapFragment
import at.rtr.rmbt.android.util.*
import at.rtr.rmbt.android.viewmodel.MapViewModel
import at.specure.data.NetworkTypeCompat
import at.specure.data.ServerNetworkType
import at.specure.data.entity.MarkerMeasurementRecord
import at.specure.location.LocationState
import io.reactivex.rxjava3.disposables.Disposable
import timber.log.Timber
import kotlin.math.abs

const val START_ZOOM_LEVEL = 12f

private const val CODE_LAYERS_DIALOG = 1
private const val CODE_FILTERS_DIALOG = 2
private const val CODE_SEARCH_DIALOG = 3
private const val ANCHOR_U = 0.5f
private const val ANCHOR_V = 0.865f

// default map position and zoom when no location information is available
// focus to Austria based on boundary box 'AT': ('Austria', (9.47996951665, 46.4318173285, 16.9796667823, 49.0390742051))
// derived from Github/graydon/country-bounding-boxes.py
// extracted from http//www.naturalearthdata.com/download/110m/cultural/ne_110m_admin_0_countries.zip
// under public domain terms
private const val DEFAULT_LAT = (49.0390742051F + 46.4318173285F) / 2F
private const val DEFAULT_LONG = (16.9796667823F + 9.47996951665F) / 2F
private const val DEFAULT_ZOOM_LEVEL = 6F
private val DEFAULT_PRESENTATION_TYPE = MapPresentationType.AUTOMATIC

class MapFragment : BaseFragment(), MapMarkerDetailsAdapter.MarkerDetailsCallback,
    MapLayersDialog.Callback,
    MapFiltersDialog.Callback, MapSearchDialog.Callback {

    private val mapViewModel: MapViewModel by viewModelLazy()
    private val binding: FragmentMapWrapperBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_map_wrapper

    private var currentOverlay: TileOverlay? = null
    private var currentLocation: LatLng? = null
    private var currentMarker: Marker? = null
    private var visiblePosition: Int? = null
    private var snapHelper: SnapHelper? = null

    private var map : Map? = null
    private var loadMapDisposable : Disposable? = null

    private var adapter: MapMarkerDetailsAdapter = MapMarkerDetailsAdapter(this)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.state = mapViewModel.state

        mapViewModel.state.playServicesAvailable.set(checkServices())
        mapViewModel.obtainFilters()

        childFragmentManager.loadMapFragment(R.id.mapFrameLayout) {
            this.map = it
            onMapReady()
        }

        binding.fabLayers.setOnClickListener {
            MapLayersDialog.instance(
                this,
                CODE_LAYERS_DIALOG,
                mapViewModel.state.style.get()!!.ordinal,
                mapViewModel.state.type.get()!!.ordinal
            )
                .show(fragmentManager)
        }

        binding.fabFilters.setOnClickListener {
            MapFiltersDialog.instance(this, CODE_FILTERS_DIALOG).show(fragmentManager)
        }

        snapHelper = LinearSnapHelper().apply { attachToRecyclerView(binding.markerItems) }
        binding.markerItems.adapter = adapter
        binding.markerItems.itemAnimator?.changeDuration = 0

        binding.markerItems.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (abs(dx) > 0) {
                    drawCurrentMarker()
                }
            }
        })
        binding.fabSearch.setOnClickListener {
            showSearchDialog()
        }

        if (!mapViewModel.state.playServicesAvailable.get()) {
            binding.webMap.settings.javaScriptEnabled = true
            binding.webMap.loadUrl("https://www.netztest.at/en/Karte")
        }
    }

    override fun onStyleSelected(style: MapStyleType) {
        mapViewModel.state.style.set(style)
        updateMapStyle()
    }

    override fun onTypeSelected(type: MapPresentationType) {
        currentOverlay?.remove()
        mapViewModel.state.type.set(type)
        currentOverlay = mapViewModel.providerLiveData.value?.let {
            map?.addTileOverlayPatched(it)
        }
    }

    override fun onAddressResult(address: Address?) {
        if (address != null) {
            map?.moveCamera(CameraUpdateFactory.get().newLatLngZoom(LatLng(address.latitude, address.longitude), 8f))
        } else {
            Toast.makeText(
                activity,
                R.string.map_search_location_dialog_not_found,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkServices(): Boolean {
        return MobileService.GMS.isAvailable() || MobileService.HMS.isAvailable()
    }

    private fun onMapReady() {
        checkLocationAndSetCurrent()
        updateMapStyle()
        setTiles()
        updateLocationPermissionRelatedUi()

        setDefaultMapPosition()

        mapViewModel.markersLiveData.listen(this) {
            adapter.items = it as MutableList<MarkerMeasurementRecord>
            if (it.isNotEmpty()) {
                val latlng = LatLng(it.first().latitude, it.first().longitude)
                if (currentLocation != latlng) {
                    currentLocation = latlng
                    Timber.d("Position markersLiveData to : $latlng")
                    map?.animateCamera(CameraUpdateFactory.get().newLatLng(latlng))
                }
                binding.markerItems.visibility = View.VISIBLE
                binding.fabsGroup.visibility = View.GONE
                visiblePosition = 0
                drawMarker(it.first())
            } else {
                onCloseMarkerDetails()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateLocationPermissionRelatedUi()
        if (!checkServices()) {
            binding.fabsGroup.visibility = View.GONE
            binding.webMap.visibility = View.VISIBLE
            binding.playServicesAvailableUi.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loadMapDisposable?.dispose()
    }

    override fun onCloseMarkerDetails() {
        binding.markerItems.visibility = View.GONE
        currentMarker?.remove()
        currentMarker = null
//        adapter.items = mutableListOf()
        if (mapViewModel.state.playServicesAvailable.get()) {
            binding.fabsGroup?.visibility = View.VISIBLE
        }
    }

    override fun onMoreDetailsClicked(openTestUUID: String) {
        // example of link: https://dev.netztest.at/en/Opentest?O2582896c-1ec4-4826-bc4c-d8297d8ff490#noMMenu
        mapViewModel.prepareDetailsLink(openTestUUID).singleResult(this) {
            ShowWebViewActivity.start(requireContext(), it)
        }
    }

    override fun onFiltersUpdated() {
        currentOverlay?.remove()
        currentOverlay = mapViewModel.providerLiveData.value?.let {
            map?.addTileOverlayPatched(it)
        }
    }

    private fun setDefaultMapPosition() {
        Timber.d("Position default check to : ${mapViewModel.state.cameraPositionLiveData.value?.latitude} ${mapViewModel.state.cameraPositionLiveData.value?.longitude}")
        if (mapViewModel.state.cameraPositionLiveData.value == null || mapViewModel.state.cameraPositionLiveData.value?.latitude == 0.0 && mapViewModel.state.cameraPositionLiveData.value?.longitude == 0.0) {
            val defaultPosition = LatLng(DEFAULT_LAT.toDouble(), DEFAULT_LONG.toDouble())
            Timber.d("Position default to : ${defaultPosition.latitude} ${defaultPosition.longitude}")
            map?.animateCamera(CameraUpdateFactory.get().newLatLngZoom(defaultPosition, DEFAULT_ZOOM_LEVEL))
            mapViewModel.state.type.set(DEFAULT_PRESENTATION_TYPE)
        }
    }

    private fun drawMarker(record: MarkerMeasurementRecord) {
        if (record.networkTypeLabel != ServerNetworkType.TYPE_UNKNOWN.stringValue) {
            record.networkTypeLabel?.let {
                val icon = when (NetworkTypeCompat.fromString(it)) {
                    NetworkTypeCompat.TYPE_UNKNOWN -> R.drawable.ic_marker_empty
                    NetworkTypeCompat.TYPE_LAN -> R.drawable.ic_marker_ethernet
                    NetworkTypeCompat.TYPE_BROWSER -> R.drawable.ic_marker_browser
                    NetworkTypeCompat.TYPE_WLAN -> R.drawable.ic_marker_wifi
                    NetworkTypeCompat.TYPE_4G -> R.drawable.ic_marker_4g
                    NetworkTypeCompat.TYPE_3G -> R.drawable.ic_marker_3g
                    NetworkTypeCompat.TYPE_2G -> R.drawable.ic_marker_2g
                    NetworkTypeCompat.TYPE_5G -> R.drawable.ic_marker_5g
                    NetworkTypeCompat.TYPE_5G_NSA -> R.drawable.ic_marker_5g
                    NetworkTypeCompat.TYPE_5G_AVAILABLE -> R.drawable.ic_marker_4g
                }
                addMarkerWithIcon(icon)
            }
        } else { // empty pin to prevent crash
            addMarkerWithIcon(R.drawable.ic_marker_empty)
        }
    }

    private fun addMarkerWithIcon(@DrawableRes icon: Int) {
        currentLocation?.let { latlng ->
            if (currentMarker == null) {
                currentMarker = map?.addMarker(MarkerOptions.create()
                    .position(latlng)
                    .anchor(ANCHOR_U, ANCHOR_V)
                    .icon(BitmapDescriptorFactory.instance().fromResource(icon))
                )
            } else {
                currentMarker?.setIcon(BitmapDescriptorFactory.instance().fromResource(icon))
            }
        }
    }

    private fun updateMapStyle() {
        with(mapViewModel.state.style.get()) {
            when (this) {
                MapStyleType.HYBRID -> {
                    activity?.window?.changeStatusBarColor(ToolbarTheme.BLUE)
                }
                MapStyleType.SATELLITE -> {
                    activity?.window?.changeStatusBarColor(ToolbarTheme.BLUE)
                }
                else -> {
                    activity?.window?.changeStatusBarColor(ToolbarTheme.WHITE)
                }
            }
            map?.mapType = MapStyleType.STANDARD.type
        }
    }

    private fun setTiles() {
        val providerData = mapViewModel.providerLiveData.value

        if(providerData == null) {
            mapViewModel.providerLiveData.listen(this) {
                currentOverlay = mapViewModel.providerLiveData.value?.let {
                    map?.addTileOverlayPatched(it)
                }
            }
        } else {
            currentOverlay = mapViewModel.providerLiveData.value?.let {
                map?.addTileOverlayPatched(it)
            }
        }

        map?.setOnMapClickListener { latlng ->
            mapViewModel.state.locationChanged.set(true)
            mapViewModel.locationLiveData.removeObservers(this)
            mapViewModel.state.cameraPositionLiveData.postValue(latlng)
            onCloseMarkerDetails()
            if (isMarkersAvailable()) {
                mapViewModel.loadMarkers(
                    latlng.latitude,
                    latlng.longitude,
                    map?.cameraPosition?.zoom?.toInt() ?: 1
                )
            }
        }

        map?.setOnCameraMoveListener {
            mapViewModel.state.locationChanged.set(true)
            mapViewModel.locationLiveData.removeObservers(this)
            mapViewModel.state.cameraPositionLiveData.postValue(map?.cameraPosition?.target)
            if (map?.cameraPosition?.zoom != mapViewModel.state.zoom) {
                currentOverlay?.remove()
                currentOverlay = mapViewModel.providerLiveData.value?.let {
                    map?.addTileOverlayPatched(it)
                }
            }
            mapViewModel.state.zoom = map?.cameraPosition?.zoom ?: 1f
        }
    }

    private fun checkLocationAndSetCurrent() {
        if (!mapViewModel.state.locationChanged.get()) {
            mapViewModel.locationLiveData.listen(this) {
                if (it != null) {
                    with(LatLng(it.latitude, it.longitude)) {
                        mapViewModel.state.cameraPositionLiveData.postValue(this)
                        mapViewModel.state.coordinatesLiveData.postValue(this)
                        map?.moveCamera(CameraUpdateFactory.get().newLatLngZoom(this, mapViewModel.state.zoom))
                        mapViewModel.locationLiveData.removeObservers(this@MapFragment)
                    }
                }
            }
        } else {
            mapViewModel.state.cameraPositionLiveData.value?.let {
                map?.moveCamera(CameraUpdateFactory.get().newLatLngZoom(it, mapViewModel.state.zoom))
            }
            visiblePosition = RecyclerView.NO_POSITION
            onCloseMarkerDetails()
            if (isMarkersAvailable()) {
                mapViewModel.state.coordinatesLiveData.value?.let {
                    mapViewModel.loadMarkers(
                        it.latitude,
                        it.longitude,
                        map?.cameraPosition?.zoom?.toInt() ?: 1
                    )
                }
            }
        }
    }

    private fun updateLocationPermissionRelatedUi() {
        mapViewModel.locationStateLiveData.listen(this) { state ->
            activity?.applicationContext?.let {
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
                    try {
                        map?.getUiSettings()?.isMyLocationButtonEnabled = false //need to disable location button after isMyLocationEnabled is called
                    }catch (e : Exception) {
                        //TODO need to find out why null pointer is produced here
                    }
                }
            }

            binding.fabLocation.setOnClickListener {
                if (state == LocationState.ENABLED) {
                    mapViewModel.locationLiveData.listen(this) { info ->
                        if (info != null) {
                            mapViewModel.locationLiveData.removeObservers(this)
                            map?.animateCamera(CameraUpdateFactory.get().newLatLng(LatLng(info.latitude, info.longitude)))
                            Timber.d("Position locationLiveData to : ${info.latitude} ${info.longitude}")
                        }
                    }
                }
            }
        }
    }

    private fun drawCurrentMarker() {
        snapHelper?.findSnapView(binding.markerItems.layoutManager)?.let { view ->
            binding.markerItems.layoutManager?.getPosition(view)?.let {
                if (it >= 0) {
                    if (visiblePosition != it) {
                        visiblePosition = it
                        drawMarker(adapter.getItem(it))
                    }
                }
            }
        }
    }

    private fun isMarkersAvailable(): Boolean =
        mapViewModel.state.type.get() == MapPresentationType.POINTS ||
                (mapViewModel.state.type.get() == MapPresentationType.AUTOMATIC && map?.cameraPosition?.zoom ?: 1f >= 10)

    private fun showSearchDialog() {
        if (!Geocoder.isPresent()) {
            Toast.makeText(
                activity,
                R.string.map_search_location_not_supported,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        MapSearchDialog.instance(this, CODE_SEARCH_DIALOG).show(fragmentManager)
    }
}