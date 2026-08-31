package at.specure.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

/** Minimum satellites a constellation must report before its C/N0 is shown (5th-highest is used). */
private const val MIN_SATELLITES_PER_CONSTELLATION = 5

/**
 * [LocationSource] that is used to provide location changes using GPS Provider
 */
class GPSLocationSource(val context: Context) : LocationSource {

    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var listener: LocationSource.Listener? = null
    private val altitudeEnricher = AltitudeEnricher(context)
    private var _satellitesCount = 0

    @Volatile
    private var latestGnssSignals: List<GnssConstellationSignal>? = null

    override val satellitesCount: Int
        get() = _satellitesCount

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            var count = 0
            // Collect the baseband C/N0 of every satellite, grouped by constellation.
            val cn0ByConstellation = HashMap<Int, MutableList<Float>>()
            for (i in 0 until status.satelliteCount) {
                if (status.usedInFix(i)) {
                    count++
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && status.hasBasebandCn0DbHz(i)) {
                    cn0ByConstellation.getOrPut(status.getConstellationType(i)) { mutableListOf() }
                        .add(status.getBasebandCn0DbHz(i).toFloat())
                }
            }
            _satellitesCount = count
            latestGnssSignals = computeConstellationSignals(cn0ByConstellation)
        }
    }

    /**
     * For each constellation with at least [MIN_SATELLITES_PER_CONSTELLATION] satellites, takes the
     * 5th-highest baseband C/N0 as its representative value. Constellations whose representative value
     * rounds to 0 dB Hz are dropped; the result is sorted best (highest) first.
     */
    private fun computeConstellationSignals(
        cn0ByConstellation: Map<Int, List<Float>>
    ): List<GnssConstellationSignal> =
        cn0ByConstellation
            .filterValues { it.size >= MIN_SATELLITES_PER_CONSTELLATION }
            .map { (type, values) ->
                val fifthHighest = values.sortedDescending()[MIN_SATELLITES_PER_CONSTELLATION - 1]
                GnssConstellationSignal(constellationName(type), fifthHighest.roundToInt())
            }
            .filter { it.cn0DbHz > 0 }
            .sortedByDescending { it.cn0DbHz }

    private fun constellationName(type: Int): String = when (type) {
        GnssStatus.CONSTELLATION_GPS -> "GPS"
        GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
        GnssStatus.CONSTELLATION_GALILEO -> "Galileo"
        GnssStatus.CONSTELLATION_BEIDOU -> "BeiDou"
        GnssStatus.CONSTELLATION_QZSS -> "QZSS"
        GnssStatus.CONSTELLATION_SBAS -> "SBAS"
        GnssStatus.CONSTELLATION_IRNSS -> "NavIC"
        else -> "Unknown"
    }

    override val location: LocationInfo?
        get() = try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                null
            } else {
                val location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                location?.let {
                    altitudeEnricher.enrichBlocking(it)
                    LocationInfo(it, latestGnssSignals)
                }
            }
        } catch (ex: Exception) {
            if (ex is CancellationException) {
                throw ex
            }
            Timber.e(ex, "Failed to get last known network location")
            null
        }

    private val locationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {
            // Deliver once, with the orthometric (MSL) height already applied (native on Android 14+,
            // converted on older devices).
            altitudeEnricher.enrich(location) { finalLocation ->
                listener?.onLocationChanged(LocationInfo(finalLocation, latestGnssSignals))
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }

    @SuppressLint("MissingPermission")
    override fun start(listener: LocationSource.Listener) {
        try {
            this.listener = listener
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                throw Exception("No location permissions enabled")
            } else {
                manager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    LocationSource.MINIMUM_UPDATE_TIME_MS,
                    LocationSource.MINIMUM_DISTANCE_METERS,
                    locationListener
                )
                manager.registerGnssStatusCallback(gnssStatusCallback, null)
                Timber.d("GPS Location Source started")
            }
        } catch (ex: Exception) {
            if (ex is kotlinx.coroutines.CancellationException) {
                throw ex
            }
            Timber.e(ex, "Failed to register gps updates")
        }
    }

    override fun stop() {
        try {
            listener = null
            manager.removeUpdates(locationListener)
            manager.unregisterGnssStatusCallback(gnssStatusCallback)
        } catch (ex: Exception) {
            Timber.e(ex, "Failed to unregister gps updates")
        }
    }
}