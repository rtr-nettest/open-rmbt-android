package at.specure.client

data class PingClientConfiguration(
    val host: String,
    val port: Int,
    val token: String,
    val protocolId: String,
    val pingIntervalMillis: Long,
    val pingTimeoutMillis: Long,
    val successResponseHeader: String,
    val errorResponseHeader: String,
)
