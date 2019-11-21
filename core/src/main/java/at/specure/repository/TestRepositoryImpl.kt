package at.specure.repository

import at.specure.database.dao.TestDao
import at.specure.database.entity.Test

class TestRepositoryImpl(private val testDao: TestDao) : TestRepository {

    override fun getTest(uuid: String): Test? {
        return testDao.getTestResult(uuid)
    }

    override fun getLatestTest(): Test? {
        return testDao.getLatestTestResult()
    }

    override fun saveTest(test: Test) {
        testDao.insert(test)
    }

    override fun deleteTest(test: Test) {
        testDao.deleteTest(test)
    }
}