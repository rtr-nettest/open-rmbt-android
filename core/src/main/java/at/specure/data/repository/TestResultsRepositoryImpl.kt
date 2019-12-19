package at.specure.data.repository

import androidx.lifecycle.LiveData
import at.rmbt.client.control.BaseResponse
import at.rmbt.client.control.CapabilitiesBody
import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.ServerTestResultBody
import at.rmbt.util.Maybe
import at.rmbt.util.io
import at.specure.data.ClientUUID
import at.specure.data.CoreDatabase
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.TestResultRecord
import at.specure.data.toModel
import at.specure.data.toQoeModel
import timber.log.Timber
import java.util.Locale

class TestResultsRepositoryImpl(
    db: CoreDatabase,
    private val clientUUID: ClientUUID,
    private val client: ControlServerClient
) : TestResultsRepository {

    private val qoeInfoDao = db.qoeInfoDao()
    private val testResultDao = db.testResultDao()

    override fun getQoEItems(testOpenUUID: String): LiveData<List<QoeInfoRecord>> {
        return qoeInfoDao.get(testOpenUUID)
    }

    override fun getServerTestResult(testUUID: String): LiveData<TestResultRecord?> {
        return testResultDao.get(testUUID)
    }

    fun loadOpenDataTestResults(openTestUUID: String): Maybe<BaseResponse> {
        val detailedTestResults = client.getDetailedTestResults(openTestUUID)
        detailedTestResults.onSuccess {
            // TODO: save graphs
        }
        return detailedTestResults
    }

    override fun loadTestResults(testUUID: String, callBack: (Maybe<Boolean>) -> Unit) = io {
        val clientUUID = clientUUID.value
        if (clientUUID == null) {
            Timber.w("Unable to update history client uuid is null")
            callBack.invoke(Maybe(false))
            return@io
        }

        val response = client.getTestResult(
            ServerTestResultBody(
                testUUID = testUUID,
                clientUUID = clientUUID,
                language = Locale.getDefault().language,
                capabilities = CapabilitiesBody()
            )
        )

        response.onSuccess {
            val testResult = it.toModel(testUUID)
            val qoeRecords = it.toQoeModel(testUUID)

            testResultDao.insert(testResult)
            qoeInfoDao.insert(qoeRecords)
            getServerTestResult(testUUID)
        }
        callBack.invoke(response.map { response.ok })
    }
}