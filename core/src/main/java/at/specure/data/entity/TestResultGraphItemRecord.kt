package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables

@Entity(
    tableName = Tables.TEST_RESULT_GRAPH_ITEM
)
data class TestResultGraphItemRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val testUUID: String,

    /**
     * time in milliseconds relative to start of the test
     */
    val time: Long,

    /**
     * total transferred bytes for type RESULT_GRAPH_ITEM_TYPE_DOWNLOAD and RESULT_GRAPH_ITEM_TYPE_UPLOAD,
     * ping value in milliseconds for type RESULT_GRAPH_ITEM_TYPE_PING,
     * dBm value for RESULT_GRAPH_ITEM_TYPE_SIGNAL
     */
    val value: Long,

    /**
     * type of the graph value
     */
    val type: Type,

    /**
     * it is graph values from local results?
     */
    val isLocal: Boolean,
) {

    enum class Type(val typeValue: Int) {
        DOWNLOAD(0),
        UPLOAD(1),
        PING(2),
        SIGNAL(3);

        companion object {

            fun fromValue(value: Int): Type {
                for (type in values()) {
                    if (type.typeValue == value) {
                        return type
                    }
                }

                throw IllegalArgumentException("Type not found for value $value")
            }
        }
    }
}
