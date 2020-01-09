package at.specure.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import at.specure.data.Columns
import at.specure.data.Tables

@Entity(
    tableName = Tables.TEST_RESULT_DETAILS,
    primaryKeys = [Columns.TEST_DETAILS_TEST_UUID, Columns.TEST_DETAILS_TITLE, Columns.TEST_DETAILS_VALUE]
)
data class TestResultDetailsRecord(
    @ColumnInfo(name = Columns.TEST_DETAILS_TEST_UUID)
    val testUUID: String,
    val openTestUUID: String?,
    val openUuid: String?,
    val time: Long?,
    val timezone: String?,
    @ColumnInfo(name = Columns.TEST_DETAILS_TITLE)
    val title: String,
    @ColumnInfo(name = Columns.TEST_DETAILS_VALUE)
    val value: String
)