package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import at.bluesource.choicesdk.maps.common.*
import at.bluesource.choicesdk.maps.common.Map
import at.bluesource.choicesdk.maps.common.options.MarkerOptions
import at.bluesource.choicesdk.maps.common.shape.CircleOptions
import at.rmbt.client.control.data.MapPresentationType
import at.rmbt.client.control.data.MapStyleType
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityDetailedFullscreenMapBinding
import at.rtr.rmbt.android.ui.dialog.MapLayersDialog
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.specure.data.NetworkTypeCompat
import kotlinx.android.synthetic.main.fragment_map.*

class DetailedFullscreenMapActivity : BaseActivity(), MapLayersDialog.Callback {

    private lateinit var binding: ActivityDetailedFullscreenMapBinding

    private lateinit var latLng: LatLng
    private lateinit var networkType: NetworkTypeCompat

    private var currentMapStyle = MapStyleType.STANDARD

    private var map: Map? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_detailed_fullscreen_map)

        setTransparentStatusBar()
        window.changeStatusBarColor(ToolbarTheme.WHITE)

        if (!intent.hasExtra(KEY_LATITUDE) || !intent.hasExtra(KEY_LONGITUDE) || !intent.hasExtra(KEY_NETWORK_TYPE)) {
            throw IllegalArgumentException("Should start with latitude and network type")
        }
        latLng = LatLng(intent.getDoubleExtra(KEY_LATITUDE, 0.0), intent.getDoubleExtra(KEY_LONGITUDE, 0.0))
        networkType = NetworkTypeCompat.values()[intent.getIntExtra(KEY_NETWORK_TYPE, 0)]

        val mapFragment: MapFragment = MapFragment.newInstance()
        supportFragmentManager.beginTransaction().apply {
            add(R.id.mapContainer, mapFragment)
            commit()
        }

        mapFragment.getMapObservable().subscribe {
            onMapReady(it)
        }

        binding.closeFab.setOnClickListener { finish() }
        binding.layersFab.setOnClickListener {
            MapLayersDialog.instance(
                activeStyle = currentMapStyle.ordinal
            ).show(this)
        }
    }

    private fun onMapReady(map : Map) {
        this.map = map
        map.addCircle(CircleOptions()
            .center(latLng)
            .fillColor(ContextCompat.getColor(this@DetailedFullscreenMapActivity, R.color.map_circle_fill))
            .strokeColor(ContextCompat.getColor(this@DetailedFullscreenMapActivity, R.color.map_circle_stroke))
            .strokeWidth(STROKE_WIDTH)
            .radius(CIRCLE_RADIUS))

        val icon = when (networkType) {
            NetworkTypeCompat.TYPE_UNKNOWN -> R.drawable.ic_marker_empty
            NetworkTypeCompat.TYPE_LAN,
            NetworkTypeCompat.TYPE_BROWSER -> R.drawable.ic_marker_browser
            NetworkTypeCompat.TYPE_WLAN -> R.drawable.ic_marker_wifi
            NetworkTypeCompat.TYPE_5G_AVAILABLE,
            NetworkTypeCompat.TYPE_4G -> R.drawable.ic_marker_4g
            NetworkTypeCompat.TYPE_3G -> R.drawable.ic_marker_3g
            NetworkTypeCompat.TYPE_2G -> R.drawable.ic_marker_2g
            NetworkTypeCompat.TYPE_5G_NSA,
            NetworkTypeCompat.TYPE_5G -> R.drawable.ic_marker_5g
        }

        map.run {
            addMarker(MarkerOptions
                .create()
                .anchor(ANCHOR_U, ANCHOR_V)
                .icon(BitmapDescriptorFactory.instance().fromResource(icon))
                .position(latLng))

            moveCamera(CameraUpdateFactory
                .get()
                .newLatLngZoom(latLng, ZOOM_LEVEL)
            )
        }
    }

    companion object {

        private const val KEY_LATITUDE = "KEY_LATITUDE"
        private const val KEY_LONGITUDE = "KEY_LONGITUDE"
        private const val KEY_NETWORK_TYPE = "KEY_NETWORK_TYPE"

        private const val ZOOM_LEVEL = 15f
        private const val CIRCLE_RADIUS = 13.0
        private const val STROKE_WIDTH = 7f
        private const val ANCHOR_U = 0.5f
        private const val ANCHOR_V = 0.865f

        fun start(context: Context, latitude: Double, longitude: Double, networkType: NetworkTypeCompat) {
            val intent = Intent(context, DetailedFullscreenMapActivity::class.java).apply {
                putExtra(KEY_NETWORK_TYPE, networkType.ordinal)
                putExtra(KEY_LATITUDE, latitude)
                putExtra(KEY_LONGITUDE, longitude)
            }
            context.startActivity(intent)
        }
    }

    override fun onStyleSelected(style: MapStyleType) {
        currentMapStyle = style
        map?.mapType = style.type
        when (style) {
            MapStyleType.HYBRID -> {
                window.changeStatusBarColor(ToolbarTheme.BLUE)
            }
            MapStyleType.SATELLITE -> {
                window.changeStatusBarColor(ToolbarTheme.BLUE)
            }
            else -> {
                window.changeStatusBarColor(ToolbarTheme.WHITE)
            }
        }
    }

    override fun onTypeSelected(type: MapPresentationType) {
        // not reachable
    }
}
