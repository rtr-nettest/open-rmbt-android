package at.specure.util

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

/**
 * Emit the first value immediately, then ignore everything for timeoutMillis period, but remember the latest value during that period and emit it when the window ends.
 */
fun <T> Flow<T>.throttleLatest(timeoutMillis: Long): Flow<T> = channelFlow {
    var lastValue: T? = null
    var job: Job? = null
    var isThrottling = false

    collect { value ->
        if (!isThrottling) {
            // Emit immediately
            send(value)
            isThrottling = true

            job = launch {
                delay(timeoutMillis)
                lastValue?.let {
                    send(it)
                    lastValue = null
                }
                isThrottling = false
            }
        } else {
            // Store latest value during throttle window
            lastValue = value
        }
    }

    // When flow completes, emit last pending value
    lastValue?.let { send(it) }
}