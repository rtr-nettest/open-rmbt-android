package at.specure.data.repository

import android.content.Context
import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.TermsAndConditionsSettings
import at.specure.config.Config
import at.specure.data.ClientUUID
import at.specure.data.ControlServerSettings
import at.specure.data.HistoryFilterOptions
import at.specure.data.MeasurementServers
import at.specure.data.TermsAndConditions
import at.specure.data.dao.TacDao
import at.specure.data.entity.TacRecord
import at.specure.data.toSettingsRequest
import at.specure.test.DeviceInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

class SettingsRepositoryImpl(
    private val context: Context,
    private val controlServerClient: ControlServerClient,
    private val clientUUID: ClientUUID,
    private val controlServerSettings: ControlServerSettings,
    private val termsAndConditions: TermsAndConditions,
    private val measurementsServers: MeasurementServers,
    private val historyFilterOptions: HistoryFilterOptions,
    private val config: Config,
    private val tacDao: TacDao
) : SettingsRepository {

    private val deviceInfo: DeviceInfo
        get() = DeviceInfo(context)

    override fun refreshSettings(): Boolean {
        val body = deviceInfo.toSettingsRequest(clientUUID, config, termsAndConditions)
        // we must remove ipv4 url before we want to check settings, because settings request should go to the original URL
        controlServerSettings.controlServerV4Url = null
        controlServerSettings.controlServerV6Url = null
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
            }
            val mapServer = settings.success.settings.first().mapServerSettings
            if (mapServer != null && !config.mapServerOverrideEnabled) {
                mapServer.host?.let { config.mapServerHost = it }
                mapServer.port?.let { config.mapServerPort = it }
                mapServer.ssl?.let { config.mapServerUseSSL = it }
            }
            val tac = settings.success.settings.first().termsAndConditions
            updateTermsAndConditions(tac)
            val versions = settings.success.settings.first().versions
            if (versions != null) {
                controlServerSettings.controlServerVersion = versions.controlServerVersion
            }
            val history = settings.success.settings.first().history
            if (history != null) {
                historyFilterOptions.devices = history.devices?.toMutableSet() ?: mutableSetOf()
                historyFilterOptions.networks = history.networks?.toMutableSet() ?: mutableSetOf()
            }
            measurementsServers.measurementServers = settings.success.settings.first().servers
            // todo: qostest types to DB
        }
        return settings.ok
    }

    private fun updateTermsAndConditions(tac: TermsAndConditionsSettings?) = tac?.let { terms ->
        if (termsAndConditions.tacUrl != terms.url) {
            termsAndConditions.tacUrl = terms.url
            termsAndConditions.tacAccepted = false
        }
        termsAndConditions.ndtTermsUrl = terms.ndtURL
        if (termsAndConditions.tacVersion != terms.version) {
            termsAndConditions.tacVersion = terms.version
            termsAndConditions.tacAccepted = false
            terms.url?.let { url ->
                tacDao.deleteTermsAndCondition(url)
            }
        }
    }

    private fun String?.removeProtocol(): String? {
        this ?: return null
        return this.removePrefix("http://").removePrefix("https://")
    }

    override fun getTermsAndConditions(): Flow<String> = flow {
        val url = termsAndConditions.tacUrl
        if (url == null) {
            getEmbeddedTac()?.let { text ->
                emit(text)
            }
        } else {
            (getOrFetchTac(url) ?: getEmbeddedTac())?.let { text ->
                emit(text)
            }
        }
    }

    private fun getEmbeddedTac(): String? = try {
        BufferedReader(InputStreamReader(termsAndConditions.localVersion)).readText()
    } catch (ex: IOException) {
        Timber.w(ex, "Failed to load embedded TaC")
        null
    }

    private fun getOrFetchTac(url: String): String? {
        val record = tacDao.loadTermsAndConditions(url)
        return record?.content
            ?: try {
                val stream = URL(url).openStream()
                val text = BufferedReader(InputStreamReader(stream)).readText()
                val newRecord = TacRecord(url, text)
                tacDao.clearInsertItems(newRecord)
                text
            } catch (ex: IOException) {
                Timber.w(ex, "Failed to load TaC")
                null
            }
    }
}
