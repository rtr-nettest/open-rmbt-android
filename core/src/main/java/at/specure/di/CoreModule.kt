package at.specure.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import at.rmbt.client.control.ControlEndpointProvider
import at.rmbt.client.control.ControlServerClient
import at.specure.config.Config
import at.specure.config.ControlServerProviderImpl
import at.specure.data.ClientUUID
import at.specure.data.ControlServerSettings
import at.specure.data.HistoryFilterOptions
import at.specure.data.MapServerSettings
import at.specure.data.TermsAndConditions
import at.specure.data.repository.IpCheckRepository
import at.specure.data.repository.SettingsRepository
import at.specure.data.repository.SettingsRepositoryImpl
import at.specure.info.cell.CellInfoWatcher
import at.specure.info.cell.CellInfoWatcherImpl
import at.specure.info.connectivity.ConnectivityWatcher
import at.specure.info.connectivity.ConnectivityWatcherImpl
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
        telephonyManager: TelephonyManager,
        activeNetworkWatcher: ActiveNetworkWatcher,
        wifiInfoWatcher: WifiInfoWatcher,
        cellInfoWatcher: CellInfoWatcher
    ): SignalStrengthWatcher =
        SignalStrengthWatcherImpl(telephonyManager, activeNetworkWatcher, wifiInfoWatcher, cellInfoWatcher)

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
        cellInfoWatcher: CellInfoWatcher
    ): ActiveNetworkWatcher =
        ActiveNetworkWatcher(connectivityWatcher, wifiInfoWatcher, cellInfoWatcher)

    @Provides
    @Singleton
    fun provideIpChangeWatcher(ipCheckRepository: IpCheckRepository, connectivityWatcher: ConnectivityWatcher): IpChangeWatcher =
        IpChangeWatcherImpl(ipCheckRepository, connectivityWatcher)

    @Provides
    @Singleton
    fun provideLocationState(context: Context): LocationProviderStateWatcher = LocationProviderStateWatcherImpl(context)

    @Provides
    @Singleton
    fun provideControlEndpointProvider(config: Config): ControlEndpointProvider = ControlServerProviderImpl(config)

    @Provides
    fun provideSettingRepository(
        context: Context,
        controlServerClient: ControlServerClient,
        clientUUID: ClientUUID,
        controlServerSettings: ControlServerSettings,
        mapServerSettings: MapServerSettings,
        termsAndConditions: TermsAndConditions,
        historyFilterOptions: HistoryFilterOptions,
        config: Config
    ): SettingsRepository =
        SettingsRepositoryImpl(
            context = context,
            controlServerClient = controlServerClient,
            clientUUID = clientUUID,
            controlServerSettings = controlServerSettings,
            mapServerSettings = mapServerSettings,
            termsAndConditions = termsAndConditions,
            historyFilterOptions = historyFilterOptions,
            config = config
        )

    @Provides
    @Singleton
    fun provideTestController(config: Config, clientUUID: ClientUUID): TestController = TestControllerImpl(config, clientUUID)

    @Provides
    @Singleton
    fun provideCellLocationWatcher(context: Context, telephonyManager: TelephonyManager): CellLocationWatcher =
        CellLocationWatcherImpl(context, telephonyManager)
}
