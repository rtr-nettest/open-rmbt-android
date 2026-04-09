package at.rtr.rmbt.android.map

import at.rtr.rmbt.android.map.wrapper.LatLngW
import com.google.android.gms.maps.model.LatLng

private const val DEFAULT_LAT: Double = (49.0390742051 + 46.4318173285) / 2.0
private const val DEFAULT_LONG: Double = (16.9796667823 + 9.47996951665) / 2.0
private const val DEFAULT_ZOOM_LEVEL = 6F

object DefaultLocation {

    val austriaLocationWrapped = LatLngW(DEFAULT_LAT, DEFAULT_LONG)
    val austriaLocation = LatLng(DEFAULT_LAT, DEFAULT_LONG)
    val austriaZoomLevel = DEFAULT_ZOOM_LEVEL

    val defaultMaximumZoomLevelForCoverage = 19f
}