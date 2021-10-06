package at.rtr.rmbt.android.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
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
import at.rtr.rmbt.android.ui.decorator.DividerDecorator
import at.rtr.rmbt.android.ui.dialog.MapTimelineFilterDialog
import at.rtr.rmbt.android.ui.dialog.MapLayersDialog
import at.rtr.rmbt.android.util.listen
import at.rtr.rmbt.android.util.model.MapSearchResult
import at.rtr.rmbt.android.util.singleResult
import at.rtr.rmbt.android.viewmodel.MapViewModel
import at.rtr.rmbt.android.viewmodel.TechnologyFilter
import at.specure.util.isCoarseLocationPermitted
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mapbox.geojson.Feature
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.layers.Property.VISIBLE
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.roundToInt

const val START_ZOOM_LEVEL = 12f

private const val ANCHOR_U = 0.5f
private const val ANCHOR_V = 0.865f

// default map position and zoom when no location information is available
// focus to Norway: ('Austria', (69.38, 19.89, 3F))
// Could be derived from Github/graydon/country-bounding-boxes.py
// extracted from http//www.naturalearthdata.com/download/110m/cultural/ne_110m_admin_0_countries.zip
// under public domain terms
private const val DEFAULT_LAT = 69.38
private const val DEFAULT_LONG = 19.89
private const val DEFAULT_ZOOM_LEVEL = 3.1F
private val DEFAULT_PRESENTATION_TYPE = MapPresentationType.AUTOMATIC
private const val CODE_FILTERS_DIALOG = 2

