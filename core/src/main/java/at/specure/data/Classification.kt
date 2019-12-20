package at.specure.data

enum class Classification(val intValue: Int) {

    NONE(0),
    BAD(1),
    NORMAL(2),
    GOOD(3),
    EXCELLENT(4);

    companion object {

        fun fromValue(value: Int): Classification {
            values().forEach {
                if (it.intValue == value) {
                    return it
                }
            }
            throw IllegalArgumentException("Unknown classification value $value")
        }
    }
}