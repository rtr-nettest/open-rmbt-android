package at.specure.location

interface LocationDispatcher {

    val latestLocation: LocationInfo?

    fun onLocationInfoChanged(source: LocationSource, location: LocationInfo?): Decision

    fun onPermissionsDisabled()

    data class Decision(val location: LocationInfo?, val publish: Boolean)
}