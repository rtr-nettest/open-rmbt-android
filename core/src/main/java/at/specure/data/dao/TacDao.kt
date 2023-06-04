package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import at.specure.data.Tables
import at.specure.data.entity.TacRecord
import timber.log.Timber

@Dao
abstract class TacDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveTermsAndConditions(tac: TacRecord)

    @Query("SELECT * FROM ${Tables.TAC} WHERE url == :url")
    abstract fun loadTermsAndConditions(url: String): TacRecord?

    @Query("DELETE FROM ${Tables.TAC} WHERE url == :url")
    abstract fun deleteTermsAndCondition(url: String) : Int

    @Transaction
    open fun clearInsertItems(tac: TacRecord) {
        val countTAC = deleteTermsAndCondition(tac.url)
        Timber.d("DB: Deleting TaC: $countTAC")
        saveTermsAndConditions(tac)
    }
}