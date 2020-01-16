package at.rtr.rmbt.android.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearSnapHelper
import at.rmbt.client.control.data.MapPresentationType
import at.rmbt.client.control.data.MapStyleType
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.FragmentMapBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.adapter.MapMarkerDetailsAdapter
import at.rtr.rmbt.android.ui.adapter.MarkerDetailsItemDecoration
import at.rtr.rmbt.android.ui.dialog.MapLayersDialog
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.viewmodel.MapViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.TileOverlay
import com.google.android.gms.maps.model.TileOverlayOptions

private const val CODE_LAYERS_DIALOG = 1

private const val START_ZOOM_LEVEL = 12f

class MapFragment : BaseFragment(), OnMapReadyCallback, MapMarkerDetailsAdapter.MarkerDetailsCallback, MapLayersDialog.Callback {

    private val mapViewModel: MapViewModel by viewModelLazy()
    private val binding: FragmentMapBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_map

    private var googleMap: GoogleMap? = null
    private var currentOverlay: TileOverlay? = null

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

        LinearSnapHelper().attachToRecyclerView(binding.markerItems)
        binding.markerItems.adapter = adapter
        binding.markerItems.addItemDecoration(MarkerDetailsItemDecoration(requireContext()))

        updateMapStyle()
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
        map?.let {
            with(map.uiSettings) {
                isRotateGesturesEnabled = false
            }
        }

        currentOverlay = googleMap?.addTileOverlay(TileOverlayOptions().tileProvider(mapViewModel.provider))
        googleMap?.setOnMapClickListener { latlng ->
            mapViewModel.state.coordinatesLiveData.postValue(latlng)
            mapViewModel.markersLiveData.listen(this) {
//                                adapter.submitList(it)
//                // todo display items
//                if (it.isNotEmpty()) {
//                    googleMap?.moveCamera(CameraUpdateFactory.newLatLng(latlng))
//                    binding.markerItems.visibility = View.VISIBLE
//                    binding.fabFilters.hide()
//                    binding.fabLocation.hide()
//                } else {
//                    onCloseMarkerDetails()
//                }
//                Timber.e("${it.size} items")
            }
            mapViewModel.loadMarkers(googleMap!!.cameraPosition.zoom.toInt())
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

    override fun onCloseMarkerDetails() {
        binding.markerItems.visibility = View.GONE
        binding.fabFilters.show()
        binding.fabLocation.show()
    }

    override fun onMoreDetailsClicked() {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}