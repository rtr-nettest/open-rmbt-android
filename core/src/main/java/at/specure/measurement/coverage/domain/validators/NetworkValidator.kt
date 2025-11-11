package at.specure.measurement.coverage.domain.validators

interface NetworkValidator {

    fun isMobileNetwork(): Boolean

    fun isTheSameNetwork(): Boolean
}