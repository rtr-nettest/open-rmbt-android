package at.specure.location

interface LocationSource {

    val location: LocationInfo?

    fun start(listener: Listener): Boolean

    fun stop()

    interface Listener {

        fun onLocationChanged(info: LocationInfo?)
    }

    companion object {
        const val MINIMUM_UPDATE_TIME_MS: Long = 1000
        const val MINIMUM_DISTANCE_METERS: Float = 5f
    }
}