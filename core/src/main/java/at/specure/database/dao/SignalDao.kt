package at.specure.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.specure.database.Tables
import at.specure.database.entity.Signal2G3G
import at.specure.database.entity.Signal4G
import at.specure.database.entity.SignalWifi

@Dao
interface SignalDao {

    @Query("SELECT * from ${Tables.SIGNAL_2G_3G} WHERE testUUID == :testUUID")
    fun getSignals2G3GForTest(testUUID: String): List<Signal2G3G>

    @Query("SELECT * from ${Tables.SIGNAL_4G} WHERE testUUID == :testUUID")
    fun getSignals4GForTest(testUUID: String): List<Signal4G>

    @Query("SELECT * from ${Tables.SIGNAL_WIFI} WHERE testUUID == :testUUID")
    fun getSignalsWifiForTest(testUUID: String): List<SignalWifi>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(signal: SignalWifi)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(signal: Signal2G3G)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(signal: Signal4G)
}