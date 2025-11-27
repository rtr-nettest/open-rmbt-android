package at.specure.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import at.rmbt.client.control.ControlEndpointProvider
import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.IpEndpointProvider
import at.rmbt.client.control.MapEndpointProvider
import at.specure.config.Config
import at.specure.config.ControlServerProviderImpl
import at.specure.config.IpEndpointProviderImpl
import at.specure.config.MapServerProviderImpl
import at.specure.data.ClientUUID
import at.specure.data.ClientUUIDLegacy
import at.specure.data.ControlServerSettings
import at.specure.data.CoreDatabase
import at.specure.data.CoverageMeasurementSettings
import at.specure.data.HistoryFilterOptions
import at.specure.data.MeasurementServers
import at.specure.data.NewsSettings
import at.specure.data.TermsAndConditions
import at.specure.data.repository.DeviceSyncRepository
import at.specure.data.repository.DeviceSyncRepositoryImpl
import at.specure.data.repository.HistoryLoader
import at.specure.data.repository.HistoryRepository
import at.specure.data.repository.IpCheckRepository
import at.specure.data.repository.MeasurementRepository
import at.specure.data.repository.MeasurementRepositoryImpl
import at.specure.data.repository.NewsRepository
import at.specure.data.repository.NewsRepositoryImpl
import at.specure.data.repository.SettingsRepository
import at.specure.data.repository.SettingsRepositoryImpl
import at.specure.data.repository.SignalMeasurementRepository
import at.specure.data.repository.TestDataRepository
import at.specure.info.cell.CellInfoWatcher
import at.specure.info.cell.CellInfoWatcherImpl
import at.specure.info.connectivity.ConnectivityWatcher
import at.specure.info.connectivity.ConnectivityWatcherImpl
import at.specure.info.ip.CaptivePortal
import at.specure.info.ip.IpChangeWatcher
import at.specure.info.ip.IpChangeWatcherImpl
import at.specure.info.network.ActiveNetworkWatcher
import at.specure.info.strength.SignalStrengthWatcher
import at.specure.info.strength.SignalStrengthWatcherImpl
import at.specure.info.wifi.WifiInfoWatcher
import at.specure.info.wifi.WifiInfoWatcherImpl
import at.specure.location.LocationWatcher
import at.specure.location.cell.CellLocationWatcher
import at.specure.location.cell.CellLocationWatcherImpl
import at.specure.measurement.coverage.RtrCoverageSessionManager
import at.specure.measurement.coverage.domain.CoverageSessionManager
import at.specure.measurement.coverage.domain.PingProcessor
import at.specure.measurement.coverage.domain.monitors.ConnectivityMonitor
import at.specure.measurement.coverage.domain.monitors.DataSimMonitor
import at.specure.measurement.coverage.domain.validators.CoverageDataValidator
import at.specure.measurement.coverage.presentation.validators.CoverageDurationValidator
import at.specure.measurement.coverage.presentation.validators.CoverageLocationValidator
import at.specure.measurement.coverage.presentation.validators.CoverageNetworkValidator
import at.specure.measurement.coverage.domain.validators.DurationValidator
import at.specure.measurement.coverage.domain.validators.LocationValidator
import at.specure.measurement.coverage.domain.validators.NetworkValidator
import at.specure.measurement.coverage.presentation.monitors.RtrConnectivityMonitor
import at.specure.measurement.coverage.presentation.RtrPingProcessor
import at.specure.measurement.coverage.presentation.monitors.CoverageDataSimMonitor
import at.specure.measurement.coverage.presentation.validators.MainCoverageDataValidator
import at.specure.test.TestController
import at.specure.test.TestControllerImpl
import at.specure.util.map.CustomMarker
import at.specure.util.permission.LocationAccess
import at.specure.util.permission.LocationAccessImpl
import at.specure.util.permission.PermissionsWatcher
import at.specure.util.permission.PhoneStateAccess
import at.specure.util.permission.PhoneStateAccessImpl
import cz.mroczis.netmonster.core.INetMonster
import cz.mroczis.netmonster.core.factory.NetMonsterFactory
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

/**
 * Dagger core module that provides instances used by core library
 */
@Module
class CoreModule {

    @Provides
    @Singleton
    fun provideSignalStrengthWatcher(
        activeNetworkWatcher: ActiveNetworkWatcher,
        wifiInfoWatcher: WifiInfoWatcher,
        cellInfoWatcher: CellInfoWatcher,
        locationAccess: LocationAccess
    ): SignalStrengthWatcher =
        SignalStrengthWatcherImpl(
            activeNetworkWatcher,
            wifiInfoWatcher,
            cellInfoWatcher,
            locationAccess
        )

