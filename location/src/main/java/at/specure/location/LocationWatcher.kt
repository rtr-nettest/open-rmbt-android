package at.specure.location

import android.content.Context
import androidx.lifecycle.LiveData
import java.util.Collections

class LocationWatcher private constructor(context: Context, sourceSet: Set<LocationSource>, private val dispatcher: LocationDispatcher) {

    private val monitor = Any()

    private val sources: Set<SourceObserver> = sourceSet.map { SourceObserver(it) }.toSet()
    private val listeners = Collections.synchronizedSet(mutableSetOf<Listener>())
    private val stateWatcher = LocationStateWatcher(context)

    private val stateListener = object : LocationStateWatcher.Listener {
        override fun onLocationStateChanged(state: LocationState?) {
            synchronized(monitor) {
                if (state == LocationState.ENABLED) {
                    if (listeners.isNotEmpty()) {
                        if (isStarted) {
                            sources.forEach {
                                dispatcher.onLocationInfoChanged(it.source, it.source.location)
                                it.attach()
                            }
                        } else {
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

    val liveData: LiveData<LocationInfo?> by lazy { LocationLiveData(this) }
    val stateLiveData: LiveData<LocationState?> by lazy {
        val liveData = LocationStateLiveData(stateWatcher)
        stateWatcher.updateLocationPermissions()
        liveData
    }

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

    // TODO call each time user changes location permissions
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

        fun attach(): Boolean = source.start(sourceListener)

        fun detach() = source.stop()
    }

    class Builder(private val context: Context) {

        private val sourceSet = mutableSetOf<LocationSource>()
        private var dispatcher: LocationDispatcher? = null

        fun addSource(source: LocationSource): Builder {
            sourceSet.add(source)
            return this
        }

        fun dispatcher(dispatcher: LocationDispatcher): Builder {
            this.dispatcher = dispatcher
            return this
        }

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