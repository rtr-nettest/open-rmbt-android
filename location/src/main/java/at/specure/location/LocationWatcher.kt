package at.specure.location

import android.content.Context
import androidx.lifecycle.LiveData
import timber.log.Timber
import java.util.Collections

/**
 * Class that uses to observe location changes from different [LocationSource]
 */
class LocationWatcher private constructor(context: Context, sourceSet: Set<LocationSource>, private val dispatcher: LocationDispatcher) {

    private val monitor = Any()

    private val sources: List<SourceObserver> = sourceSet.map { SourceObserver(it) }
    private val listeners = Collections.synchronizedSet(mutableSetOf<Listener>())

    /**
     * [LocationStateWatcher] instance that uses to obtain location state changes
     */
    val stateWatcher = LocationStateWatcher(context)

    private val stateListener = object : LocationStateWatcher.Listener {
        override fun onLocationStateChanged(state: LocationState?) {
            synchronized(monitor) {
                if (state == LocationState.ENABLED) {
                    if (listeners.isNotEmpty()) {
                        if (isStarted) {
                            sources.forEach {
                                dispatcher.onLocationInfoChanged(it.source, it.source.location)
                            }
                        } else {
                            Timber.w("location update: on state changed start managers")
                            sources.forEach { it.attach() }
                            isStarted = true
                        }
                    }
                } else {
                    dispatcher.onPermissionsDisabled()
                    listeners.forEach { it.onLocationInfoChanged(null) }
                }
            }
        }
    }

    /**
     * LiveData that produces [LocationInfo] changes
     */
    val liveData: LiveData<LocationInfo?> by lazy { LocationLiveData(this) }

    /**
     * LiveData that produces [LocationState] changes
     */
    val stateLiveData: LiveData<LocationState?> by lazy {
        val liveData = LocationStateLiveData(stateWatcher)
        stateWatcher.updateLocationPermissions()
        liveData
    }

    /**
     * Returns the latest [LocationInfo] with best accuracy from the different sources
     */
    val latestLocation: LocationInfo?
        get() = dispatcher.latestLocation(sources.map { it.source })

    /**
     * Current [LocationState]
     */
    val state: LocationState?
        get() = stateWatcher.state

    private var isStarted = false

    /**
     * Add listener to listen for location info changes
     *
     * @property listener [Listener] to add
     */
    fun addListener(listener: Listener) {
        synchronized(monitor) {
            listeners.add(listener)
            if (listeners.size == 1) {
                if (stateWatcher.state == LocationState.ENABLED) {
                    Timber.w("location update: Add listener start managers")
                    sources.forEach { it.attach() }
                    isStarted = true
                }
                stateWatcher.addListener(stateListener)
            }
        }
    }

    /**
     * Remove listener from listening for location info changes
     *
     * @property listener [Listener] to remove
     */
    fun removeListener(listener: Listener) {
        synchronized(monitor) {
            listeners.remove(listener)
            if (listeners.isEmpty()) {
                if (isStarted) {
                    sources.forEach { it.detach() }
                    isStarted = false
                }
                stateWatcher.removeListener(stateListener)
            }
        }
    }

    /**
     * Should be called in base activity or fragment inside onRequestPermissionsResult for location permission changes
     * It is needed to track location app permission changes
     */
    fun updateLocationPermissions() {
        stateWatcher.updateLocationPermissions()
    }

    private fun onLocationInfoChanged(source: LocationSource, info: LocationInfo?) {
        val decision = dispatcher.onLocationInfoChanged(source, info)
        if (decision.publish) {
            synchronized(monitor) {
                listeners.forEach { it.onLocationInfoChanged(decision.location) }
            }
        }
    }

    private inner class SourceObserver(val source: LocationSource) {

        val sourceListener = object : LocationSource.Listener {

            override fun onLocationChanged(info: LocationInfo?) {
                onLocationInfoChanged(source, info)
            }
        }

        fun attach() = source.start(sourceListener)

        fun detach() = source.stop()
    }

    /**
     * Class used to create an instance of [LocationWatcher]
     */
    class Builder(private val context: Context) {

        private val sourceSet = mutableSetOf<LocationSource>()
        private var dispatcher: LocationDispatcher? = null

        /**
         * Add new [LocationSource] that will be used to obtain location changes
         */
        fun addSource(source: LocationSource): Builder {
            sourceSet.add(source)
            return this
        }

        /**
         * Add [LocationDispatcher] that will be used to make decisions about the best location from [LocationSource]
         * In dispatches is not set [DefaultLocationDispatcher] will be set
         */
        fun dispatcher(dispatcher: LocationDispatcher): Builder {
            this.dispatcher = dispatcher
            return this
        }

        /**
         * Builds an instance of [LocationWatcher]
         */
        fun build(): LocationWatcher {
            return LocationWatcher(context, sourceSet, dispatcher ?: DefaultLocationDispatcher())
        }
    }

    /**
     * Interface responsible for delivering location info changes
     */
    interface Listener {

        /**
         * Triggered whenever location info is changed
         *
         * @property LocationInfo android location object
         */
        fun onLocationInfoChanged(locationInfo: LocationInfo?)
    }
}