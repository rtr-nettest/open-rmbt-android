package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables

@Entity(tableName = Tables.LOOP_MODE)
data class LoopModeRecord(

    @PrimaryKey
    val uuid: String,
    var testsPerformed: Int = 1,
    var lastTestLongitude: Double? = null,
    var lastTestLatitude: Double? = null,
    var lastTestFinishedTimeMillis: Long = 0,
    var movementDistanceMeters: Int = 0,
    var status: LoopModeState = LoopModeState.RUNNING
)

enum class LoopModeState(val valueInt: Int) {

    IDLE(0),
    RUNNING(1),
    FINISHED(2);

    companion object {

        fun fromValue(value: Int): LoopModeState {
            values().forEach {
                if (it.valueInt == value) return it
            }
            throw IllegalArgumentException("Unknown loop mode value $value")
        }
    }
}