package at.rmbt.client.control

interface MapEndpointProvider {

    /**
     * Map server host, example "myhost.com"
     */
    val host: String

    /**
     * Port that should be used for control server client
     */
    val port: Int

    /**
     * Route to the map server, example for value "RMBTMapServer",
     * "myhost.com/RMBTMapServer/endpoint" will be used for requests to MapServer
     */
    val route: String

    /**
     * Link suffix to obtain markers, example "MapServer/V2/tiles/markers"
     */
    val getMapMarkersUrl: String

    /**
     * Link suffix to obtain tiles for map screen, example "RMBTMapServer/tiles/{type}/{zoom}/{x}/{y}.png?map_options=all/download&statistical_method=0.5&period=180
     */
    val getMapTilesUrl: String

    /**
     * Url pattern for marker details data, should be opened via WebView. Example of link: https://controlServer/en/Opentest?O2582896c-1ec4-4826-bc4c-d8297d8ff490#noMMenu
     */
    val mapMarkerShowDetailsUrl: String

    /**
     * Link suffix to obtain map filters data, example "RMBTMapServer/tiles/info
     */
    val mapFilterInfoUrl: String

    /**
     * List of operators for map filters
     */
    val getNationalTableUrl: String

    val nettestHeader: String?
}