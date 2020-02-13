package at.specure.data.repository

import android.content.Context
import at.rmbt.client.control.ControlServerClient
import at.specure.config.Config
import at.specure.data.ClientUUID
import at.specure.data.ControlServerSettings
import at.specure.data.HistoryFilterOptions
import at.specure.data.MapServerSettings
import at.specure.data.TermsAndConditions
import at.specure.data.toSettingsRequest
import at.specure.test.DeviceInfo

class SettingsRepositoryImpl(
    context: Context,
    private val controlServerClient: ControlServerClient,
    private val clientUUID: ClientUUID,
    private val controlServerSettings: ControlServerSettings,
    private val mapServerSettings: MapServerSettings,
    private val termsAndConditions: TermsAndConditions,
    private val historyFilterOptions: HistoryFilterOptions,
    private val config: Config
) : SettingsRepository {

    private val deviceInfo = DeviceInfo(context)

    override fun refreshSettings(): Boolean {
        val body = deviceInfo.toSettingsRequest(clientUUID, config, termsAndConditions)
        val settings = controlServerClient.getSettings(body)
        settings.onSuccess {
            val uuid = settings.success.settings.first().uuid
            if (uuid != null && uuid.isNotBlank()) {
                clientUUID.value = uuid
            }

            controlServerSettings.filterDevices = it.settings.first().history?.devices ?: listOf()
            controlServerSettings.filterNetworkTypes = it.settings.first().history?.networks ?: listOf()

            val urls = settings.success.settings.first().urls
            if (urls != null) {
                controlServerSettings.controlServerV4Url = urls.ipv4OnlyControlServerUrl.removeProtocol()
                controlServerSettings.controlServerV6Url = urls.ipv6OnlyControlServerUrl.removeProtocol()
                controlServerSettings.ipV4CheckUrl = urls.ipv4CheckUrl.removeProtocol()
                controlServerSettings.ipV6CheckUrl = urls.ipv6CheckUrl.removeProtocol()
                controlServerSettings.openDataPrefix = urls.openDataPrefixUrl
                controlServerSettings.shareUrl = urls.shareUrl
                controlServerSettings.statisticsUrl = urls.statisticsUrl
                mapServerSettings.mapServerUrl = urls.mapServerUrl
            }
            val mapServer = settings.success.settings.first().mapServerSettings
            if (mapServer != null) {
                mapServerSettings.mapServerHost = mapServer.host
                mapServerSettings.mapServerPort = mapServer.port
                mapServerSettings.mapServerUseSsl = mapServer.ssl
            }
            val tac = settings.success.settings.first().termsAndConditions
            if (tac != null) {
                termsAndConditions.tacUrl = tac.url
                termsAndConditions.ndtTermsUrl = tac.ndtURL
                termsAndConditions.tacVersion = tac.version
            }
            val versions = settings.success.settings.first().versions
            if (versions != null) {
                controlServerSettings.controlServerVersion = versions.controlServerVersion
            }
            val history = settings.success.settings.first().history
            if (history != null) {
                historyFilterOptions.devices = history.devices?.toMutableSet() ?: mutableSetOf()
                historyFilterOptions.networks = history.networks?.toMutableSet() ?: mutableSetOf()
            }
            // todo: qostest types to DB
            // todo: servers to DB
        }
        return settings.ok
    }

    private fun String?.removeProtocol(): String? {
        this ?: return null
        return this.removePrefix("http://").removePrefix("https://")
    }
}
