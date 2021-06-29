package at.specure.measurement

enum class MeasurementState {
    IDLE,
    INIT,
    PING,
    DOWNLOAD,
    UPLOAD,
    QOS,
    FINISH,
    ERROR,
    ABORTED,
    JITTER_AND_PACKET_LOSS
}