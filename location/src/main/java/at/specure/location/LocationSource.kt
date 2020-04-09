package at.specure.location

/**
 * An interface of [LocationSource] that should be implemented to use with [LocationWatcher]
 */
interface LocationSource {

    /**
     * The latest available location
     */
    val location: LocationInfo?

    /**
     * Init and start polling location changes to [Listener]
     */
    fun start(listener: Listener)

    /**
     * Remove all listeners and stop polling location changes
     */
    fun stop()

    interface Listener {

        /**
         * Location info changed
         */
        fun onLocationChanged(info: LocationInfo?)
    }

    companion object {
        const val MINIMUM_UPDATE_TIME_MS: Long = 1000
        const val MINIMUM_DISTANCE_METERS: Float = 5f
    }
}