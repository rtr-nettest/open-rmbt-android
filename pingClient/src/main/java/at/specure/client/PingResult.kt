package at.specure.client

sealed class PingResult {

    open fun getRTTMillis(): Double? {
        return null
    }

    data class Success(val sequenceNumber: Int, val rttMillis: Double, val sentAtNanos: Long = 0L) : PingResult() {

        override fun getRTTMillis(): Double? {
            return rttMillis
        }

        override fun toString(): String {
            return "Success(sequenceNumber=$sequenceNumber, rttMillis=$rttMillis)"
        }
    }
    data class ServerError(val sequenceNumber: Int, val exception: PingServerException) : PingResult()
    data class Lost(val sequenceNumber: Int, val sentAtNanos: Long = 0L) : PingResult()
    data class ClientError(val sequenceNumber: Int, val exception: Exception) : PingResult()
}