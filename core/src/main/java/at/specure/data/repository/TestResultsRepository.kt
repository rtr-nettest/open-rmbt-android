package at.specure.data.repository

import androidx.lifecycle.LiveData
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.TestResultDetailsRecord
import at.specure.data.entity.TestResultGraphItemRecord
import at.specure.data.entity.TestResultRecord
import kotlinx.coroutines.flow.Flow

interface TestResultsRepository {

    fun getQoEItems(testOpenUUID: String): LiveData<List<QoeInfoRecord>>

    fun getServerTestResult(testUUID: String): LiveData<TestResultRecord?>

    fun loadTestResults(testUUID: String): Flow<Boolean>

    fun loadTestDetailsResult(testUUID: String): Flow<Boolean>

    fun getTestDetailsResult(testUUID: String): LiveData<List<TestResultDetailsRecord>>

    fun getServerTestResultDownloadGraphItems(openTestUUID: String): LiveData<List<TestResultGraphItemRecord>?>

    fun getServerTestResultUploadGraphItems(openTestUUID: String): LiveData<List<TestResultGraphItemRecord>?>

    fun getServerTestResultPingGraphItems(openTestUUID: String): LiveData<List<TestResultGraphItemRecord>?>

    fun getServerTestResultSignalGraphItems(openTestUUID: String): LiveData<List<TestResultGraphItemRecord>?>
}