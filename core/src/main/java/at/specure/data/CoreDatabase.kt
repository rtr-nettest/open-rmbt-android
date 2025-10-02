package at.specure.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import at.specure.data.dao.CapabilitiesDao
import at.specure.data.dao.CellInfoDao
import at.specure.data.dao.CellLocationDao
import at.specure.data.dao.ConnectivityStateDao
import at.specure.data.dao.FencesResultItemDao
import at.specure.data.dao.GeoLocationDao
import at.specure.data.dao.GraphItemDao
import at.specure.data.dao.HistoryDao
import at.specure.data.dao.HistoryMedianDao
import at.specure.data.dao.JplResultsDao
import at.specure.data.dao.MapDao
import at.specure.data.dao.PermissionStatusDao
import at.specure.data.dao.PingDao
import at.specure.data.dao.QoeInfoDao
import at.specure.data.dao.QosCategoryDao
import at.specure.data.dao.QosTestGoalDao
import at.specure.data.dao.QosTestItemDao
import at.specure.data.dao.SignalDao
import at.specure.data.dao.SignalMeasurementDao
import at.specure.data.dao.SpeedDao
import at.specure.data.dao.TacDao
import at.specure.data.dao.TestDao
import at.specure.data.dao.TestResultDao
import at.specure.data.dao.TestResultDetailsDao
import at.specure.data.dao.TestResultGraphItemDao
import at.specure.data.entity.CapabilitiesRecord
import at.specure.data.entity.CellInfoRecord
import at.specure.data.entity.CellLocationRecord
import at.specure.data.entity.ConnectivityStateRecord
import at.specure.data.entity.FencesResultItemRecord
import at.specure.data.entity.GeoLocationRecord
import at.specure.data.entity.GraphItemRecord
import at.specure.data.entity.History
import at.specure.data.entity.HistoryReference
import at.specure.data.entity.LoopModeRecord
import at.specure.data.entity.MarkerMeasurementRecord
import at.specure.data.entity.PermissionStatusRecord
import at.specure.data.entity.PingRecord
import at.specure.data.entity.QoSResultRecord
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.QosCategoryRecord
import at.specure.data.entity.QosTestGoalRecord
import at.specure.data.entity.QosTestItemRecord
import at.specure.data.entity.SignalMeasurementChunk
import at.specure.data.entity.SignalMeasurementFenceRecord
import at.specure.data.entity.SignalMeasurementRecord
import at.specure.data.entity.SignalMeasurementSession
import at.specure.data.entity.SignalRecord
import at.specure.data.entity.SpeedRecord
import at.specure.data.entity.TacRecord
import at.specure.data.entity.TestRecord
import at.specure.data.entity.TestResultDetailsRecord
import at.specure.data.entity.TestResultGraphItemRecord
import at.specure.data.entity.TestResultRecord
import at.specure.data.entity.TestTelephonyRecord
import at.specure.data.entity.TestWlanRecord
import at.specure.data.entity.VoipTestResultRecord

@Database(
    entities = [
        CapabilitiesRecord::class,
        CellInfoRecord::class,
        CellLocationRecord::class,
        FencesResultItemRecord::class,
        GeoLocationRecord::class,
        GraphItemRecord::class,
        History::class,
        HistoryLoopMedian::class,
        VoipTestResultRecord::class,
        PermissionStatusRecord::class,
        PingRecord::class,
        QoeInfoRecord::class,
        QosCategoryRecord::class,
        QosTestGoalRecord::class,
        QosTestItemRecord::class,
        QoSResultRecord::class,
        SignalRecord::class,
        TestTelephonyRecord::class,
        SpeedRecord::class,
        TacRecord::class,
        TestRecord::class,
        TestResultGraphItemRecord::class,
        TestResultRecord::class,
        TestWlanRecord::class,
        TestResultDetailsRecord::class,
        MarkerMeasurementRecord::class,
        LoopModeRecord::class,
        SignalMeasurementRecord::class,
        SignalMeasurementChunk::class,
        SignalMeasurementFenceRecord::class,
        SignalMeasurementSession::class,
        ConnectivityStateRecord::class,
        HistoryReference::class,
               ],
    // Needs to upgraded when schema changes - else: "Room cannot verify the data integrity. Looks like you've changed schema but forgot to update the version number. You can simply fix this by increasing the version number."
    version = 156
)
@TypeConverters(TypeConverter::class)
abstract class CoreDatabase : RoomDatabase() {

    abstract fun capabilitiesDao(): CapabilitiesDao
    abstract fun cellInfoDao(): CellInfoDao
    abstract fun cellLocationDao(): CellLocationDao
    abstract fun geoLocationDao(): GeoLocationDao
    abstract fun graphItemsDao(): GraphItemDao
    abstract fun historyDao(): HistoryDao
    abstract fun historyMedianDao(): HistoryMedianDao
    abstract fun jplResultsDao(): JplResultsDao
    abstract fun permissionStatusDao(): PermissionStatusDao
    abstract fun pingDao(): PingDao
    abstract fun qoeInfoDao(): QoeInfoDao
    abstract fun qosCategoryDao(): QosCategoryDao
    abstract fun qosTestGoalDao(): QosTestGoalDao
    abstract fun qosTestItemDao(): QosTestItemDao
    abstract fun signalDao(): SignalDao
    abstract fun speedDao(): SpeedDao
    abstract fun tacDao(): TacDao
    abstract fun testDao(): TestDao
    abstract fun testResultDao(): TestResultDao
    abstract fun testResultDetailsDao(): TestResultDetailsDao
    abstract fun testResultGraphItemDao(): TestResultGraphItemDao
    abstract fun fencesResultItemDao(): FencesResultItemDao
    abstract fun mapDao(): MapDao
    abstract fun signalMeasurementDao(): SignalMeasurementDao
    abstract fun connectivityStateDao(): ConnectivityStateDao
}