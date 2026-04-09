package at.specure.measurement.coverage.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

class CoverageTimer(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private var _job: Job? = null
    private var _duration: Duration = Duration.ZERO
    private var _action: () -> Unit = {}

    fun start(duration: Duration, action: () -> Unit) {
        cancel() // cancel any existing timer
        _duration = duration
        _action = action
        _job = scope.launch {
            delay(duration.inWholeMilliseconds)
            action()
        }
    }

    fun cancel() {
        _job?.cancel()
        _job = null
    }

    fun restart() {
        start(_duration, _action)
    }

    val isRunning: Boolean
        get() = _job?.isActive == true
}