    @Provides
    @Singleton
    fun provideConnectivityWatcher(connectivityManager: ConnectivityManager): ConnectivityWatcher = ConnectivityWatcherImpl(connectivityManager)

    @Provides
    @Singleton
    fun provideNetmonster(context: Context): INetMonster = NetMonsterFactory.get(context)

    @Provides
    @Singleton
    fun provideLocationAccess(context: Context): LocationAccess = LocationAccessImpl(context)

    @Provides
    @Singleton
    fun providePhoneStateAccess(context: Context): PhoneStateAccess =
        PhoneStateAccessImpl(context)

    @Provides
    @Singleton
    fun providePermissionsWatcher(context: Context, locationAccess: LocationAccess, phoneStateAccess: PhoneStateAccess): PermissionsWatcher =
        PermissionsWatcher(context, locationAccess, phoneStateAccess)

    @Provides
    @Singleton
    fun provideCellInfoWatcher(
        context: Context,
        telephonyManager: TelephonyManager,
        connectivityManager: ConnectivityManager,
        netmonster: INetMonster,
        subscriptionManager: SubscriptionManager,
        config: Config
    ): CellInfoWatcher =
        CellInfoWatcherImpl(
            context,
            telephonyManager,
            connectivityManager,
            netmonster,
            subscriptionManager,
            config
        )

    @Provides
    @Singleton
    fun provideWifiInfoWatcher(wifiManager: WifiManager): WifiInfoWatcher = WifiInfoWatcherImpl(wifiManager)

    @Provides
    @Singleton
    fun provideActiveNetworkWatcher(
        context: Context,
        netmonster: INetMonster,
        subscriptionManager: SubscriptionManager,
        telephonyManager: TelephonyManager,
        connectivityManager: ConnectivityManager,
        connectivityWatcher: ConnectivityWatcher,
        wifiInfoWatcher: WifiInfoWatcher,
        @Named("GPSAndNetworkLocationProvider") locationWatcher: LocationWatcher,
        captivePortal: CaptivePortal
    ): ActiveNetworkWatcher =
        ActiveNetworkWatcher(
            context,
            netmonster,
            subscriptionManager,
            telephonyManager,
            connectivityManager,
            connectivityWatcher,
            wifiInfoWatcher,
            locationWatcher.stateWatcher,
            captivePortal
        )

    @Provides
    @Singleton
    fun provideIpChangeWatcher(
        ipCheckRepository: IpCheckRepository,
        connectivityWatcher: ConnectivityWatcher,
        captivePortal: CaptivePortal
    ): IpChangeWatcher =
        IpChangeWatcherImpl(ipCheckRepository, connectivityWatcher, captivePortal)

    @Provides
    @Singleton
    fun provideControlEndpointProvider(config: Config): ControlEndpointProvider = ControlServerProviderImpl(config)

    @Provides
    @Singleton
    fun provideIpEndpointProvider(config: Config): IpEndpointProvider = IpEndpointProviderImpl(config)

    @Provides
    @Singleton
    fun provideMapEndpointProvider(config: Config): MapEndpointProvider = MapServerProviderImpl(config)

    @Provides
    @Singleton
    fun provideCaptivePortal(ipEndpointProvider: IpEndpointProvider): CaptivePortal = CaptivePortal(ipEndpointProvider)

    @Provides
    fun provideSettingRepository(
        context: Context,
        controlServerClient: ControlServerClient,
        clientUUID: ClientUUID,
        clientUUIDLegacy: ClientUUIDLegacy,
        controlServerSettings: ControlServerSettings,
        termsAndConditions: TermsAndConditions,
        measurementServers: MeasurementServers,
        historyFilterOptions: HistoryFilterOptions,
        config: Config,
        coreDatabase: CoreDatabase
    ): SettingsRepository =
        SettingsRepositoryImpl(
            context = context,
            controlServerClient = controlServerClient,
            clientUUID = clientUUID,
            clientUUIDLegacy = clientUUIDLegacy,
            controlServerSettings = controlServerSettings,
            termsAndConditions = termsAndConditions,
            measurementsServers = measurementServers,
            historyFilterOptions = historyFilterOptions,
            config = config,
            tacDao = coreDatabase.tacDao()
        )

