package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.data.Tables
import at.specure.data.entity.TestResultDetailsRecord

@Dao
interface TestResultDetailsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(testResult: List<TestResultDetailsRecord>)

    @Query("SELECT * from ${Tables.TEST_RESULT_DETAILS} WHERE testUUID == :uuid")
    fun get(uuid: String): LiveData<List<TestResultDetailsRecord>>
}