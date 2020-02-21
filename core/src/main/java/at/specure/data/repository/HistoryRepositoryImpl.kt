package at.specure.data.repository

import androidx.paging.DataSource
import androidx.paging.PagedList
import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.HistoryRequestBody
import at.rmbt.util.Maybe
import at.rmbt.util.exception.HandledException
import at.rmbt.util.io
import at.specure.config.Config
import at.specure.data.ClientUUID
import at.specure.data.HistoryFilterOptions
import at.specure.data.dao.HistoryDao
import at.specure.data.entity.HistoryContainer
import at.specure.data.toCapabilitiesBody
import at.specure.data.toModelList
import timber.log.Timber
import java.util.Locale

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

    override fun getHistorySource(): DataSource.Factory<Int, HistoryContainer> = historyDao.getHistorySource()

    private fun loadItems(offset: Int, limit: Int): Maybe<Boolean> {
        val clientUUID = clientUUID.value
        if (clientUUID == null) {
            Timber.w("Unable to update history client uuid is null")
            return Maybe(false)
        }

        val body = HistoryRequestBody(
            clientUUID = clientUUID,
            offset = offset,
            limit = limit,
            capabilities = config.toCapabilitiesBody(),
            devices = historyFilterOptions.activeDevices?.toList(),
            networks = historyFilterOptions.activeNetworks?.toList(),
            language = Locale.getDefault().language
        )

        val response = client.getHistory(body)

        response.onSuccess {
            if (offset == 0) {
                historyDao.clear()
            }
            historyDao.insert(it.toModelList())
            Timber.i("history offset: $offset limit: $limit loaded: ${it.history.size}")
        }

        return response.map { response.ok }
    }

    override fun boundaryCallback(
        limit: Int,
        onLoadingCallback: (Boolean) -> Unit,
        onErrorCallback: (HandledException) -> Unit
    ) = HistoryBoundaryCallback(limit, onLoadingCallback, onErrorCallback)

    inner class HistoryBoundaryCallback(
        private val limit: Int,
        private val onLoadingCallback: ((Boolean) -> Unit),
        private val onErrorCallback: ((HandledException) -> Unit)
    ) :
        PagedList.BoundaryCallback<HistoryContainer>() {

        override fun onZeroItemsLoaded() = io {
            loadContent()
        }

        override fun onItemAtEndLoaded(itemAtEnd: HistoryContainer) {
            loadContent()
        }

        private fun loadContent() = io {
            onLoadingCallback.invoke(true)
            val count = historyDao.getItemsCount()
            val result = loadItems(count, limit)
            onLoadingCallback.invoke(false)
            result.onFailure {
                onErrorCallback.invoke(it)
            }
        }
    }

    override fun refreshHistory(limit: Int, onLoadingCallback: (Boolean) -> Unit, onErrorCallback: (HandledException) -> Unit) = io {
        onLoadingCallback.invoke(true)
        loadItems(0, limit).onFailure(onErrorCallback)
        onLoadingCallback.invoke(false)
        settingsRepository.refreshSettings()
    }

    override fun clearHistory() {
        historyDao.clear()
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
}