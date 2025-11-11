package at.specure.measurement.coverage.presentation.validators

import at.specure.measurement.coverage.domain.validators.DurationValidator
import javax.inject.Singleton

@Singleton
class CoverageDurationValidator(
    val minimalFenceDurationMillis: Long,
): DurationValidator {
    override fun isMinimalTimePassed(newTimestamp: Long?, lastTimestamp: Long?): Boolean {
        if (newTimestamp == null) return false

        if (lastTimestamp == null) return true

        return newTimestamp - lastTimestamp >= minimalFenceDurationMillis
    }

}