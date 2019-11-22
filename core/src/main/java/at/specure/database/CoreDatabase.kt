package at.specure.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import at.specure.database.dao.CapabilitesDao
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
import at.specure.database.entity.Capabilities
import at.specure.database.entity.CellInfo
import at.specure.database.entity.CellLocation
import at.specure.database.entity.GeoLocation
import at.specure.database.entity.GraphItem
import at.specure.database.entity.History
import at.specure.database.entity.MobileTest
import at.specure.database.entity.PermissionStatus
import at.specure.database.entity.Ping
import at.specure.database.entity.Signal2G3G
import at.specure.database.entity.Signal4G
import at.specure.database.entity.SignalWifi
import at.specure.database.entity.Test
import at.specure.database.entity.TestTrafficDownload
import at.specure.database.entity.TestTrafficUpload
import at.specure.database.entity.WifiTest

@Database(
    entities = [Capabilities::class,
        CellInfo::class,
        CellLocation::class,
        GeoLocation::class,
        GraphItem::class,
        History::class,
        MobileTest::class,
        PermissionStatus::class,
        Ping::class,
        SignalWifi::class,
        Signal2G3G::class,
        Signal4G::class,
        Test::class,
        TestTrafficDownload::class,
        TestTrafficUpload::class,
        WifiTest::class],
    version = 1
)
@TypeConverters(TypeConverter::class)
abstract class CoreDatabase : RoomDatabase() {

    abstract fun capabilitiesDao(): CapabilitesDao
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