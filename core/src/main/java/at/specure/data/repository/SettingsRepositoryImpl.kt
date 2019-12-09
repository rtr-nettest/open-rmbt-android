package at.specure.data.repository

import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.SettingsRequestBody
import at.specure.data.ClientUUID
import at.specure.data.ControlServerSettings
import at.specure.data.HistoryFilterOptions
import at.specure.data.MapServerSettings
import at.specure.data.TermsAndConditions

class SettingsRepositoryImpl(
    private val controlServerClient: ControlServerClient,
    private val clientUUID: ClientUUID,
    private val controlServerSettings: ControlServerSettings,
    private val mapServerSettings: MapServerSettings,
    private val termsAndConditions: TermsAndConditions,
    private val historyFilterOptions: HistoryFilterOptions
) : SettingsRepository {

    override fun refreshSettings(): Boolean {
        val settings = controlServerClient.getSettings(SettingsRequestBody(uuid = clientUUID.value ?: ""))
        if (settings.ok) {
            val uuid = settings.success.settings.first().uuid
            if (uuid != null && uuid.isNotBlank()) {
                clientUUID.value = uuid
            }
            val urls = settings.success.settings.first().urls
            if (urls != null) {
                controlServerSettings.controlServerV4Url = urls.ipv4OnlyControlServerUrl
                controlServerSettings.controlServerV6Url = urls.ipv6OnlyControlServerUrl
                controlServerSettings.ipV4CheckUrl = urls.ipv4CheckUrl
                controlServerSettings.ipV6CheckUrl = urls.ipv6CheckUrl
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
                historyFilterOptions.valueOptionsFilterDevices = history.devices?.toSet()
                historyFilterOptions.valueOptionsFilterNetworks = history.networks?.toSet()
            }
            // todo: qostest types to DB
            // todo: servers to DB
        }
        return settings.ok
    }
}
