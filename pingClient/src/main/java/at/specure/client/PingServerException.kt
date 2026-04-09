package at.specure.client

sealed class PingServerException(message: String) : Exception(message) {

    object UnknownPingResponseHeaderExceptionPing :
        PingServerException("Server responded with unknown ping header") {
        private fun readResolve(): Any = UnknownPingResponseHeaderExceptionPing
    }

    object PingErrorResponseExceptionPing :
        PingServerException("Server returned explicit ping error response") {
        private fun readResolve(): Any = PingErrorResponseExceptionPing
    }
}