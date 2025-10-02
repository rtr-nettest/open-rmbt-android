package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import at.specure.data.Tables
import at.specure.data.entity.FencesResultItemRecord
import timber.log.Timber

@Dao
abstract class FencesResultItemDao {

    @Query("SELECT * from ${Tables.FENCES_RESULT_ITEM} WHERE testUUID == :testUUID ORDER BY offsetMillis ASC ")
    abstract fun getFencesLiveData(testUUID: String): LiveData<List<FencesResultItemRecord>>

    @Query("DELETE FROM ${Tables.FENCES_RESULT_ITEM} WHERE (testUUID == :testOpenUUID)")
    abstract fun removeFenceItem(testOpenUUID: String): Int

    @Transaction
    open fun clearInsertItems(fences: List<FencesResultItemRecord>?) {
        if (!fences.isNullOrEmpty()) {
            val removedCount = removeFenceItem(fences.first().testUUID)
            Timber.d("DB: clearing graph items:  $removedCount for uuid: ${fences.first().testUUID}")
            val inserted = insertItem(fences)
            Timber.d("DB: inserted graph items:  $inserted for uuid: ${fences.first().testUUID}")
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertItem(fences: List<FencesResultItemRecord>)

    @Query("DELETE FROM ${Tables.FENCES_RESULT_ITEM}")
    abstract fun deleteAll(): Int
}