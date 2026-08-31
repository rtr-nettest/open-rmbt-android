package at.specure.location

import android.content.Context
import android.location.Location
import androidx.core.location.LocationCompat
import androidx.core.location.altitude.AltitudeConverterCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Enriches raw [Location] fixes with an orthometric height (Mean Sea Level altitude) so downstream
 * [LocationInfo] can report MSL instead of the WGS84 ellipsoidal height from [Location.getAltitude].
 *
 *  - Android 14+ (API 34): GNSS fixes usually already carry a native MSL altitude
 *    ([Location.hasMslAltitude]/[Location.getMslAltitudeMeters], surfaced via [LocationCompat]);
 *    nothing to do in that case.
 *  - Otherwise: androidx.core's [AltitudeConverterCompat] derives MSL from the ellipsoidal height and
 *    the geoid, storing it back on the [Location] (readable through [LocationCompat]). The conversion
 *    is a blocking, potentially disk-loading call, so it runs off the main thread and the enriched fix
 *    is handed back via [onEnriched] after the original one has already been delivered.
 */
class AltitudeEnricher(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Adds an MSL altitude to [location] when it is missing one but has an ellipsoidal altitude to
     * convert. Runs off the main thread; [onEnriched] is invoked with the mutated [location] only when
     * an MSL altitude was actually produced. When the fix already carries an MSL altitude (native on
     * API 34+) or has no altitude at all, nothing happens and [onEnriched] is not called.
     */
    fun enrich(location: Location, onEnriched: (Location) -> Unit) {
        if (LocationCompat.hasMslAltitude(location) || !location.hasAltitude()) {
            return
        }
        scope.launch {
            try {
                AltitudeConverterCompat.addMslAltitudeToLocation(context, location)
                if (LocationCompat.hasMslAltitude(location)) {
                    onEnriched(location)
                }
            } catch (e: Exception) {
                Timber.w(e, "MSL altitude conversion failed")
            }
        }
    }
}
