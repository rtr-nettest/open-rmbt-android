package at.specure.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import at.specure.data.Tables

@Entity(tableName = Tables.LOOP_MODE)
data class LoopModeRecord(

    @PrimaryKey
    val localUuid: String,
    var uuid: String?,
    var lastTestUuid: String?,
    var testsPerformed: Int = 0,
    var lastTestLongitude: Double? = null,
    var lastTestLatitude: Double? = null,
    var lastTestFinishedTimeMillis: Long = 0,
    var movementDistanceMeters: Int = 0,
    var status: LoopModeState = LoopModeState.RUNNING
)

enum class LoopModeState(val valueInt: Int) {

    IDLE(0), // loop mode is not running (like default) or running in waiting phase - we should revisit states and introduce loop mode waiting state maybe
    RUNNING(1), // loop mode is running - it means that test is running
    FINISHED(2), // loop mode is finished - all tests are executed
    CANCELLED(3); // loop mode is cancelled - user cancelled the test

    companion object {

        fun fromValue(value: Int): LoopModeState {
            values().forEach {
                if (it.valueInt == value) return it
            }
            throw IllegalArgumentException("Unknown loop mode value $value")
        }
    }
}