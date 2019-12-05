package at.specure.repository

import at.rmbt.util.io
import at.specure.database.dao.TestDao
import at.specure.database.entity.TestRecord
import timber.log.Timber

class TestRepositoryImpl(private val testDao: TestDao) : TestRepository {

    private var counter = 0

    override fun saveTest(test: TestRecord) = io {
        testDao.insert(test)
    }

    override fun update(testRecord: TestRecord) = io {
        Timber.d("update{$counter}: ${testRecord.bytesDownload} ${testRecord.totalBytesDownload}")
        counter++
        testDao.update(testRecord)
    }
}