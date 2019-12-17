package at.specure.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import at.specure.data.dao.CapabilitiesDao
import at.specure.data.dao.CellInfoDao
import at.specure.data.dao.CellLocationDao
import at.specure.data.dao.GeoLocationDao
import at.specure.data.dao.GraphItemDao
import at.specure.data.dao.HistoryDao
import at.specure.data.dao.PermissionStatusDao
import at.specure.data.dao.PingDao
import at.specure.data.dao.SignalDao
import at.specure.data.dao.SpeedDao
import at.specure.data.dao.TestDao
import at.specure.data.entity.CapabilitiesRecord
import at.specure.data.entity.CellInfoRecord
import at.specure.data.entity.CellLocationRecord
import at.specure.data.entity.GeoLocationRecord
import at.specure.data.entity.GraphItemRecord
import at.specure.data.entity.History
import at.specure.data.entity.PermissionStatusRecord
import at.specure.data.entity.PingRecord
import at.specure.data.entity.SignalRecord
import at.specure.data.entity.SpeedRecord
import at.specure.data.entity.TestRecord
import at.specure.data.entity.TestTelephonyRecord
import at.specure.data.entity.TestWlanRecord

@Database(
    entities = [CapabilitiesRecord::class,
        CellInfoRecord::class,
        CellLocationRecord::class,
        GeoLocationRecord::class,
        GraphItemRecord::class,
        History::class,
        TestTelephonyRecord::class,
        PermissionStatusRecord::class,
        PingRecord::class,
        SignalRecord::class,
        TestRecord::class,
        SpeedRecord::class,
        TestWlanRecord::class],
    version = 22
)
@TypeConverters(TypeConverter::class)
abstract class CoreDatabase : RoomDatabase() {

    abstract fun capabilitiesDao(): CapabilitiesDao
    abstract fun cellInfoDao(): CellInfoDao
    abstract fun cellLocationDao(): CellLocationDao
    abstract fun geoLocationDao(): GeoLocationDao
    abstract fun graphItemsDao(): GraphItemDao
    abstract fun historyDao(): HistoryDao
    abstract fun permissionStatusDao(): PermissionStatusDao
    abstract fun pingDao(): PingDao
    abstract fun signalDao(): SignalDao
    abstract fun testDao(): TestDao
    abstract fun speedDao(): SpeedDao
}