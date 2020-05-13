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
    ABORTED
}