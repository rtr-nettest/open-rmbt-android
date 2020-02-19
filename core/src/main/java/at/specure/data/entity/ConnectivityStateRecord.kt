package at.specure.data.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables
import at.specure.info.connectivity.ConnectivityState

@Keep
@Entity(tableName = Tables.CONNECTIVITY_STATE)
data class ConnectivityStateRecord(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val uuid: String,

    val state: ConnectivityState,

    val message: String?,

    val timeNanos: Long
)