package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.paging.PagedList
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.ui.viewstate.HistoryViewState
import at.specure.data.entity.HistoryContainer
import at.specure.data.repository.HistoryLoader
import at.specure.data.repository.HistoryRepository
import at.specure.util.download.FileDownloadData
import at.specure.util.download.FileDownloader
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
    private val fileDownloader: FileDownloader,
    private val repository: HistoryRepository,
    private val loader: HistoryLoader
) : BaseViewModel() {

    private val _isLoadingLiveData = MutableLiveData<Boolean>()
    private var _historyLiveData: LiveData<PagedList<HistoryContainer>> = MutableLiveData<PagedList<HistoryContainer>>()

    val state = HistoryViewState()

    val isLoadingLiveData: LiveData<Boolean>
        get() = _isLoadingLiveData

    val historyLiveData: LiveData<PagedList<HistoryContainer>>
        get() {
            _historyLiveData = loader.historyLiveData
            return _historyLiveData
        }

    val downloadFileLiveData: LiveData<FileDownloadData>
        get() = _downloadFileLiveData

    private val _downloadFileLiveData = MutableLiveData<FileDownloadData>()
    init {
        addStateSaveHandler(state)

        val isLoadingChannel = Channel<Boolean>()
        launch {
            isLoadingChannel.consumeAsFlow()
                .debounce(200)
                .collect {
                    _isLoadingLiveData.postValue(it)
                }
        }

        val errorChannel = Channel<HandledException>()
        launch {
            errorChannel.consumeAsFlow()
                .collect {
                    postError(it)
                }
        }

        this.viewModelScope.launch {
            fileDownloader.downloadStateFlow.collect { state ->
                when (state) {
                    is FileDownloader.DownloadState.Initial -> {
                        _downloadFileLiveData.postValue(FileDownloadData(null, null, null))
                    }

                    is FileDownloader.DownloadState.Downloading -> {
                        _downloadFileLiveData.postValue(
                            FileDownloadData(
                                null,
                                state.progress,
                                null
                            )
                        )
                    }

                    is FileDownloader.DownloadState.Success -> {
                        // Download completed successfully
                        val downloadedFile = state.file
                        // Open the downloaded PDF file
                        _downloadFileLiveData.postValue(FileDownloadData(state.file, 100, null))
                        fileDownloader.openFile(downloadedFile) { exception ->
                            _downloadFileLiveData.postValue(
                                FileDownloadData(
                                    state.file,
                                    100,
                                    exception.message
                                )
                            )
                        }
                    }

                    is FileDownloader.DownloadState.Error -> {
                        _downloadFileLiveData.postValue(
                            FileDownloadData(
                                null,
                                null,
                                "ERROR_DOWNLOAD"
                            )
                        )
                    }
                }
            }
        }

        loader.isLoadingChannel = isLoadingChannel
        loader.errorChannel = errorChannel
    }

    val activeFiltersLiveData = repository.appliedFiltersLiveData

    init {
        addStateSaveHandler(state)
    }

    fun refreshHistory() {
        loader.refresh()
    }

    fun removeFromFilters(value: String) {
        repository.removeFromFilters(value)
        refreshHistory()
    }

    fun downloadFile(format: String) {
        val openUuids = if (historyLiveData.value.isNullOrEmpty()) {
            emptyList<String>()
        } else {
            historyLiveData.value?.flatMap { historyContainer ->
                    historyContainer.items.map { it.openTestUUID }
            }
        }
        val languageCode = Locale.getDefault().toLanguageTag().split("-")[0]
        val url =
            if (format == "pdf") "https://m-cloud.netztest.at/RMBTStatisticServer/export/pdf/$languageCode"
            else "https://m-cloud.netztest.at/RMBTStatisticServer/opentests/search"
        openUuids?.let { openUuids ->
            viewModelScope.launch {
                fileDownloader.downloadFile(
                    urlString = url,
                    openUuid = openUuids.joinToString(","),
                    format = format
                )
            }
        }
    }
}