package at.specure.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import at.specure.data.Tables
import at.specure.data.entity.TacRecord

@Dao
abstract class TacDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveTermsAndConditions(tac: TacRecord)

    @Query("SELECT * FROM ${Tables.TAC} WHERE url == :url")
    abstract fun loadTermsAndConditions(url: String): TacRecord?

    @Query("DELETE FROM ${Tables.TAC} WHERE url == :url")
    abstract fun deleteTermsAndCondition(url: String)

    @Transaction
    open fun clearInsertItems(tac: TacRecord) {
        deleteTermsAndCondition(tac.url)
        saveTermsAndConditions(tac)
    }
}