package at.specure.client

sealed class PingResult {

    open fun getRTTMillis(): Double? {
        return null
    }

    data class Success(val sequenceNumber: Int, val rttMillis: Double) : PingResult() {

        override fun getRTTMillis(): Double? {
            return rttMillis
        }

        override fun toString(): String {
            return "Success(sequenceNumber=$sequenceNumber, rttMillis=$rttMillis)"
        }
    }
    data class ServerError(val sequenceNumber: Int) : PingResult()
    data class Lost(val sequenceNumber: Int) : PingResult()
    data class ClientError(val sequenceNumber: Int, val exception: Exception) : PingResult()
}