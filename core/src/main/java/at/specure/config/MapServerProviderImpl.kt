package at.specure.config

import at.rmbt.client.control.MapEndpointProvider
import javax.inject.Inject

class MapServerProviderImpl @Inject constructor(private val config: Config) : MapEndpointProvider {

    // using without SSL is not permitted, server responds 301 code
    private val protocol = "https://"

    override val port: Int
        get() = config.mapServerPort

    override val host: String
        get() = config.mapServerHost

    override val route: String
        get() = config.mapServerRoute

    override val getMapMarkersUrl: String
        get() = "$protocol$host/$route/${config.mapMarkersEndpoint}"

    override val getMapTilesUrl: String
        get() = "$protocol$host/$route/${config.mapTilesEndpoint}"

    override val mapMarkerShowDetailsUrl: String
        get() = "$protocol$host/$route/${config.mapMarkerShowDetailsRoute}"

    override val mapFilterInfoUrl: String
        get() = "$protocol$host/$route/${config.mapFilterInfoEndpoint}"
}