package at.specure

data class PingClientConfiguration(
    val host: String,
    val port: Int,
    val token: String,
    val protocolId: String,
    val pingIntervalMillis: Long,
    val pingTimeoutMillis: Long,
    val headerSizeBytes: Int,
    val successResponseHeader: String,
    val errorResponseHeader: String,
)
