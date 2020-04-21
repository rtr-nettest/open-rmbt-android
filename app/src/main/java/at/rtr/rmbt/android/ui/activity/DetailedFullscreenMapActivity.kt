package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import at.rmbt.client.control.data.MapPresentationType
import at.rmbt.client.control.data.MapStyleType
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityDetailedFullscreenMapBinding
import at.rtr.rmbt.android.ui.dialog.MapLayersDialog
import at.rtr.rmbt.android.util.ToolbarTheme
import at.rtr.rmbt.android.util.changeStatusBarColor
import at.rtr.rmbt.android.util.iconFromVector
import at.specure.data.NetworkTypeCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class DetailedFullscreenMapActivity : BaseActivity(), OnMapReadyCallback, MapLayersDialog.Callback {

    private lateinit var binding: ActivityDetailedFullscreenMapBinding

    private lateinit var latLng: LatLng
    private lateinit var networkType: NetworkTypeCompat

    private var map: GoogleMap? = null
    private var currentMapStyle = MapStyleType.STANDARD

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

        binding.map.onCreate(savedInstanceState)
        binding.map.getMapAsync(this)

        binding.closeFab.setOnClickListener { finish() }
        binding.layersFab.setOnClickListener {
            MapLayersDialog.instance(activeStyle = currentMapStyle.ordinal).show(this)
        }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap
        with(latLng) {
            googleMap?.addCircle(
                CircleOptions()
                    .center(this)
                    .fillColor(ContextCompat.getColor(this@DetailedFullscreenMapActivity, R.color.map_circle_fill))
                    .strokeColor(ContextCompat.getColor(this@DetailedFullscreenMapActivity, R.color.map_circle_stroke))
                    .strokeWidth(STROKE_WIDTH)
                    .radius(CIRCLE_RADIUS)
            )

            val icon = when (networkType) {
                NetworkTypeCompat.TYPE_LAN -> R.drawable.ic_marker_wifi
                NetworkTypeCompat.TYPE_WLAN -> R.drawable.ic_marker_wifi
                NetworkTypeCompat.TYPE_4G -> R.drawable.ic_marker_4g
                NetworkTypeCompat.TYPE_3G -> R.drawable.ic_marker_3g
                NetworkTypeCompat.TYPE_2G -> R.drawable.ic_marker_2g
                NetworkTypeCompat.TYPE_5G -> throw IllegalArgumentException("Need to add 5G marker image for the map")
            }

            googleMap?.addMarker(MarkerOptions().position(this).anchor(ANCHOR_U, ANCHOR_V).iconFromVector(this@DetailedFullscreenMapActivity, icon))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(this, ZOOM_LEVEL))
            googleMap?.setOnMarkerClickListener { true }
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
        map?.mapType = when (style) {
            MapStyleType.HYBRID -> {
                window.changeStatusBarColor(ToolbarTheme.BLUE)
                GoogleMap.MAP_TYPE_HYBRID
            }
            MapStyleType.SATELLITE -> {
                window.changeStatusBarColor(ToolbarTheme.BLUE)
                GoogleMap.MAP_TYPE_SATELLITE
            }
            else -> {
                window.changeStatusBarColor(ToolbarTheme.WHITE)
                GoogleMap.MAP_TYPE_NORMAL
            }
        }
    }

    override fun onTypeSelected(type: MapPresentationType) {
        // not reachable
    }
}
