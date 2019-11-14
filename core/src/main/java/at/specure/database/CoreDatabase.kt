package at.specure.database

import androidx.room.Database
import androidx.room.RoomDatabase
import at.specure.database.dao.HistoryDao
import at.specure.database.entity.Capabilities
import at.specure.database.entity.CellInfo
import at.specure.database.entity.CellLocation
import at.specure.database.entity.GeoLocations
import at.specure.database.entity.History
import at.specure.database.entity.PermissionStatus
import at.specure.database.entity.Ping
import at.specure.database.entity.Signal
import at.specure.database.entity.Test
import at.specure.database.entity.TestTrafficItem

@Database(
    entities = [Capabilities::class, CellInfo::class, CellLocation::class, GeoLocations::class, History::class, PermissionStatus::class, Ping::class, Signal::class, Test::class, TestTrafficItem::class],
    version = 1
)
abstract class CoreDatabase : RoomDatabase() {

    abstract fun historyDao(): HistoryDao
}