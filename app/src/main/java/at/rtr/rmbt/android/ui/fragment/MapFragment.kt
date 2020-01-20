package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import at.rmbt.client.control.data.MapPresentationType
import at.rmbt.client.control.data.MapStyleType
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentMapBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.activity.ShowWebViewActivity
import at.rtr.rmbt.android.ui.adapter.MapMarkerDetailsAdapter
import at.rtr.rmbt.android.ui.dialog.MapLayersDialog
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.iconFromVector
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.util.singleResult
import at.rtr.rmbt.android.viewmodel.MapViewModel
import at.specure.data.NetworkTypeCompat
import at.specure.data.ServerNetworkType
import at.specure.data.entity.MarkerMeasurementRecord
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions
import timber.log.Timber
import kotlin.math.abs

private const val CODE_LAYERS_DIALOG = 1

private const val START_ZOOM_LEVEL = 12f

private const val ANCHOR_U = 0.5f
private const val ANCHOR_V = 0.865f

class MapFragment : BaseFragment(), OnMapReadyCallback, MapMarkerDetailsAdapter.MarkerDetailsCallback, MapLayersDialog.Callback {

    private val mapViewModel: MapViewModel by viewModelLazy()
    private val binding: FragmentMapBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_map

    private var googleMap: GoogleMap? = null
    private var currentOverlay: TileOverlay? = null
    private var currentLocation: LatLng? = null
    private var currentMarker: Marker? = null
    private var zoom: Float = START_ZOOM_LEVEL
    private var visiblePosition: Int? = null
    private var snapHelper: SnapHelper? = null

    private var adapter: MapMarkerDetailsAdapter = MapMarkerDetailsAdapter(this)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.state = mapViewModel.state

        activity?.window?.changeStatusBarColor(ToolbarTheme.WHITE)

        binding.map.onCreate(savedInstanceState)
        binding.map.getMapAsync(this)

        binding.fabLayers.setOnClickListener {
            MapLayersDialog.instance(this, CODE_LAYERS_DIALOG, mapViewModel.state.style.get()!!.ordinal, mapViewModel.state.type.get()!!.ordinal)
                .show(fragmentManager)
        }

        snapHelper = LinearSnapHelper().apply { attachToRecyclerView(binding.markerItems) }
        binding.markerItems.adapter = adapter
        binding.markerItems.itemAnimator?.changeDuration = 0

