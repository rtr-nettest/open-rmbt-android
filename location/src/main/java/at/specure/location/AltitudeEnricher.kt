package at.specure.location

import android.content.Context
import android.location.Location
import androidx.core.location.LocationCompat
import androidx.core.location.altitude.AltitudeConverterCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Enriches raw [Location] fixes with an orthometric height (Mean Sea Level altitude) so downstream
 * [LocationInfo] can report MSL instead of the WGS84 ellipsoidal height from [Location.getAltitude].
 *
 *  - Android 14+ (API 34): GNSS fixes usually already carry a native MSL altitude
 *    ([Location.hasMslAltitude]/[Location.getMslAltitudeMeters], surfaced via [LocationCompat]);
 *    nothing to do in that case.
 *  - Otherwise: androidx.core's [AltitudeConverterCompat] derives MSL from the ellipsoidal height and
 *    the geoid, storing it back on the [Location] (readable through [LocationCompat]).
 *
 * The converter is backed by a Room database that refuses main-thread access, so every conversion -
 * including the synchronous [enrichBlocking] path used by the last-known-location getters - runs on
 * [Dispatchers.IO]. The geoid database is opened once up front ([warmUp]) so those blocking calls
 * resolve from a warm, in-memory cache rather than doing first-time disk work while the caller waits.
 */
class AltitudeEnricher(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** True once the geoid database has been opened, so [enrichBlocking] can convert quickly. */
    @Volatile
    private var warm = false

    init {
        scope.launch {
            // A dummy fix is enough to open the Room database and load the geoid params.
            if (runConverter(Location("warmup").apply { altitude = 0.0 })) {
                warm = true
            }
        }
    }

    /**
     * Enriches [location] with an MSL altitude when needed, then invokes [deliver] exactly once with the
     * (possibly mutated) location.
     *
     * When nothing has to be converted - the fix already carries a native MSL altitude (API 34+) or has
     * no ellipsoidal altitude at all - [deliver] runs synchronously on the caller's thread. Otherwise the
     * conversion runs on [Dispatchers.IO] and [deliver] is invoked there once it completes.
     *
     * Delivering exactly once, with MSL already applied, is deliberate: emitting the raw fix first and an
     * enriched copy afterwards would give the enriched copy the same timestamp/provider as the raw one,
     * which the downstream [LocationDispatcher]s discard as a duplicate - so the MSL value would never
     * reach observers.
     */
    fun enrich(location: Location, deliver: (Location) -> Unit) {
        if (LocationCompat.hasMslAltitude(location) || !location.hasAltitude()) {
            deliver(location)
            return
        }
        scope.launch {
            runConverter(location)
            deliver(location)
        }
    }

    /**
     * Synchronous counterpart of [enrich] for the last-known-location getters, which must return an
     * already-enriched [Location] rather than deliver it later. Adds an MSL altitude in place when the
     * fix lacks one but has an ellipsoidal altitude. Safe to call from the main thread: the (Room-based)
     * conversion is executed on [Dispatchers.IO] and the caller blocks only until the warm, cached
     * lookup returns. If the geoid database is not warm yet, the conversion is skipped rather than
     * blocking the caller on first-time disk work - the asynchronous [enrich] path fills the gap.
     */
    fun enrichBlocking(location: Location) {
        if (LocationCompat.hasMslAltitude(location) || !location.hasAltitude() || !warm) {
            return
        }
        runBlocking {
            withTimeoutOrNull(CONVERSION_TIMEOUT_MS) { runConverter(location) }
        }
    }

    private suspend fun runConverter(location: Location): Boolean = withContext(Dispatchers.IO) {
        try {
            AltitudeConverterCompat.addMslAltitudeToLocation(context, location)
            true
        } catch (e: Exception) {
            Timber.w(e, "MSL altitude conversion failed")
            false
        }
    }

    companion object {
        private const val CONVERSION_TIMEOUT_MS = 2000L
    }
}
