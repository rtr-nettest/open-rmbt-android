package at.specure.measurement.coverage.domain.validators

import at.specure.info.network.NetworkInfo

interface NetworkValidator {

    fun isNetworkToBeLogged(networkInfo: NetworkInfo?): Boolean

    fun isTheSameNetwork(newNetworkInfo: NetworkInfo?, previousNetworkInfo: NetworkInfo?): Boolean
}