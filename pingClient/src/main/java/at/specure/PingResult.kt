package at.specure

sealed class PingResult {
    data class Success(val sequenceNumber: Int, val rttMillis: Long) : PingResult()
    data class ServerError(val sequenceNumber: Int) : PingResult()
    data class Lost(val sequenceNumber: Int) : PingResult()
    data class ClientError(val sequenceNumber: Int, val exception: Exception) : PingResult()
}