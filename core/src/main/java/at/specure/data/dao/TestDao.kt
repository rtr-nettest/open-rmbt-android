package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import at.specure.data.Tables
import at.specure.data.entity.TestRecord
import at.specure.data.entity.TestTelephonyRecord
import at.specure.data.entity.TestWlanRecord

@Dao
interface TestDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(test: TestRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(test: TestTelephonyRecord)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(test: TestWlanRecord)

    @Update
    fun update(test: TestRecord)

    @Query("DELETE FROM ${Tables.TEST}")
    fun deleteAll(): Int

    @Delete
    fun deleteTest(test: TestRecord): Int

    @Query("SELECT * from ${Tables.TEST} WHERE uuid == :uuid")
    fun get(uuid: String): TestRecord?
}