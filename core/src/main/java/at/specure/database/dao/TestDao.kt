package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.TestTelephonyRecord
import at.specure.database.entity.TestRecord
import at.specure.database.entity.TestWlanRecord

@Dao
interface TestDao {

    @Query("SELECT * from ${Tables.TEST} ORDER BY timeMillis DESC LIMIT 1")
    fun getLatestTestResult(): TestRecord?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(test: TestRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(test: TestTelephonyRecord)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(test: TestWlanRecord)

    @Query("DELETE FROM ${Tables.TEST}")
    fun deleteAll(): Int

    @Delete
    fun deleteTest(test: TestRecord): Int

    @Query("SELECT * from ${Tables.TEST} WHERE uuid == :uuid")
    fun getTestResult(uuid: String): TestRecord?
}