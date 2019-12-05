package at.specure.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import at.specure.database.dao.CapabilitiesDao
import at.specure.database.dao.CellInfoDao
import at.specure.database.dao.CellLocationDao
import at.specure.database.dao.GeoLocationDao
import at.specure.database.dao.GraphItemDao
import at.specure.database.dao.HistoryDao
import at.specure.database.dao.PermissionStatusDao
import at.specure.database.dao.PingDao
import at.specure.database.dao.SignalDao
import at.specure.database.dao.TestDao
import at.specure.database.dao.TestTrafficItemDao
import at.specure.database.entity.CapabilitiesRecord
import at.specure.database.entity.CellInfoRecord
import at.specure.database.entity.CellLocationRecord
import at.specure.database.entity.GeoLocationRecord
import at.specure.database.entity.GraphItemRecord
import at.specure.database.entity.History
import at.specure.database.entity.TestTelephonyRecord
import at.specure.database.entity.PermissionStatusRecord
import at.specure.database.entity.PingRecord
import at.specure.database.entity.SignalRecord
import at.specure.database.entity.TestRecord
import at.specure.database.entity.DownloadTrafficRecord
import at.specure.database.entity.UploadTrafficRecord
import at.specure.database.entity.TestWlanRecord

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
        DownloadTrafficRecord::class,
        UploadTrafficRecord::class,
        TestWlanRecord::class],
    version = 15
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
    abstract fun testTrafficItemDao(): TestTrafficItemDao
}