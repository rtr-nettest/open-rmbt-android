package at.specure.location

/**
 * Enum that describes state of location permissions for the application
 */
enum class LocationState {

    /**
     * All permissions are enabled
     */
    ENABLED,

    /**
     * Application permissions are disabled by used
     */
    DISABLED_APP,

    /**
     * Location is turned off for whole system
     */
    DISABLED_DEVICE
}