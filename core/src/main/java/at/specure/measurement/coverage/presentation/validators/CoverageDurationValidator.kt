package at.specure.measurement.coverage.presentation.validators

import at.specure.measurement.coverage.domain.validators.DurationValidator
import timber.log.Timber
import javax.inject.Singleton

@Singleton
class CoverageDurationValidator(
    val minimalFenceDurationMillis: Long,
): DurationValidator {
    override fun isMinimalTimePassed(newTimestamp: Long?, lastTimestamp: Long?): Boolean {
        if (newTimestamp == null) {
            Timber.d("newTimestamp is null - not able to decide")
            return false
        }

        if (lastTimestamp == null) {
            Timber.d("lasrTimestamp is null - not able to decide")
            return true
        }

        val timeDifferenceMillis = newTimestamp - lastTimestamp
        val isValidTimeDifferenceMillis = timeDifferenceMillis >= minimalFenceDurationMillis
        Timber.d("Time Difference Millis: $timeDifferenceMillis, Is Valid: $isValidTimeDifferenceMillis")
        return isValidTimeDifferenceMillis
    }

}