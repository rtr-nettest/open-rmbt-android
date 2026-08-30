package at.specure.location

import timber.log.Timber

/**
 * [LocationDispatcher] for the "combined-location" watcher used by the general app (start screen,
 * normal measurement). It combines the gps, fused and network sources and reports exactly one of
 * them, choosing by the following criteria (the reported [LocationInfo.provider] reflects the choice):
 *
 *  - "gps"     : accuracy below [GPS_MAX_ACCURACY_METERS] and age below [FRESH_MAX_AGE_NANOS]
 *                (highest preference)
 *  - "fused"   : not older than [FRESH_MAX_AGE_NANOS] and better accuracy than the network fix
 *  - "network" : final fallback, unless older than [NETWORK_MAX_AGE_NANOS] or accuracy worse than
 *                [NETWORK_MAX_ACCURACY_METERS]
 *
 * The dedicated GPS-only "gps-location" watcher (used by the signal measurement) does NOT use this
 * dispatcher - it keeps the default one and always reports "gps".
 */
class CombinedLocationDispatcher : LocationDispatcher {

    // Latest non-null location per source, so cross-source criteria can be evaluated on every update.
    private val latestBySource = mutableMapOf<LocationSource, LocationInfo>()
    private var lastPublished: LocationInfo? = null

    override fun latestLocation(sources: List<LocationSource>): LocationInfo? =
        chooseLocation(sources.mapNotNull { it.location })

    override fun onPermissionsDisabled() {
        latestBySource.clear()
        lastPublished = null
    }

    override fun onLocationInfoChanged(source: LocationSource, location: LocationInfo?): LocationDispatcher.Decision {
        if (location == null) latestBySource.remove(source) else latestBySource[source] = location

        val chosen = chooseLocation(latestBySource.values.toList())
        val changed = chosen?.time != lastPublished?.time || chosen?.provider != lastPublished?.provider
        Timber.d("Combined location: chosen provider=${chosen?.provider} accuracy=${chosen?.accuracy}")

        return when {
            chosen == null && lastPublished == null -> LocationDispatcher.Decision(null, false)
            changed -> {
                lastPublished = chosen
                LocationDispatcher.Decision(chosen, true)
            }
            else -> LocationDispatcher.Decision(null, false)
        }
    }

    private fun chooseLocation(locations: List<LocationInfo>): LocationInfo? {
        val gps = locations.firstOrNull { it.provider.equals(GPS, ignoreCase = true) }
        val fused = locations.firstOrNull { it.provider.equals(FUSED, ignoreCase = true) }
        val network = locations.firstOrNull { it.provider.equals(NETWORK, ignoreCase = true) }

        // 1) GPS, highest preference: accurate and fresh.
        if (gps != null && gps.hasAccuracy &&
            gps.accuracy < GPS_MAX_ACCURACY_METERS && gps.ageNanos < FRESH_MAX_AGE_NANOS
        ) {
            return gps
        }

        // 2) Fused: fresh and better accuracy than the (fallback) network fix.
        if (fused != null && fused.hasAccuracy && fused.ageNanos < FRESH_MAX_AGE_NANOS &&
            (network == null || !network.hasAccuracy || fused.accuracy < network.accuracy)
        ) {
            return fused
        }

        // 3) Network fallback: only if recent enough and not absurdly inaccurate.
        if (network != null && network.hasAccuracy &&
            network.ageNanos <= NETWORK_MAX_AGE_NANOS && network.accuracy <= NETWORK_MAX_ACCURACY_METERS
        ) {
            return network
        }

        return null
    }

    companion object {
        private const val GPS = "gps"
        private const val FUSED = "fused"
        private const val NETWORK = "network"

        private const val GPS_MAX_ACCURACY_METERS = 30f
        private const val NETWORK_MAX_ACCURACY_METERS = 10_000f
        private const val FRESH_MAX_AGE_NANOS = 15_000_000_000L    // 15 s (gps & fused)
        private const val NETWORK_MAX_AGE_NANOS = 60_000_000_000L  // 60 s (network fallback)
    }
}
