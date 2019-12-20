package at.specure.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Columns
import at.specure.data.Tables
import org.json.JSONArray

@Entity(tableName = Tables.QOS_RESULT)
class QoSResultRecord(
    @PrimaryKey
    @ColumnInfo(name = Columns.TEST_UUID_PARENT_COLUMN)
    val uuid: String,

    val timeMillis: Long,

    val testToken: String,

    val results: JSONArray,

    var isSubmitted: Boolean = false
)