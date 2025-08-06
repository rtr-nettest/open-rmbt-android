package at.specure

internal sealed class ServerResponse {
    data class Success(val sequenceNumber: Int, val rttMillis: Long) : ServerResponse()
    data class Error(val sequenceNumber: Int) : ServerResponse()
}