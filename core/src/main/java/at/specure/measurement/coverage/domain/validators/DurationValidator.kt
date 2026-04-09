package at.specure.measurement.coverage.domain.validators

interface DurationValidator {

    fun isMinimalTimePassed(newTimestamp: Long?, lastTimestamp: Long?): Boolean

}