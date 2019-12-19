package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.TestResultRecord

@Dao
interface TestResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(testResult: TestResultRecord)

    @Query("DELETE FROM ${Tables.TEST_RESULT}")
    fun deleteAll(): Int

    @Delete
    fun deleteTest(test: TestResultRecord): Int

    @Query("SELECT * from ${Tables.TEST_RESULT} WHERE uuid == :uuid")
    fun get(uuid: String): LiveData<TestResultRecord?>
}