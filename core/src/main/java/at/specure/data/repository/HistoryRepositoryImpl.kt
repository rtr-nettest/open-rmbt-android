package at.specure.data.repository

import androidx.lifecycle.LiveData
import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.HistoryRequestBody
import at.rmbt.util.Maybe
import at.rmbt.util.io
import at.specure.config.Config
import at.specure.data.ClientUUID
import at.specure.data.dao.HistoryDao
import at.specure.data.entity.History
import at.specure.data.toCapabilitiesBody
import timber.log.Timber
import java.util.Locale

private const val HISTORY_PAGE_SIZE = 25

class HistoryRepositoryImpl(
    private val historyDao: HistoryDao,
    private val config: Config,
    private val clientUUID: ClientUUID,
    private val client: ControlServerClient
) : HistoryRepository {

    override fun getHistory(): LiveData<List<History>> = historyDao.get()

    override fun refreshHistory(callback: (Maybe<Boolean>) -> Unit) = io {
        val clientUUID = clientUUID.value
        if (clientUUID == null) {
            Timber.w("Unable to update history client uuid is null")
            callback.invoke(Maybe(false))
            return@io
        }

        val body = HistoryRequestBody(
            clientUUID = clientUUID,
            offset = 0,
            limit = HISTORY_PAGE_SIZE,
            capabilities = config.toCapabilitiesBody(),
            devices = null,
            networks = null,
            language = Locale.getDefault().language
        )

        val response = client.getHistory(body)

        Timber.d("History received")
    }
}