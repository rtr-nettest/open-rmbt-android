package at.rtr.rmbt.android.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import at.rmbt.util.io
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.ui.viewstate.HistoryDownloadViewState
import at.specure.data.ControlServerSettings
import at.specure.data.entity.History
import at.specure.data.repository.HistoryRepository
import at.specure.util.download.FileDownloadData
import at.specure.util.download.FileDownloader
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

class HistoryDownloadViewModel @Inject constructor(
    context: Context,
    private val fileDownloader: FileDownloader,
    private val repository: HistoryRepository,
    private val controlServerSettings: ControlServerSettings,
) : BaseViewModel() {

    private val fileName = context.getString(R.string.history_filename)

    val state = HistoryDownloadViewState()

    val historyItemsLiveData: LiveData<List<History>?> = repository.getLoadedHistoryItems(100)

    val downloadFileLiveData: LiveData<FileDownloadData>
        get() = _downloadFileLiveData

    private val _downloadFileLiveData = MutableLiveData<FileDownloadData>()

    init {
        addStateSaveHandler(state)

        loadData()

        this.viewModelScope.launch((CoroutineName("HistoryDownloadViewModelInit"))) {
            fileDownloader.downloadStateFlow.collect { downloadState ->
                when (downloadState) {
                    is FileDownloader.DownloadState.Initial -> {
                        state.isDownloadingLiveData.set(false)
                        _downloadFileLiveData.postValue(FileDownloadData(null, null, null))
                    }

                    is FileDownloader.DownloadState.Downloading -> {
                        state.isDownloadingLiveData.set(true)
                        _downloadFileLiveData.postValue(
                            FileDownloadData(
                                null,
                                downloadState.progress,
                                null
                            )
                        )
                    }

                    is FileDownloader.DownloadState.Success -> {
                        state.isDownloadingLiveData.set(false)
                        // Download completed successfully
                        val downloadedFile = downloadState.file
                        // Open the downloaded PDF file
                        _downloadFileLiveData.postValue(FileDownloadData(downloadState.file, 100, null))
                        fileDownloader.openFile(downloadedFile) { exception ->
                            _downloadFileLiveData.postValue(
                                FileDownloadData(
                                    downloadState.file,
                                    100,
                                    exception.message
                                )
                            )
                        }
                    }

                    is FileDownloader.DownloadState.Error -> {
                        state.isDownloadingLiveData.set(false)
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
    }

    private fun loadData() = io {
        state.isHistoryEmpty.set(historyItemsLiveData.value.isNullOrEmpty())
    }
    fun downloadFile(format: String) {
        viewModelScope.launch(CoroutineName("Download file coroutine")) {
            val openUuids = if (historyItemsLiveData.value.isNullOrEmpty()) {
                emptyList<String>()
            } else {
                historyItemsLiveData.value?.map { historyItems ->
                    historyItems.openTestUUID
                }
            }
            val languageCode = Locale.getDefault().toLanguageTag().split("-")[0]
            val statisticServerUrl = controlServerSettings.statisticsMasterServerUrl
                ?: "https://m-cloud.netztest.at/RMBTStatisticServer"
            val url =
                if (format == "pdf") "$statisticServerUrl/export/pdf/$languageCode"
                else "$statisticServerUrl/opentests/search"
            openUuids?.let { openUuids ->
                fileDownloader.downloadFile(
                    urlString = url,
                    openUuid = openUuids.joinToString(","),
                    format = format,
                    fileName = fileName
                )
            }
        }
    }


}