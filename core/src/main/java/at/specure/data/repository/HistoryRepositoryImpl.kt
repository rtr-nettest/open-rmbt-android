package at.specure.data.repository

import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.HistoryRequestBody
import at.rmbt.util.Maybe
import at.rmbt.util.io
import at.specure.config.Config
import at.specure.data.ClientUUID
import at.specure.data.HistoryFilterOptions
import at.specure.data.HistoryLoopMedian
import at.specure.data.dao.HistoryDao
import at.specure.data.entity.History
import at.specure.data.toCapabilitiesBody
import at.specure.data.toModelList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.util.Locale
import kotlin.math.ceil

class HistoryRepositoryImpl(
    private val historyDao: HistoryDao,
    private val config: Config,
    private val clientUUID: ClientUUID,
    private val client: ControlServerClient,
    private val settingsRepository: SettingsRepository,
    private val historyFilterOptions: HistoryFilterOptions
) : HistoryRepository {

    override val activeNetworksLiveData = historyFilterOptions.activeNetworksLiveData
    override val activeDevicesLiveData = historyFilterOptions.activeDevicesLiveData

    override val networksLiveData = historyFilterOptions.networksLiveData
    override val devicesLiveData = historyFilterOptions.devicesLiveData

    override val appliedFiltersLiveData = historyFilterOptions.appliedFiltersLiveData

    override fun loadHistoryItems(offset: Int, limit: Int, ignoreFilters: Boolean): Maybe<List<History>> {
        val clientUUID = clientUUID.value
        if (clientUUID == null) {
            Timber.w("Unable to update history client uuid is null")
            return Maybe(emptyList())
        }

        val body = HistoryRequestBody(
            clientUUID = clientUUID,
            offset = offset,
            limit = limit,
            capabilities = config.toCapabilitiesBody(),
            devices = if (ignoreFilters) null else historyFilterOptions.activeDevices?.toList(),
            networks = if (ignoreFilters) null else historyFilterOptions.activeNetworks?.toList(),
            language = Locale.getDefault().language
        )

        val response = client.getHistory(body)

        return response.map {
            val items = it.toModelList()
            if (offset == 0) {
                settingsRepository.refreshSettings()
                historyDao.clear()
            }
            historyDao.insert(items)
            Timber.i("history offset: $offset limit: $limit loaded: ${it.history?.size}")
            items
        }
    }

    override fun loadHistoryItems(offset: Int, limit: Int): Maybe<List<History>> {
        return loadHistoryItems(offset, limit, false)
    }

    /**
     * load loop history items present in the history table (watch out for filters)
     */
    override fun loadLoopHistoryItems(loopUuid: String): List<History> {
        return historyDao.getItemByLoopUUID(loopUuid)
    }

    /**
     * compute median values for particular loop measurement from currently available results in the history (watch out for filters)
     */
    override fun loadLoopMedianValues(loopUuid: String): Flow<HistoryLoopMedian?> = flow {
        val historyItems = historyDao.getItemByLoopUUID(loopUuid)
        if (historyItems.isEmpty()) {
            emit(null)
            Timber.e("history items: No loop items")
        }
        val pingList = mutableListOf<Float>()
        val jitterList = mutableListOf<Float>()
        val packetLoss = mutableListOf<Float>()
        val downloadList = mutableListOf<Float>()
        val uploadList = mutableListOf<Float>()
        val qosList = mutableListOf<Float>()
        historyItems.forEach { historyItem ->
            historyItem.ping.toFloatOrNull()?.let { ping ->
                pingList.add(ping)
            }
//            todo: prepared for ONT CS
//            historyItem.jitter.toFloatOrNull()?.let { jitter ->
//                jitterList.add(jitter)
//            }
//            historyItem.packetLoss.toFloatOrNull()?.let { packetLoss ->
//                packetLossList.add(packetLoss)
//            }
            historyItem.speedDownload.toFloatOrNull()?.let { downloadSpeed ->
                downloadList.add(downloadSpeed)
            }
            historyItem.speedUpload.toFloatOrNull()?.let { uploadSpeed ->
                uploadList.add(uploadSpeed)
            }
        }
        Timber.d("history items: ${historyItems.size}")
        historyItems.forEach { Timber.d("history item from ${it.timeString} with ${it.ping}, ${it.speedDownload}, ${it.speedUpload}") }
        Timber.d("history median: ${median(pingList)}, ${median(downloadList)}, ${median(uploadList)}")
        emit(
            HistoryLoopMedian(
                loopUuid = loopUuid,
                pingMedian = median(pingList),
                packetLossMedian = median(packetLoss),
                jitterMedian = median(jitterList),
                downloadMedian = median(downloadList),
                uploadMedian = median(uploadList),
                qosMedian = median(qosList)
            )
        )
    }

    private fun median(floatList: List<Float>): Float {
        if (floatList.isEmpty()) {
            return 0f
        }

        val sortedFloatList = floatList.sorted()
        val halfIndex = (ceil((sortedFloatList.size / 2f).toDouble()) - 1).toInt()
        return if (sortedFloatList.size % 2 == 0) {
            (sortedFloatList[halfIndex] + sortedFloatList[halfIndex + 1]) / 2f
        } else {
            sortedFloatList[halfIndex]
        }
    }

    override fun saveFiltersNetwork(selected: Set<String>) {
        val data = if (selected.isEmpty()) null else selected
        historyFilterOptions.activeNetworks = data
        updateAppliedFilters()
    }

    override fun saveFiltersDevices(selected: Set<String>) {
        val data = if (selected.isEmpty()) null else selected
        historyFilterOptions.activeDevices = data
        updateAppliedFilters()
    }

    override fun removeFromFilters(value: String) {
        if (historyFilterOptions.activeDevices?.contains(value) == true) {
            with(historyFilterOptions.activeDevices?.filter { it != value }) {
                if (isNullOrEmpty()) {
                    historyFilterOptions.activeDevices = null
                } else {
                    historyFilterOptions.activeDevices = this!!.toSet()
                }
            }
        } else if (historyFilterOptions.activeNetworks?.contains(value) == true) {
            with(historyFilterOptions.activeNetworks?.filter { it != value }) {
                if (isNullOrEmpty()) {
                    historyFilterOptions.activeNetworks = null
                } else {
                    historyFilterOptions.activeNetworks = this!!.toSet()
                }
            }
        }
        updateAppliedFilters()
    }

    override fun getActiveDevices(): Set<String>? = historyFilterOptions.activeDevices

    override fun getActiveNetworks(): Set<String>? = historyFilterOptions.activeNetworks

    override fun getNetworks(): Set<String>? = historyFilterOptions.networks

    override fun getDevices(): Set<String>? = historyFilterOptions.devices

    private fun updateAppliedFilters() {
        historyFilterOptions.appliedFilters = mutableSetOf<String>().apply {
            historyFilterOptions.activeDevices?.let { addAll(it) }
            historyFilterOptions.activeNetworks?.let { addAll(it) }
        }
    }

    override fun cleanHistory() = io {
        historyDao.clearHistory()
    }
}