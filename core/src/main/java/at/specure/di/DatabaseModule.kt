/*
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.specure.di

import android.content.Context
import androidx.room.Room
import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.IpClient
import at.rmbt.client.control.MapServerClient
import at.specure.config.Config
import at.specure.data.ClientUUID
import at.specure.data.ControlServerSettings
import at.specure.data.CoreDatabase
import at.specure.data.HistoryFilterOptions
import at.specure.data.repository.HistoryRepository
import at.specure.data.repository.HistoryRepositoryImpl
import at.specure.data.repository.IpCheckRepository
import at.specure.data.repository.IpCheckRepositoryImpl
import at.specure.data.repository.MapRepository
import at.specure.data.repository.MapRepositoryImpl
import at.specure.data.repository.ResultsRepository
import at.specure.data.repository.ResultsRepositoryImpl
import at.specure.data.repository.SettingsRepository
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.data.repository.SignalMeasurementRepositoryImpl
import at.specure.data.repository.TestDataRepository
import at.specure.data.repository.TestDataRepositoryImpl
import at.specure.data.repository.TestResultsRepository
import at.specure.data.repository.TestResultsRepositoryImpl
import at.specure.info.strength.SignalStrengthWatcher
import at.specure.location.LocationWatcher
import at.specure.util.ActiveFilter
import at.specure.util.FilterValuesStorage
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

/**
 * Dagger database module that provides database instance used by core library
 */
@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun provideCoreDatabase(context: Context): CoreDatabase {
        val builder = Room.databaseBuilder(context, CoreDatabase::class.java, "CoreDatabase.db")
        builder.fallbackToDestructiveMigration()
        return builder.build()
    }

    @Provides
    fun provideTestDataRepository(database: CoreDatabase): TestDataRepository =
        TestDataRepositoryImpl(database)

    @Provides
    fun provideResultsRepository(
        context: Context,
        database: CoreDatabase,
        clientUUID: ClientUUID,
        client: ControlServerClient,
        config: Config,
        settingsRepository: SettingsRepository
    ): ResultsRepository =
        ResultsRepositoryImpl(context, database, clientUUID, client, config, settingsRepository)

    @Provides
    fun provideIpCheckRepository(
        context: Context,
        config: Config,
        clientUUID: ClientUUID,
        @Named("GPSAndNetworkLocationProvider") locationWatcher: LocationWatcher,
        signalStrengthWatcher: SignalStrengthWatcher,
        client: IpClient
    ): IpCheckRepository = IpCheckRepositoryImpl(
        context = context,
        config = config,
        clientUUID = clientUUID,
        locationWatcher = locationWatcher,
        signalStrengthWatcher = signalStrengthWatcher,
        client = client
    )

    @Provides
    fun provideHistoryRepository(
        database: CoreDatabase,
        config: Config,
        clientUUID: ClientUUID,
        controlServerClient: ControlServerClient,
        settingsRepository: SettingsRepository,
        filterOptions: HistoryFilterOptions
    ): HistoryRepository =
        HistoryRepositoryImpl(database.historyDao(), database.historyMedianDao(), config, clientUUID, controlServerClient, settingsRepository, filterOptions, database.qoeInfoDao())

    @Provides
    fun provideTestResultRepository(
        database: CoreDatabase,
        clientUUID: ClientUUID,
        controlServerClient: ControlServerClient,
        config: Config
    ): TestResultsRepository =
        TestResultsRepositoryImpl(database, clientUUID, controlServerClient, config)

    @Provides
    fun provideMapRepository(
        client: MapServerClient,
        database: CoreDatabase,
        filterValuesStorage: FilterValuesStorage,
        activeFilter: ActiveFilter,
        controlServerSettings: ControlServerSettings,
        config: Config
    ): MapRepository =
        MapRepositoryImpl(client, database, filterValuesStorage, activeFilter, controlServerSettings, config)

    @Provides
    fun provideSignalMeasurementRepository(
        database: CoreDatabase,
        context: Context,
        clientUUID: ClientUUID,
        client: ControlServerClient
    ): SignalMeasurementRepository = SignalMeasurementRepositoryImpl(database, context, clientUUID, client)
}
