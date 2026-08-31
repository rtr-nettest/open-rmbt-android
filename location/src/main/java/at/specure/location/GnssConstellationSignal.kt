package at.specure.location

/**
 * Representative baseband carrier-to-noise density (C/N0, in dB-Hz) of a single GNSS constellation.
 *
 * The value is the 5th-highest per-satellite baseband C/N0 reading
 * ([android.location.GnssStatus.getBasebandCn0DbHz]) within that constellation. Constellations that
 * report fewer than 5 satellites are not represented at all. See [GPSLocationSource] for the
 * derivation.
 */
data class GnssConstellationSignal(
    val constellationName: String,
    val cn0DbHz: Int
)
