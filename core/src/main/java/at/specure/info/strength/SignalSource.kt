package at.specure.info.strength

enum class SignalSource(val stringValue: String) {

    CELL_INFO("CELL_INFO"),
    SIGNAL_STRENGTH_CHANGED("SIGNAL_STRENGTH_CHANGED"),
    NM_CELL_INFO("NM_CELL_INFO"),
    NOT_AVAILABLE("NOT_AVAILABLE");

    companion object {

        fun fromString(value: String): SignalSource {
            values().forEach {
                if (it.stringValue == value) return it
            }
            return NOT_AVAILABLE
        }
    }
}