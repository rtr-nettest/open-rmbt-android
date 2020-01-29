package at.specure.data.repository

import androidx.lifecycle.LiveData
import at.rmbt.client.control.BaseResponse
import at.rmbt.client.control.CapabilitiesBody
import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.QosTestResultDetailBody
import at.rmbt.client.control.ServerTestResultBody
import at.rmbt.client.control.TestResultDetailBody
import at.rmbt.util.Maybe
import at.specure.data.Classification
import at.specure.data.ClientUUID
import at.specure.data.CoreDatabase
import at.specure.data.entity.QoeInfoRecord
import at.specure.data.entity.QosCategoryRecord
import at.specure.data.entity.QosTestGoalRecord
import at.specure.data.entity.QosTestItemRecord
import at.specure.data.entity.TestResultDetailsRecord
import at.specure.data.entity.TestResultGraphItemRecord
import at.specure.data.entity.TestResultRecord
import at.specure.data.toModel
import at.specure.data.toModelList
import at.specure.data.toModels
import at.specure.data.toQoeModel
import at.specure.result.QoECategory
import at.specure.result.QoSCategory
import at.specure.util.exception.DataMissingException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.util.Locale

class TestResultsRepositoryImpl(
    db: CoreDatabase,
    private val clientUUID: ClientUUID,
    private val client: ControlServerClient
) : TestResultsRepository {

    private val qoeInfoDao = db.qoeInfoDao()
    private val qosCategoryDao = db.qosCategoryDao()
    private val qosTestGoalDao = db.qosTestGoalDao()
    private val qosTestItemDao = db.qosTestItemDao()
    private val testResultDao = db.testResultDao()
    private val testResultDetailsDao = db.testResultDetailsDao()
    private val testResultGraphItemDao = db.testResultGraphItemDao()

    override fun getQoEItems(testOpenUUID: String): LiveData<List<QoeInfoRecord>> {
        return qoeInfoDao.get(testOpenUUID)
    }

    override fun getServerTestResult(testUUID: String): LiveData<TestResultRecord?> {
        return testResultDao.get(testUUID)
    }

    override fun getServerTestResultDownloadGraphItems(openTestUUID: String): LiveData<List<TestResultGraphItemRecord>?> {
        return testResultGraphItemDao.getDownloadGraphLiveData(openTestUUID)
    }

    override fun getServerTestResultUploadGraphItems(openTestUUID: String): LiveData<List<TestResultGraphItemRecord>?> {
        return testResultGraphItemDao.getUploadGraphLiveData(openTestUUID)
    }

    override fun getServerTestResultPingGraphItems(openTestUUID: String): LiveData<List<TestResultGraphItemRecord>?> {
        return testResultGraphItemDao.getPingGraphLiveData(openTestUUID)
    }

    override fun getServerTestResultSignalGraphItems(openTestUUID: String): LiveData<List<TestResultGraphItemRecord>?> {
        return testResultGraphItemDao.getSignalGraphLiveData(openTestUUID)
    }

    override fun getTestDetailsResult(testUUID: String): LiveData<List<TestResultDetailsRecord>> = testResultDetailsDao.get(testUUID)

    override fun getQosTestCategoriesResult(testUUID: String): LiveData<List<QosCategoryRecord>> {
        return qosCategoryDao.get(testUUID)
    }

    override fun getQosItemsResult(testUUID: String, category: QoSCategory): LiveData<List<QosTestItemRecord>> {
        return qosTestItemDao.get(testUUID, category)
    }

    override fun getQosGoalsResult(testUUID: String, testItemId: Long): LiveData<List<QosTestGoalRecord>> {
        return qosTestGoalDao.get(testUUID, testItemId)
    }

    override fun loadTestDetailsResult(testUUID: String) = flow {
        val clientUUID = clientUUID.value
        if (clientUUID == null) {
            Timber.w("Unable to load data; client uuid is null")
            throw DataMissingException("ClientUUID is null")
        } else {
            val body = TestResultDetailBody(testUUID, clientUUID, Locale.getDefault().language)
            val result = client.getTestResultDetail(body)
            result.onSuccess {
                testResultDetailsDao.insert(it.toModelList(testUUID))
                emit(result.ok)
            }

            result.onFailure { throw it }
        }
    }

    private fun loadOpenDataTestResults(openTestUUID: String): Maybe<BaseResponse> {
        val detailedTestResults = client.getDetailedTestResults(openTestUUID)
        detailedTestResults.onSuccess { response ->
            testResultGraphItemDao.clearInsertItems(response.speedCurve.download.map {
                it.toModel(
                    openTestUUID,
                    TestResultGraphItemRecord.RESULT_GRAPH_ITEM_TYPE_DOWNLOAD
                )
            })

            testResultGraphItemDao.clearInsertItems(response.speedCurve.upload.map {
                it.toModel(
                    openTestUUID,
                    TestResultGraphItemRecord.RESULT_GRAPH_ITEM_TYPE_UPLOAD
                )
            })

            testResultGraphItemDao.clearInsertItems(response.speedCurve.ping.map { it.toModel(openTestUUID) })

            testResultGraphItemDao.clearInsertItems(response.speedCurve.signal.map { it.toModel(openTestUUID) })
        }
        return detailedTestResults
    }

    override fun loadTestResults(testUUID: String): Flow<Boolean> = flow {
        val clientUUID = clientUUID.value
        if (clientUUID == null) {
            Timber.w("Unable to update history client uuid is null")
            throw DataMissingException("ClientUUID is null")
        } else {
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
                qoeInfoDao.clearInsert(qoeRecords)
                getServerTestResult(testUUID)
                loadOpenDataTestResults(testResult.testOpenUUID)
                loadQosTestResults(testUUID, clientUUID)
                emit(true)
            }

            response.onFailure { throw it }
        }
    }

    private fun loadQosTestResults(testUUID: String, clientUUID: String): Maybe<BaseResponse> {
        val qosTestResults = client.getQosTestResultDetail(
            QosTestResultDetailBody(
                testUUID = testUUID,
                clientUUID = clientUUID,
                language = Locale.getDefault().language
            )
        )
        qosTestResults.onSuccess { response ->
            var failureCount = 0
            var successCount = 0
            response.qosResultDetails.forEach { result ->
                if (result.failureCount > 0) {
                    failureCount++
                } else {
                    successCount++
                }
            }

            val qosModelPair = response.toModels(testUUID, Locale.getDefault().language)
            qosModelPair.first.forEach {
                qosCategoryDao.clearQoSInsert(it)
            }
            qosTestItemDao.clearQosItemsInsert(qosModelPair.second)
            qosTestGoalDao.clearQosGoalsInsert(qosModelPair.third)

            val percentage: Float = (successCount.toFloat() / (successCount + failureCount).toFloat())
            qoeInfoDao.clearQoSInsert(
                QoeInfoRecord(
                    testUUID = testUUID,
                    category = QoECategory.QOE_QOS,
                    percentage = percentage,
                    info = "${(percentage * 100).toInt()}% ($successCount/${successCount + failureCount})",
                    classification = getQosClassification(percentage * 100f),
                    priority = 1
                )
            )
        }
        return qosTestResults
    }

    private fun getQosClassification(percentage: Float): Classification {
        return when {
            percentage.toInt() >= 100 -> Classification.EXCELLENT
            percentage > 95f -> Classification.GOOD
            percentage > 50f -> Classification.NORMAL
            else -> Classification.BAD
        }
    }
}