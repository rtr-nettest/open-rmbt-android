package at.specure.measurement.coverage.domain.models

/**
 * Raised when a coverage measurement could not be registered with the server within
 * [timeoutMinutes] minutes because there was no network connectivity the whole time. The app keeps
 * retrying registration while offline and only gives up (with this) once the time budget is
 * exhausted, so the UI can show a clear "no connectivity for N minutes" message instead of a generic
 * error and return to the start screen.
 */
class CoverageRegistrationTimeoutException(val timeoutMinutes: Int) :
    Exception("No network connectivity to register the coverage measurement for $timeoutMinutes minutes")
