package at.specure.data.repository

import androidx.lifecycle.LiveData
import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.HistoryONTRequestBody
import at.rmbt.client.control.HistoryRequestBody
import at.rmbt.util.Maybe
import at.rmbt.util.io
import at.specure.config.Config
import at.specure.data.Classification
import at.specure.data.ClientUUID
import at.specure.data.HistoryFilterOptions
import at.specure.data.HistoryLoopMedian
import at.specure.data.dao.HistoryDao
import at.specure.data.dao.HistoryMedianDao
import at.specure.data.dao.QoeInfoDao
import at.specure.data.entity.History
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.toCapabilitiesBody
import at.specure.data.toModelList
import at.specure.result.QoECategory
import at.specure.util.extractFloatValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.util.Locale
import kotlin.math.ceil

class HistoryRepositoryImpl(
    private val historyDao: HistoryDao,
    private val historyMedianDao: HistoryMedianDao,
    private val config: Config,
    private val clientUUID: ClientUUID,
    private val client: ControlServerClient,
    private val settingsRepository: SettingsRepository,
    private val historyFilterOptions: HistoryFilterOptions,
    private val qoeInfoDao: QoeInfoDao
) : HistoryRepository {

    override val activeNetworksLiveData = historyFilterOptions.activeNetworksLiveData
    override val activeDevicesLiveData = historyFilterOptions.activeDevicesLiveData

    override val networksLiveData = historyFilterOptions.networksLiveData
    override val devicesLiveData = historyFilterOptions.devicesLiveData

    override val appliedFiltersLiveData = historyFilterOptions.appliedFiltersLiveData

    override fun loadHistoryItems(
        offset: Int,
        limit: Int,
        ignoreFilters: Boolean
    ): Maybe<List<History?>?> {
        val clientUUID = clientUUID.value
        if (clientUUID == null) {
            Timber.w("Unable to update history client uuid is null")
            return Maybe(emptyList())
        }

        val useONTApiVersion = config.headerValue.isNotEmpty()
        return if (useONTApiVersion) {
            loadHistoryONT(clientUUID, offset, limit, ignoreFilters)
        } else {
            loadHistoryRTR(clientUUID, offset, limit, ignoreFilters)
        }
    }

    private fun loadHistoryRTR(
        clientUUID: String,
        offset: Int,
        limit: Int,
        ignoreFilters: Boolean
    ): Maybe<List<History?>?> {
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
            val items = it?.toModelList()
            if (offset == 0) {
                settingsRepository.refreshSettings()
                historyDao.clear()
            }
            if (items != null) {
                historyDao.insert(items)
            }
            Timber.i("history offset: $offset limit: $limit loaded: ${it.history?.size}")
            items
        }
    }

    private fun loadHistoryONT(
        clientUUID: String,
        offset: Int,
        limit: Int,
        ignoreFilters: Boolean
    ): Maybe<List<History>> {
        val body = HistoryONTRequestBody(
            clientUUID = clientUUID,
            page = (offset / limit).toLong(),
            size = limit.toLong(),
            devices = if (ignoreFilters) null else historyFilterOptions.activeDevices?.toList(),
            networks = if (ignoreFilters) null else historyFilterOptions.activeNetworks?.toList()
        )
        val response = client.getHistoryONT(body, limit.toLong(), (offset / limit).toLong())
        return response.map {
            val items = it.toModelList()
            if (offset == 0) {
                settingsRepository.refreshSettings()
                historyDao.clear()
            }
            historyDao.insert(items)
            val loopMeasurementsMap = mutableMapOf<String, MutableList<History>?>()
            items.forEach { historyItem ->
                if (historyItem.loopUUID != null) {
                    var currentList: MutableList<History>? = loopMeasurementsMap[historyItem.loopUUID]
                    if (currentList == null) {
                        currentList = mutableListOf<History>()
                    }
                    currentList.add(historyItem)
                    loopMeasurementsMap[historyItem.loopUUID] = currentList
                }
            }
            loopMeasurementsMap.forEach { loopList ->
                extractLoopMedianValues(loopList.value, loopList.key)
            }
            Timber.i("history offset: $offset limit: $limit loaded: ${it.history?.historyList?.size}")
            items
        }
    }

    override fun loadHistoryItems(offset: Int, limit: Int): Maybe<List<History?>?> {
        return loadHistoryItems(offset, limit, false)
    }

    /**
     * load loop history items present in the history table (watch out for filters)
     */
    override fun loadLoopHistoryItems(loopUuid: String): Flow<List<History>?> = flow {
        val loopItems = historyDao.getItemByLoopUUID(loopUuid)
        Timber.i("history loaded: ${loopItems.size}")
        emit(loopItems)
    }

    /**
     * compute median values for particular loop measurement from currently available results in the history (watch out for filters)
     */
    override fun loadLoopMedianValues(loopUuid: String): Flow<HistoryLoopMedian?> = flow {
        val historyItems = historyDao.getItemByLoopUUID(loopUuid)
        Timber.d("history items: for $loopUuid")
        if (historyItems.isEmpty() == true) {
            emit(null)
            Timber.e("history items: No loop items")
        }
        extractLoopMedianValues(historyItems, loopUuid)
    }

    private fun extractLoopMedianValues(historyItems: List<History>?, loopUuid: String) {
        val pingList = mutableListOf<Float>()
        val jitterList = mutableListOf<Float>()
        val packetLossList = mutableListOf<Float>()
        val downloadList = mutableListOf<Float>()
        val uploadList = mutableListOf<Float>()
        val qosList = mutableListOf<Float>()
        historyItems?.forEach { historyItem ->
            historyItem.ping.extractFloatValue()?.let { ping ->
                pingList.add(ping)
            }
            historyItem.jitterMillis?.extractFloatValue()?.let { jitter ->
                jitterList.add(jitter)
            }
            historyItem.packetLossPercents?.extractFloatValue()?.let { packetLoss ->
                packetLossList.add(packetLoss)
            }
            historyItem.qos?.extractFloatValue()?.let { qos ->
                qosList.add(qos)
            }
            historyItem.speedDownload.extractFloatValue()?.let { downloadSpeed ->
                downloadList.add(downloadSpeed)
            }
            historyItem.speedUpload.extractFloatValue()?.let { uploadSpeed ->
                uploadList.add(uploadSpeed)
            }
        }
        Timber.d("history items: ${historyItems?.size}")
        historyItems?.forEach { Timber.d("history item from ${it.timeString} with ${it.ping}, ${it.speedDownload}, ${it.speedUpload}") }
        Timber.d("history median: ${median(pingList)}, ${median(downloadList)}, ${median(uploadList)}")
        val medianQos = median(qosList)
        historyMedianDao.insert(
            HistoryLoopMedian(
                loopUuid = loopUuid,
                pingMedianMillis = median(pingList) ?: -1f,
                packetLossMedian = median(packetLossList) ?: -1f,
                jitterMedianMillis = median(jitterList) ?: -1f,
                downloadMedianMbps = median(downloadList) ?: -1f,
                uploadMedianMbps = median(uploadList) ?: -1f,
                qosMedian = median(qosList)
            )
        )
        medianQos?.let {
            qoeInfoDao.clearQoSInsert(
                QoeInfoRecord(
                    testUUID = loopUuid,
                    category = QoECategory.QOE_QOS,
                    classification = Classification.NONE,
                    percentage = it,
                    info = "$it%",
                    priority = -1
                )
            )
        }
    }

    override fun getLoopMedianValues(loopUuid: String): LiveData<HistoryLoopMedian?> {
        return historyMedianDao.getItemByLoopUUID(loopUuid)
    }

    /**
     * load loop history items present in the history table (watch out for filters)
     */
    override fun getLoopHistoryItems(loopUuid: String): LiveData<List<History>?> {
        val loopItems: LiveData<List<History>?> = historyDao.getItemByLoopUUIDLiveData(loopUuid)
        Timber.i("history loaded: ${loopItems.value?.size}")
        return loopItems
    }

    private fun median(floatList: List<Float>): Float? {
        if (floatList.isEmpty()) {
            return null
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

    override fun getLoadedHistoryItems(limit: Int): LiveData<List<History>?> {
        return historyDao.getLoadedItemsLiveData(limit)
    }

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