    @Provides
    @Singleton
    fun provideNewsRepository(
        context: Context,
        controlServerClient: ControlServerClient,
        clientUUID: ClientUUID,
        newsSettings: NewsSettings
    ): NewsRepository =
        NewsRepositoryImpl(context = context, controlServerClient = controlServerClient, clientUUID = clientUUID, newsSettings = newsSettings)

    @Provides
    @Singleton
    fun provideTestController(
        context: Context,
        config: Config,
        clientUUID: ClientUUID,
        measurementServers: MeasurementServers,
        connectivityManager: ConnectivityManager
    ): TestController =
        TestControllerImpl(context, config, clientUUID, connectivityManager, measurementServers)

    @Provides
    @Singleton
    fun provideMeasurementServers(context: Context): MeasurementServers =
        MeasurementServers(context)

    @Provides
    @Singleton
    fun provideCellLocationWatcher(
        context: Context,
        telephonyManager: TelephonyManager,
        subscriptionManager: SubscriptionManager
    ): CellLocationWatcher =
        CellLocationWatcherImpl(context, telephonyManager, subscriptionManager)

    @Provides
    fun provideDeviceSyncRepository(
        context: Context,
        controlServerClient: ControlServerClient,
        clientUUID: ClientUUID,
        historyRepository: HistoryRepository,
        settingsRepository: SettingsRepository,
        historyLoader: HistoryLoader
    ): DeviceSyncRepository = DeviceSyncRepositoryImpl(context, controlServerClient, clientUUID, historyRepository, settingsRepository, historyLoader)

    @Provides
    fun provideMeasurementRepository(
        context: Context,
        telephonyManager: TelephonyManager,
        subscriptionManager: SubscriptionManager,
        activeNetworkWatcher: ActiveNetworkWatcher,
        cellInfoWatcher: CellInfoWatcher,
        repository: TestDataRepository,
        wifiInfoWatcher: WifiInfoWatcher,
        config: Config,
        permissionsWatcher: PermissionsWatcher
    ): MeasurementRepository = MeasurementRepositoryImpl(
        context = context,
        telephonyManager = telephonyManager,
        subscriptionManager = subscriptionManager,
        activeNetworkWatcher = activeNetworkWatcher,
        cellInfoWatcher = cellInfoWatcher,
        repository = repository,
        wifiInfoWatcher = wifiInfoWatcher,
        config = config,
        permissionsWatcher = permissionsWatcher
    )

    @Provides
    @Singleton
    fun provideCoverageSettings(context: Context): CoverageMeasurementSettings = CoverageMeasurementSettings(context)

    @Provides
    @Singleton
    fun provideCustomMarker(
        context: Context
    ) : CustomMarker = CustomMarker(context = context)

    @Provides
    @Singleton
    fun provideGpsValidator(
        config: Config
    ): LocationValidator = CoverageLocationValidator(
        config
    )

    @Provides
    @Singleton
    fun provideDurationValidator(
        config: Config
    ): DurationValidator = CoverageDurationValidator(
        minimalFenceDurationMillis = config.minimalFenceDurationMillisForSignalMeasurement,
    )

    @Provides
    @Singleton
    fun provideNetworkValidator(): NetworkValidator = CoverageNetworkValidator()

    @Provides
    @Singleton
    fun provideCoverageDataValidator(
        networkValidator: NetworkValidator,
        locationValidator: LocationValidator,
        durationValidator: DurationValidator
    ): CoverageDataValidator = MainCoverageDataValidator(
        networkValidator = networkValidator,
        locationValidator = locationValidator,
        durationValidator = durationValidator
    )

    @Provides
    @Singleton
    fun providePingProcessor(): PingProcessor = RtrPingProcessor()

    @Provides
    @Singleton
    fun provideCoverageSessionManager(
        signalMeasurementRepository: SignalMeasurementRepository,
        coverageMeasurementSettings: CoverageMeasurementSettings
    ): CoverageSessionManager = RtrCoverageSessionManager(
        signalMeasurementRepository = signalMeasurementRepository,
        coverageMeasurementSettings = coverageMeasurementSettings
    )

    @Provides
    @Singleton
    fun provideConnectivityMonitor(
        context: Context,
        connectivityManager: ConnectivityManager,
        telephonyManager: TelephonyManager,
    ): ConnectivityMonitor = RtrConnectivityMonitor(
        context,
        connectivityManager,
        telephonyManager,
    )
}
