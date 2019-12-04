package at.specure.repository

import at.rmbt.util.io
import at.specure.database.dao.TestDao
import at.specure.database.entity.TestRecord

class TestRepositoryImpl(private val testDao: TestDao) : TestRepository {

    override fun saveTest(test: TestRecord) = io {
        testDao.insert(test)
    }

    override fun update(testRecord: TestRecord) = io {
        testDao.update(testRecord)
    }
}