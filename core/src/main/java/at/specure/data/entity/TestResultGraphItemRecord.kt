package at.specure.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import at.specure.data.Columns
import at.specure.data.Tables

@Entity(tableName = Tables.TEST_RESULT_GRAPH_ITEM)
data class TestResultGraphItemRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ForeignKey(
        entity = TestResultRecord::class,
        parentColumns = [Columns.TEST_OPEN_UUID_PARENT_COLUMN],
        childColumns = ["testOpenUUID"],
        onDelete = ForeignKey.CASCADE
    )
    val testOpenUUID: String,

    /**
     * time in milliseconds relative to start of the test
     */
    val time: Long,

    /**
     * kilobits per seconds for type RESULT_GRAPH_ITEM_TYPE_DOWNLOAD and RESULT_GRAPH_ITEM_TYPE_UPLOAD,
     * ping value in milliseconds for type RESULT_GRAPH_ITEM_TYPE_PING,
     * dBm value for RESULT_GRAPH_ITEM_TYPE_SIGNAL
     */
    val value: Long,

    /**
     * type of the graph value
     */
    val type: Type
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
