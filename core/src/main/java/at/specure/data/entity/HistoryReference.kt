package at.specure.data.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables

@Keep
@Entity(tableName = Tables.HISTORY_REFERENCE)
data class HistoryReference(

    /**
     * test uuid for a single measurement & loop uuid for loop measurement
     */
    @PrimaryKey
    val uuid: String,

    val time: Long
)