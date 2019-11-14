package at.specure.location

interface LocationProviderStateWatcher {

    /**
     * Add listener to listen for location providing changes by system
     *
     * @property listener Listener to add
     */
    fun addListener(listener: LocationEnabledChangeListener)

    /**
     * Remove listener from listening for changes in location providing by the system
     *
     * @property listener Listener to remove
     */
    fun removeListener(listener: LocationEnabledChangeListener)

    /**
     * Returns true if there is location provider enabled by the system
     */
    fun isLocationEnabled(): Boolean

    /**
     * Interface to listen on changes in providing geolocation information from system
     */
    interface LocationEnabledChangeListener {

        /**
         * Triggered when user turn on or off location access in the device settings
         */
        fun onLocationStateChange(enabled: Boolean)
    }
}