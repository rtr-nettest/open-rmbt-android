package at.specure.data.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import at.specure.data.NetworkTypeCompat
import at.specure.data.Tables
import at.specure.data.entity.History
import at.specure.data.entity.HistoryContainer
import at.specure.data.entity.HistoryReference

@Dao
abstract class HistoryDao {

    @Transaction
    @Query(
        """
        SELECT DISTINCT hr.* 
        FROM ${Tables.HISTORY_REFERENCE} hr
        INNER JOIN ${Tables.HISTORY} h 
            ON hr.uuid = h.referenceUUID
        /* filters */
        WHERE (
            (:ignoreNetworkTypes OR h.networkType IN (:networks))
        AND(
            (:ignoreDevices OR h.networkType IN (:devices))
        ))
        /* add more filters here later */
        ORDER BY hr.time DESC
    """
    )
    abstract fun getHistorySource(
        networks: List<String>,
        ignoreNetworkTypes: Boolean,
        devices: List<String>,
        ignoreDevices: Boolean
    ): DataSource.Factory<Int, HistoryContainer>

    @Query(
        """
        SELECT COUNT(DISTINCT hr.uuid) 
        FROM ${Tables.HISTORY_REFERENCE} hr
        INNER JOIN ${Tables.HISTORY} h 
            ON hr.uuid = h.referenceUUID
        /* filters */
        WHERE (
            (:ignoreNetworkTypes OR h.networkType IN (:networks))
        AND(
            (:ignoreDevices OR h.networkType IN (:devices))
        ))
        /* add more filters here later */
        ORDER BY hr.time DESC
    """
    )
    abstract fun getItemsCount(
        networks: List<String>,
        ignoreNetworkTypes: Boolean,
        devices: List<String>,
        ignoreDevices: Boolean
    ): Int

    /** Number of raw history records cached locally (used to align the paging cursor). */
    @Query("SELECT COUNT(*) FROM ${Tables.HISTORY}")
    abstract fun getHistoryRecordsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveHistory(history: List<History>)

    @Upsert
    abstract fun saveReferences(history: List<HistoryReference>)

    @Transaction
    open fun insert(history: List<History?>) {
        val references = history.map {
            it?.let {
                HistoryReference(it.referenceUUID, it.time)
            }
        }
        val referencesNonNull = references.filterNotNull()
        val historyNonNull = history.filterNotNull()
        saveReferences(referencesNonNull)
        saveHistory(historyNonNull)
    }

    @Query("DELETE FROM ${Tables.HISTORY_REFERENCE}")
    abstract fun clearReferences(): Int

    @Query("DELETE FROM ${Tables.HISTORY}")
    abstract fun clearHistory(): Int

    @Transaction
    open fun clearInsert(history: List<History>) {
        clearReferences()
        clearHistory()
        insert(history)
    }

    @Transaction
    open fun clear() {
        clearReferences()
        clearHistory()
    }

    @Query("SELECT * FROM ${Tables.HISTORY} WHERE testUUID ==:testUUID ORDER BY time DESC")
    abstract fun getItemByUUID(testUUID: String): History?

    @Query("SELECT * FROM ${Tables.HISTORY} WHERE loopUUID ==:loopUuid ORDER BY time DESC")
    abstract fun getItemByLoopUUID(loopUuid: String): List<History>

    @Query("SELECT * FROM ${Tables.HISTORY} WHERE loopUUID ==:loopUuid ORDER BY time DESC")
    abstract fun getItemByLoopUUIDLiveData(loopUuid: String): LiveData<List<History>?>

    @Query("SELECT * FROM ${Tables.HISTORY} ORDER BY time DESC LIMIT :limit")
    abstract fun getLoadedItemsLiveData(limit: Int): LiveData<List<History>?>
}