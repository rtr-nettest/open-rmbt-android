package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables
import java.util.UUID

@Entity(tableName = Tables.SIGNAL_MEASUREMENT_SESSION)
data class SignalMeasurementSession(


    /**
     * id of a signal measurement session
     */
    @PrimaryKey
    val sessionId: String = UUID.randomUUID().toString(),

    /**
     * Timestamp of the signal measurement start
     */
    val timestamp: Long = System.currentTimeMillis()
)