package at.specure.repository

import at.specure.database.dao.TestDao
import at.specure.database.entity.TestRecord

class TestRepositoryImpl(private val testDao: TestDao) : TestRepository {

    override fun getTest(uuid: String): TestRecord? {
        return testDao.getTestResult(uuid)
    }

    override fun getLatestTest(): TestRecord? {
        return testDao.getLatestTestResult()
    }

    override fun saveTest(test: TestRecord) {
        testDao.insert(test)
    }

    override fun deleteTest(test: TestRecord) {
        testDao.deleteTest(test)
    }
}