class MapFragment : BaseFragment(), OnMapReadyCallback, MapMarkerDetailsAdapter.MarkerDetailsCallback,
    MapLayersDialog.Callback, MapTimelineFilterDialog.Callback {

    private val mapViewModel: MapViewModel by viewModelLazy()
    private val binding: FragmentMapBinding by bindingLazy()

    override val layoutResId = R.layout.fragment_map

    private var mapboxMap: MapboxMap? = null

    private var visiblePosition: Int? = null
    private var snapHelper: SnapHelper? = null

    private var adapter: MapMarkerDetailsAdapter = MapMarkerDetailsAdapter(this)
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
    private var searchJob: Job? = null

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.state = mapViewModel.state
        binding.adapter = mapViewModel.mapSearchResultAdapter
        mapViewModel.obtainFiltersFromServer()

        binding.map.onCreate(savedInstanceState)
        binding.map.getMapAsync(this)

        mapViewModel.providersSpinnerAdapter = ArrayAdapter(requireContext(), R.layout.item_provider)
        binding.providersSpinner.adapter = mapViewModel.providersSpinnerAdapter
        mapViewModel.providersLiveData.listen(this) {
            mapViewModel.providersSpinnerAdapter.clear()
            mapViewModel.providersSpinnerAdapter.addAll(it)
            mapViewModel.providersSpinnerAdapter.notifyDataSetChanged()
        }
        binding.providersSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                mapViewModel.setProvider(position)
                updateMapStyle()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        snapHelper = LinearSnapHelper().apply { attachToRecyclerView(binding.markerItems) }
        binding.markerItems.adapter = adapter
        binding.markerItems.itemAnimator?.changeDuration = 0

        binding.searchResultsRecyclerview?.addItemDecoration(DividerDecorator(requireContext()))

        binding.searchButton.setOnClickListener {
            setSearchActive(true)
        }
        binding.searchCancelButton.setOnClickListener {
            searchJob?.cancel()
            setSearchActive(false)
        }
        binding.searchInput.setOnTouchListener { _, _ ->
            setSearchActive(true)
            false
        }
        binding.searchInput.addTextChangedListener {
            onSearchInputEdit()
        }
        val technologyFilterList: List<TextView> = listOf(
            binding.filterTechAll,
            binding.filterTech2g,
            binding.filterTech3g,
            binding.filterTech4g,
            binding.filterTech5g
        )
        binding.filterTechAll.setOnClickListener {
            setTechnologySelected(technologyFilterList, it, TechnologyFilter.FILTER_ALL)
        }
        binding.filterTech2g.setOnClickListener {
            setTechnologySelected(technologyFilterList, it, TechnologyFilter.FILTER_2G)
        }
        binding.filterTech3g.setOnClickListener {
            setTechnologySelected(technologyFilterList, it, TechnologyFilter.FILTER_3G)
        }
        binding.filterTech4g.setOnClickListener {
            setTechnologySelected(technologyFilterList, it, TechnologyFilter.FILTER_4G)
        }
        binding.filterTech5g.setOnClickListener {
            setTechnologySelected(technologyFilterList, it, TechnologyFilter.FILTER_5G)
        }
        setTechnologySelected(technologyFilterList, binding.filterTechAll, TechnologyFilter.FILTER_ALL)

        binding.cardTimeline.setOnClickListener {
            MapTimelineFilterDialog.instance(this, CODE_FILTERS_DIALOG).show(fragmentManager)
        }
    }

    private fun setSearchActive(active: Boolean) {
        binding.cardFilters.visibility = if (active) View.GONE else View.VISIBLE
        binding.searchCancelButton.visibility = if (active) View.VISIBLE else View.GONE
        binding.searchButton.visibility = if (active) View.GONE else View.VISIBLE
        if (!active) {
            binding.searchInput.text.clear()
            mapViewModel.mapSearchResultAdapter.items.clear()
            binding.cardSearchResult?.visibility = View.GONE
        }
    }

    private fun setTechnologySelected(technologyFilterList: List<TextView>, selectedView: View, filterType: TechnologyFilter) {
        mapViewModel.setTechnologyFilter(filterType)
        technologyFilterList.forEach { view ->
            view.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.text_lightest_gray)
            view.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_black_transparency_60))
        }
        selectedView.backgroundTintList = ContextCompat.getColorStateList(requireContext(), filterType.colorId)
        (selectedView as TextView).setTextColor(ContextCompat.getColor(requireContext(), R.color.text_lightest_gray))
        updateMapStyle()
    }

    override fun onStyleSelected(style: MapStyleType) {
        mapViewModel.state.style.set(style)
        updateMapStyle()
    }

    override fun onTypeSelected(type: MapPresentationType) {
        mapViewModel.state.type.set(type)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(map: MapboxMap) {
        mapboxMap = map
        checkLocationAndSetCurrent()
        updateMapStyle()
        addOnMapClickListener()
        setTiles()
        map.uiSettings.isRotateGesturesEnabled = false
        if (this.context?.isCoarseLocationPermitted() == true) {
            // todo: show current location
        }
        updateLocationPermissionRelatedUi()

        setDefaultMapPosition()
    }

    private fun addOnMapClickListener() {
        mapboxMap?.addOnMapClickListener { latLng ->
            val currentLayerName = when {
                mapboxMap!!.cameraPosition.zoom <= 5 -> {
                    mapViewModel.currentLayers.find { it.startsWith("C") }
                }
                mapboxMap!!.cameraPosition.zoom <= 7.5 -> {
                    mapViewModel.currentLayers.find { it.startsWith("M") }
                }
                mapboxMap!!.cameraPosition.zoom <= 10 -> {
                    mapViewModel.currentLayers.find { it.startsWith("H10") }
                }
                mapboxMap!!.cameraPosition.zoom <= 12 -> {
                    mapViewModel.currentLayers.find { it.startsWith("H1") }
                }
                mapboxMap!!.cameraPosition.zoom <= 14 -> {
                    mapViewModel.currentLayers.find { it.startsWith("H01") }
                }
                else -> {
                    mapViewModel.currentLayers.find { it.startsWith("H001") }
                }
            }
            val features = mapboxMap!!.queryRenderedFeatures(
                mapboxMap!!.projection.toScreenLocation(latLng), currentLayerName)
            val feature = if (features.isNotEmpty()) features[0] else null
            val currentLayerPrefix = currentLayerName
                ?.removeRange(0, currentLayerName.indexOf('-') + 1)
            if (feature != null) {
                showBottomSheetPopup(feature, currentLayerPrefix)
            }
            true
        }
    }

    private fun showBottomSheetPopup(feature: Feature, currentLayerPrefix: String?) {
        val regionType = if (mapboxMap!!.cameraPosition.zoom <= 5) "County" else "Municipality"
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        bottomSheetDialog.setContentView(R.layout.bottomsheet_dialog_map_popup)
        bottomSheetDialog.findViewById<TextView>(R.id.regionType)?.text = regionType
        bottomSheetDialog.findViewById<TextView>(R.id.name)?.text =
            feature.getProperty("NAME")?.asString
        bottomSheetDialog.findViewById<TextView>(R.id.totalMeasurements)?.text =
            feature.getProperty("$currentLayerPrefix-COUNT")?.toString() ?: "0"
        bottomSheetDialog.findViewById<TextView>(R.id.averageDown)?.text = String.format(
            "%d Mbps", feature.getProperty("$currentLayerPrefix-DOWNLOAD")?.asDouble?.roundToInt() ?: 0)
        bottomSheetDialog.findViewById<TextView>(R.id.averageUp)?.text = String.format(
            "%d Mbps", feature.getProperty("$currentLayerPrefix-UPLOAD")?.asDouble?.roundToInt() ?: 0)
        bottomSheetDialog.findViewById<TextView>(R.id.averageLatency)?.text = String.format(
            "%d ms", feature.getProperty("$currentLayerPrefix-PING")?.asDouble?.roundToInt() ?: 0)
        bottomSheetDialog.findViewById<ImageButton>(R.id.closeButton)?.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }

    override fun onStart() {
        super.onStart()
        binding.map.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.map.onResume()
        updateLocationPermissionRelatedUi()
    }

    override fun onStop() {
        super.onStop()
        binding.map.onStop()
    }

    override fun onPause() {
        binding.map.onPause()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        try {
            binding.map.onSaveInstanceState(outState)
        } catch (exception: UninitializedPropertyAccessException) {
            Timber.e(exception.localizedMessage)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.map.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.map.onDestroy()
    }

    override fun onCloseMarkerDetails() {
        binding.markerItems.visibility = View.GONE
    }

    override fun onMoreDetailsClicked(openTestUUID: String) {
        // example of link: https://dev.netztest.at/en/Opentest?O2582896c-1ec4-4826-bc4c-d8297d8ff490#noMMenu
        mapViewModel.prepareDetailsLink(openTestUUID).singleResult(this) {
            ShowWebViewActivity.start(requireContext(), it)
        }
    }

    override fun onTimeFilterUpdated(filterYear: Int, filterMonth: Int) {
        Timber.d("Active timeline on map filter: $filterMonth $filterYear")
        mapViewModel.setTimeFilter(filterYear, filterMonth)
        updateMapStyle()
    }

    private fun setDefaultMapPosition() {
        Timber.d("Position default check to : ${mapViewModel.state.cameraPositionLiveData.value?.latitude} ${mapViewModel.state.cameraPositionLiveData.value?.longitude}")
        if (mapViewModel.state.cameraPositionLiveData.value == null || mapViewModel.state.cameraPositionLiveData.value?.latitude == 0.0 && mapViewModel.state.cameraPositionLiveData.value?.longitude == 0.0) {
            val defaultPosition = LatLng(DEFAULT_LAT.toDouble(), DEFAULT_LONG.toDouble())
            Timber.d("Position default to : ${defaultPosition.latitude} ${defaultPosition.longitude}")
            mapboxMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultPosition, DEFAULT_ZOOM_LEVEL.toDouble()))
            mapViewModel.state.type.set(DEFAULT_PRESENTATION_TYPE)
        }
    }

    private fun updateMapStyle() {
        val onStyleLoaded = Style.OnStyleLoaded {
            initializeStyles(it)
        }
        mapboxMap?.setStyle(Style.Builder().fromUri(mapViewModel.provideStyle()), onStyleLoaded)
    }

    private fun initializeStyles(style: Style) {
        mapViewModel.currentLayers.forEach {
            val layer = style.getLayer(it)
            layer?.let {
                layer.setProperties(visibility(VISIBLE))
            }
        }
    }

    private fun setTiles() {
        mapboxMap?.setOnMarkerClickListener { true }

        mapboxMap?.addOnCameraMoveStartedListener {
            mapViewModel.state.locationChanged.set(true)
            mapViewModel.locationLiveData.removeObservers(this)
        }
    }

    private fun checkLocationAndSetCurrent() {
        if (!mapViewModel.state.locationChanged.get()) {
            mapViewModel.locationLiveData.listen(this) {
                if (it != null) {
                    with(LatLng(it.latitude, it.longitude)) {
                        mapViewModel.state.cameraPositionLiveData.postValue(this)
                        mapViewModel.state.coordinatesLiveData.postValue(this)
                        mapboxMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(this, mapViewModel.state.zoom.toDouble()))
                        mapViewModel.locationLiveData.removeObservers(this@MapFragment)
                    }
                }
            }
        } else {
            mapViewModel.state.cameraPositionLiveData.value?.let {
                mapboxMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(it, mapViewModel.state.zoom.toDouble()))
                visiblePosition = RecyclerView.NO_POSITION
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
//                    mapboxMap?.locationComponent?.isLocationComponentEnabled = state == LocationState.ENABLED
                }
            }
        }
    }

    private fun onSearchResultSelect(item: MapSearchResult) {
        mapboxMap?.animateCamera(
            CameraUpdateFactory.newLatLngBounds(item.bounds, 0)
        )
    }

    private fun onSearchInputEdit() {
        val value: String = binding.searchInput.text.toString()
        if (value.isNotEmpty()) {
            searchJob?.cancel()
            searchJob = coroutineScope.launch {
                delay(500)
                mapViewModel.loadSearchResults(value, LatLng(DEFAULT_LAT, DEFAULT_LONG)) {
                    activity?.runOnUiThread {
                        binding.cardSearchResult?.visibility = View.VISIBLE
                        mapViewModel.mapSearchResultAdapter.items = it.toMutableList()
                        mapViewModel.mapSearchResultAdapter.actionCallback = { item ->
                            onSearchResultSelect(item)
                        }
                    }
                }
            }
        }
    }
}