        binding.markerItems.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (abs(dx) > 0) {
                    snapHelper?.findSnapView(recyclerView.layoutManager)?.let { view ->
                        recyclerView.layoutManager?.getPosition(view)?.let {
                            if (it >= 0) {
                                if (visiblePosition != it) {
                                    visiblePosition = it
                                    drawMarker(adapter.getItem(it))
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onStyleSelected(style: MapStyleType) {
        mapViewModel.state.style.set(style)
        updateMapStyle()
    }

    override fun onTypeSelected(type: MapPresentationType) {
        currentOverlay?.remove()
        mapViewModel.state.type.set(type)
        currentOverlay = googleMap?.addTileOverlay(TileOverlayOptions().tileProvider(mapViewModel.provider))
    }

    override fun onMapReady(map: GoogleMap?) {
        googleMap = map
        updateMapStyle()
        setTiles()
        map?.let {
            with(map.uiSettings) {
                isRotateGesturesEnabled = false
            }
        }

        mapViewModel.markersLiveData.listen(this) {
            adapter.items = it as MutableList<MarkerMeasurementRecord>
            if (it.isNotEmpty()) {
                val latlng = LatLng(it.first().latitude, it.first().longitude)
                if (currentLocation != latlng) {
                    currentLocation = latlng
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLng(latlng))
                }
                binding.markerItems.visibility = View.VISIBLE
                binding.fabFilters.hide()
                binding.fabLocation.hide()
                visiblePosition = 0
                drawMarker(it.first())
            } else {
                onCloseMarkerDetails()
            }
        }

        mapViewModel.locationInfoLiveData.listen(this) {
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), START_ZOOM_LEVEL))
        }
    }

    override fun onStart() {
        super.onStart()
        binding.map.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
    }

    override fun onStop() {
        super.onStop()
        binding.map.onStop()
    }

    override fun onPause() {
        binding.map.onPause()
        super.onPause()
    }

    override fun onCloseMarkerDetails() {
        binding.markerItems.visibility = View.GONE
        binding.fabFilters.show()
        binding.fabLocation.show()
        currentMarker?.remove()
        currentMarker = null
    }

    override fun onMoreDetailsClicked(openTestUUID: String) {
        // example of link: https://dev.netztest.at/en/Opentest?O2582896c-1ec4-4826-bc4c-d8297d8ff490#noMMenu
        mapViewModel.prepareDetailsLink(openTestUUID).singleResult(this) {
            ShowWebViewActivity.start(requireContext(), it)
        }
    }

    private fun drawMarker(record: MarkerMeasurementRecord) {
        if (record.networkTypeLabel != ServerNetworkType.UNKNOWN_BLUETOOTH.stringValue) {
            record.networkTypeLabel?.let {
                val icon = when (NetworkTypeCompat.fromString(it)) {
                    NetworkTypeCompat.TYPE_WLAN -> R.drawable.ic_marker_wifi
                    NetworkTypeCompat.TYPE_4G -> R.drawable.ic_marker_4g
                    NetworkTypeCompat.TYPE_3G -> R.drawable.ic_marker_3g
                    NetworkTypeCompat.TYPE_2G -> R.drawable.ic_marker_2g
                    NetworkTypeCompat.TYPE_5G -> throw IllegalArgumentException("Need to add 5G marker image for the map")
                }

                currentLocation?.let { latlng ->
                    if (currentMarker == null) {
                        currentMarker = googleMap?.addMarker(
                            MarkerOptions().position(latlng).anchor(ANCHOR_U, ANCHOR_V).iconFromVector(requireContext(), icon)
                        )
                    } else {
                        currentMarker?.iconFromVector(requireContext(), icon)
                    }
                }
            }
        } else { // empty pin to prevent crash
            currentLocation?.let { latlng ->
                if (currentMarker == null) {
                    currentMarker = googleMap?.addMarker(
                        MarkerOptions().position(latlng).anchor(ANCHOR_U, ANCHOR_V).iconFromVector(requireContext(), R.drawable.ic_marker_empty)
                    )
                } else {
                    currentMarker?.iconFromVector(requireContext(), R.drawable.ic_marker_empty)
                }
            }
        }
    }

    private fun updateMapStyle() {
        with(mapViewModel.state.style.get()) {
            googleMap?.mapType = when (this) {
                MapStyleType.HYBRID -> {
                    activity?.window?.changeStatusBarColor(ToolbarTheme.BLUE)
                    GoogleMap.MAP_TYPE_HYBRID
                }
                MapStyleType.SATELLITE -> {
                    activity?.window?.changeStatusBarColor(ToolbarTheme.BLUE)
                    GoogleMap.MAP_TYPE_SATELLITE
                }
                else -> {
                    activity?.window?.changeStatusBarColor(ToolbarTheme.WHITE)
                    GoogleMap.MAP_TYPE_NORMAL
                }
            }
        }
    }

    private fun setTiles() {
        currentOverlay = googleMap?.addTileOverlay(TileOverlayOptions().tileProvider(mapViewModel.provider))
        googleMap?.setOnMapClickListener { latlng ->
            onCloseMarkerDetails()
            Timber.e("${latlng.latitude}   ${latlng.longitude}    ${googleMap!!.cameraPosition.zoom.toInt()} ")
            if (isMarkersAvailable()) {
                mapViewModel.loadMarkers(latlng.latitude, latlng.longitude, googleMap!!.cameraPosition.zoom.toInt())
            }
        }
        googleMap?.setOnMarkerClickListener { true }

        googleMap?.setOnCameraChangeListener {
            if (it.zoom != zoom) {
                zoom = it.zoom
                currentOverlay?.remove()
                currentOverlay = googleMap?.addTileOverlay(TileOverlayOptions().tileProvider(mapViewModel.provider))
            }
            mapViewModel.state.zoom = it.zoom
        }
    }

    private fun isMarkersAvailable(): Boolean =
        mapViewModel.state.type.get() == MapPresentationType.POINTS ||
                (mapViewModel.state.type.get() == MapPresentationType.AUTOMATIC && googleMap?.cameraPosition != null &&
                        googleMap?.cameraPosition!!.zoom >= 10)
}