package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.Test

@Dao
interface TestDao {

    @Query("SELECT * from ${Tables.TEST} ORDER BY time DESC LIMIT 1")
    fun getLatestTestResult(): Test?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(test: Test)

    @Query("DELETE FROM ${Tables.TEST}")
    suspend fun deleteAll(): Int

    @Delete
    suspend fun deleteTest(test: Test): Int
}