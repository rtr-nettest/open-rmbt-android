package at.specure.measurement.coverage.presentation.validators

import at.specure.measurement.coverage.domain.validators.NetworkValidator
import javax.inject.Singleton

@Singleton
class CoverageNetworkValidator(): NetworkValidator {
    override fun isMobileNetwork(): Boolean {
        return false // todo
    }

    override fun isTheSameNetwork(): Boolean {
        return false // todo
    }
}