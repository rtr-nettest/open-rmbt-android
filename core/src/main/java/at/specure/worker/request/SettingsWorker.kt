package at.specure.worker.request

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import at.specure.di.CoreInjector
import at.specure.data.repository.SettingsRepository
import javax.inject.Inject

class SettingsWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    @Inject
    lateinit var repository: SettingsRepository

    override fun doWork(): Result {
        CoreInjector.inject(this)

        val result = repository.refreshSettings()
        return if (result) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}