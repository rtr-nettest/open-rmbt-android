package at.specure.data.repository

import androidx.lifecycle.LiveData
import at.rmbt.client.control.BaseResponse
import at.rmbt.client.control.CapabilitiesBody
import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.QosTestResultDetailBody
import at.rmbt.client.control.ServerTestResultBody
import at.rmbt.client.control.TestResultDetailBody
import at.rmbt.util.Maybe
import at.specure.config.Config
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
    private val client: ControlServerClient,
    private val config: Config
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

    override fun getGraphDataLiveData(testUUID: String, type: TestResultGraphItemRecord.Type): LiveData<List<TestResultGraphItemRecord>> =
        testResultGraphItemDao.getGraphDataLiveData(testUUID, type.typeValue)

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

    private fun loadOpenDataTestResults(openTestUUID: String, testUUID: String) {
        val useONTApiVersion = config.headerValue.isNotEmpty()
        if (useONTApiVersion) {
            loadGraphResults(openTestUUID, testUUID)
        } else {
            loadOpendataResults(openTestUUID, testUUID)
        }
    }

    private fun loadOpendataResults(openTestUUID: String, testUUID: String) {
        val detailedTestResults = client.getDetailedTestResults(openTestUUID)
        detailedTestResults.onSuccess { response ->
            testResultGraphItemDao.clearInsertItems(response.speedCurve.download.map {
                it.toModel(
                    testUUID,
                    TestResultGraphItemRecord.Type.DOWNLOAD
                )
            })

            testResultGraphItemDao.clearInsertItems(response.speedCurve.upload.map {
                it.toModel(
                    testUUID,
                    TestResultGraphItemRecord.Type.UPLOAD
                )
            })

            testResultGraphItemDao.clearInsertItems(response.speedCurve.ping.map {
                it.toModel(
                    testUUID
                )
            })

            testResultGraphItemDao.clearInsertItems(response.speedCurve.signal.map {
                it.toModel(
                    testUUID
                )
            })
        }
    }

    private fun loadGraphResults(openTestUUID: String, testUUID: String) {
        val detailedTestResults = client.getTestResultGraphs(testUUID)
        detailedTestResults.onSuccess { response ->
            testResultGraphItemDao.clearInsertItems(response.speedCurve.download?.map {
                it.toModel(
                    testUUID,
                    TestResultGraphItemRecord.Type.DOWNLOAD
                )
            })

            testResultGraphItemDao.clearInsertItems(response.speedCurve.upload?.map {
                it.toModel(
                    testUUID,
                    TestResultGraphItemRecord.Type.UPLOAD
                )
            })
        }
    }

    override fun loadTestResults(testUUID: String): Flow<Boolean> = flow {
        val clientUUID = clientUUID.value
        if (clientUUID == null) {
            Timber.w("Unable to update test results client uuid is null")
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
                loadOpenDataTestResults(testResult.testOpenUUID, testUUID)
                loadQosTestResults(testUUID, clientUUID)
                emit(true)
            }

            response.onFailure { throw it }
        }
    }

    override fun saveOverallQosItem(overallQosPercentage: Float?, testUUID: String) {
        overallQosPercentage?.let {
            qoeInfoDao.insert(
                QoeInfoRecord(
                    testUUID = testUUID,
                    category = QoECategory.QOE_QOS,
                    percentage = overallQosPercentage ?: 0f,
                    classification = Classification.NONE,
                    priority = -1,
                    info = "${overallQosPercentage ?: 0}%"
                )
            )
        }
    }

    override fun getOverallQosItem(testUUID: String): LiveData<List<QoeInfoRecord>> {
        return qoeInfoDao.get(testUUID)
    }

    private fun loadQosTestResults(testUUID: String, clientUUID: String): Maybe<BaseResponse> {
        val isONTBasedApp = !config.headerValue.isNullOrEmpty()

        if (isONTBasedApp) {
            val qosTestResults = client.getTestResultDetailONT(testUUID)
            val overallQosPercentage = qosTestResults.success.overallQosPercentage
            val partialQosResults = qosTestResults.success.partialQosResults
            saveOverallQosItem(overallQosPercentage, qosTestResults.success.testUUID)

            partialQosResults.forEach { qosResultItem ->
                qosCategoryDao.insert(
                    QosCategoryRecord(
                        testUUID = qosTestResults.success.testUUID,
                        category = qosResultItem.testType?.let { QoSCategory.fromString(it) }
                            ?: QoSCategory.QOS_UNKNOWN,
                        categoryName = "",
                        categoryDescription = "",
                        language = "",
                        failedCount = qosResultItem.totalCount?.let {
                            it - (qosResultItem.successCount ?: 0)
                        } ?: 0,
                        successCount = qosResultItem.successCount ?: 0
                    )
                )
            }
            return qosTestResults
        } else {
            val qosTestResults = client.getQosTestResultDetail(
                QosTestResultDetailBody(
                    testUUID = testUUID,
                    clientUUID = clientUUID,
                    language = Locale.getDefault().language,
                    capabilities = CapabilitiesBody()
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

                val percentage: Float =
                    (successCount.toFloat() / (successCount + failureCount).toFloat())

                val info =
                    "${(percentage * 100).toInt()}% ($successCount/${successCount + failureCount})"

                qoeInfoDao.clearQoSInsert(
                    QoeInfoRecord(
                        testUUID = testUUID,
                        category = QoECategory.QOE_QOS,
                        percentage = percentage,
                        info = info,
                        classification = getQosClassification(percentage * 100f),
                        priority = -1
                    )
                )
            }
            return qosTestResults
        }
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