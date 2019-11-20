package at.specure.database.dao

import androidx.lifecycle.MutableLiveData
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.TestTrafficItem

interface TestTrafficItemDao {

    @Query("SELECT * from ${Tables.TEST_TRAFFIC_ITEM} WHERE testUUID == :testUUID")
    fun getTestTrafficItemsForTest(testUUID: String): MutableLiveData<List<TestTrafficItem>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(testTrafficItem: TestTrafficItem)
}