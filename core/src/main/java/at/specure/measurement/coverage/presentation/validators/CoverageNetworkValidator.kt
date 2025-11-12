package at.specure.measurement.coverage.presentation.validators

import at.specure.info.TransportType
import at.specure.info.network.NetworkInfo
import at.specure.measurement.coverage.domain.validators.NetworkValidator
import javax.inject.Singleton

@Singleton
class CoverageNetworkValidator(): NetworkValidator {

    override fun isNetworkToBeLogged(networkInfo: NetworkInfo?): Boolean {
        if (networkInfo == null) {
            return true
        }
        return when(networkInfo.type) {
            TransportType.CELLULAR -> true
            else -> false
        }
    }

    override fun isTheSameNetwork(
        newNetworkInfo: NetworkInfo?,
        previousNetworkInfo: NetworkInfo?
    ): Boolean {
        if ((newNetworkInfo == null).and(previousNetworkInfo == null))   {
            return true
        }
        if ((newNetworkInfo == null).xor(previousNetworkInfo == null))   {
            return false
        }
        // TODO: check with RTR about condition to know what is the same network in case of coverage measurement
        return newNetworkInfo?.name == previousNetworkInfo?.name
    }

}