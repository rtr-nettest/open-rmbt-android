package at.rtr.rmbt.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import at.rmbt.util.exception.HandledException
import at.rtr.rmbt.android.config.AppConfig
import at.rtr.rmbt.android.ui.viewstate.ResultViewState
import at.specure.data.ControlServerSettings
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.QosCategoryRecord
import at.specure.data.entity.TestResultDetailsRecord
import at.specure.data.entity.TestResultRecord
import at.specure.data.repository.TestResultsRepository
import at.specure.util.download.FileDownloadData
import at.specure.util.download.FileDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

class ResultViewModel @Inject constructor(
    private val appConfig: AppConfig,
    private val testResultsRepository: TestResultsRepository,
    private val fileDownloader: FileDownloader,
    private val controlServerSettings: ControlServerSettings
) : BaseViewModel() {

    val state = ResultViewState(appConfig)

    private var _testResultLiveData: LiveData<TestResultRecord?>? = null
    val testServerResultLiveData: LiveData<TestResultRecord?>
        get() {
            if (_testResultLiveData == null) {
                _testResultLiveData = testResultsRepository.getServerTestResult(state.testUUID)
            }
            return this._testResultLiveData!!
        }

    val qoeResultLiveData: LiveData<List<QoeInfoRecord>>
        get() = testResultsRepository.getQoEItems(state.testUUID)

    val testResultDetailsLiveData: LiveData<List<TestResultDetailsRecord>>
        get() = testResultsRepository.getTestDetailsResult(state.testUUID)

    val qosCategoryResultLiveData: LiveData<List<QosCategoryRecord>>
        get() = testResultsRepository.getQosTestCategoriesResult(state.testUUID)

    val loadingLiveData: LiveData<Boolean>
        get() = _loadingLiveData

    private val _loadingLiveData = MutableLiveData<Boolean>()

    val downloadFileLiveData: LiveData<FileDownloadData>
        get() = _downloadFileLiveData

    private val _downloadFileLiveData = MutableLiveData<FileDownloadData>()

    init {
        addStateSaveHandler(state)
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
    }

    fun loadTestResults() = launch {
        testResultsRepository.loadTestResults(state.testUUID).zip(
            testResultsRepository.loadTestDetailsResult(state.testUUID)
        ) { a, b -> a && b }
            .flowOn(Dispatchers.IO)
            .catch {
                if (it is HandledException) {
                    emit(false)
                    postError(it)
                } else {
                    throw it
                }
            }
            .collect {
                _loadingLiveData.postValue(it)
            }
    }

    fun downloadFile(format: String) {
        val languageCode = Locale.getDefault().toLanguageTag().split("-")[0]
        val statisticServerUrl = controlServerSettings.statisticsMasterServerUrl ?: "https://m-cloud.netztest.at/RMBTStatisticServer"
        val url =
            if (format == "pdf") "$statisticServerUrl/export/pdf/$languageCode"
            else "$statisticServerUrl/opentests/search"
        this.testServerResultLiveData.value?.testOpenUUID?.let { openUUID ->
            viewModelScope.launch {
                fileDownloader.downloadFile(
                    urlString = url,
                    openUuid = openUUID,
                    format = format
                )
            }
        }

    }
}