package at.specure.location

import android.location.Location

interface LocationWatcherOld {

    /**
     * Add listener to listen for location info changes
     *
     * @property listener Listener to add
     */
    fun addLocationInfoListener(listener: LocationInfoChangeListener)

    /**
     * Remove listener from listening for location info changes
     *
     * @property listener Listener to remove
     */
    fun removeLocationInfoListener(listener: LocationInfoChangeListener)

    /**
     * Returns the most fresh location or null when it is not available
     */
    fun getLatestLocation(): Location?

    /**
     * Returns the most fresh location information or null when it is not available
     */
    fun getLatestLocationInfo(): LocationInfo?

    /**
     * Interface responsible for delivering location info changes
     */
    interface LocationInfoChangeListener {

        /**
         * Triggered whenever location info is changed
         *
         * @property LocationInfo android location object
         */
        fun onLocationInfoChanged(locationInfo: LocationInfo?)
    }

    companion object {
        const val MINIMUM_UPDATE_TIME_MS: Long = 1000
        const val MINIMUM_DISTANCE_METERS: Float = 5f
    }
}