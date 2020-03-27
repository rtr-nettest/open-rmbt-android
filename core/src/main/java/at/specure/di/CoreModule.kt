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
import at.specure.data.ControlServerSettings
import at.specure.data.CoreDatabase
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
import at.specure.location.LocationProviderStateWatcher
import at.specure.location.LocationProviderStateWatcherImpl
import at.specure.location.cell.CellLocationWatcher
import at.specure.location.cell.CellLocationWatcherImpl
import at.specure.test.TestController
import at.specure.test.TestControllerImpl
import at.specure.util.permission.LocationAccess
import at.specure.util.permission.LocationAccessImpl
import at.specure.util.permission.PermissionsWatcher
import at.specure.util.permission.PhoneStateAccess
import at.specure.util.permission.PhoneStateAccessImpl
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Dagger core module that provides instances used by core library
 */
@Module
class CoreModule {

    @Provides
    @Singleton
    fun provideSignalStrengthWatcher(
        subscriptionManager: SubscriptionManager,
        telephonyManager: TelephonyManager,
        activeNetworkWatcher: ActiveNetworkWatcher,
        wifiInfoWatcher: WifiInfoWatcher,
        cellInfoWatcher: CellInfoWatcher,
        locationAccess: LocationAccess
    ): SignalStrengthWatcher =
        SignalStrengthWatcherImpl(subscriptionManager, telephonyManager, activeNetworkWatcher, wifiInfoWatcher, cellInfoWatcher, locationAccess)

    @Provides
    @Singleton
    fun provideConnectivityWatcher(connectivityManager: ConnectivityManager): ConnectivityWatcher = ConnectivityWatcherImpl(connectivityManager)

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
        telephonyManager: TelephonyManager,
        subscriptionManager: SubscriptionManager,
        locationAccess: LocationAccess,
        phoneStateAccess: PhoneStateAccess,
        connectivityManager: ConnectivityManager
    ): CellInfoWatcher =
        CellInfoWatcherImpl(telephonyManager, subscriptionManager, locationAccess, phoneStateAccess, connectivityManager)

    @Provides
    @Singleton
    fun provideWifiInfoWatcher(wifiManager: WifiManager): WifiInfoWatcher = WifiInfoWatcherImpl(wifiManager)

    @Provides
    @Singleton
    fun provideActiveNetworkWatcher(
        connectivityWatcher: ConnectivityWatcher,
        wifiInfoWatcher: WifiInfoWatcher,
        cellInfoWatcher: CellInfoWatcher,
        locationProvider: LocationProviderStateWatcher
    ): ActiveNetworkWatcher =
        ActiveNetworkWatcher(connectivityWatcher, wifiInfoWatcher, cellInfoWatcher, locationProvider)

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
    fun provideLocationState(context: Context): LocationProviderStateWatcher = LocationProviderStateWatcherImpl(context)

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
    fun provideCellLocationWatcher(context: Context, telephonyManager: TelephonyManager): CellLocationWatcher =
        CellLocationWatcherImpl(context, telephonyManager)

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
}
