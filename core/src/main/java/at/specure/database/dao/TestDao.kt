package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.MobileTest
import at.specure.database.entity.Test
import at.specure.database.entity.WifiTest

@Dao
interface TestDao {

    @Query("SELECT * from ${Tables.TEST} ORDER BY timeMillis DESC LIMIT 1")
    fun getLatestTestResult(): Test?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(test: Test)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(test: MobileTest)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(test: WifiTest)

    @Query("DELETE FROM ${Tables.TEST}")
    fun deleteAll(): Int

    @Delete
    fun deleteTest(test: Test): Int

    @Query("SELECT * from ${Tables.TEST} WHERE uuid == :uuid")
    fun getTestResult(uuid: String): Test?
}