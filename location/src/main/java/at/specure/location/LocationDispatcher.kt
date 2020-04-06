package at.specure.location

interface LocationDispatcher {

    fun latestLocation(sources: LocationSource): LocationInfo?

    fun onLocationInfoChanged(source: LocationSource, location: LocationInfo?): Decision

    data class Decision(val location: LocationInfo?, val publish: Boolean)
}