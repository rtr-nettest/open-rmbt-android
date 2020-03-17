package at.specure.data.repository

import android.content.Context
import at.rmbt.client.control.ControlServerClient
import at.rmbt.client.control.NewsItem
import at.rmbt.client.control.NewsRequestBody
import at.rmbt.util.io
import at.specure.data.ClientUUID
import at.specure.data.NewsSettings
import at.specure.test.DeviceInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale

class NewsRepositoryImpl(
    context: Context,
    private val controlServerClient: ControlServerClient,
    private val clientUUID: ClientUUID,
    private val newsSettings: NewsSettings
) : NewsRepository {

    private val deviceInfo = DeviceInfo(context)

    override fun getNews(): Flow<List<NewsItem>?> = flow {
        val getNews = controlServerClient.getNews(
            NewsRequestBody(
                language = Locale.getDefault().language,
                lastNewsUid = newsSettings.lastNewsUID,
                softwareVersionCode = deviceInfo.clientVersionCode.toString(),
                uuid = clientUUID.value
            )
        )

        getNews.onSuccess {
            emit(it.news)
        }
        getNews.onFailure {
            emit(null)
        }
    }

    override fun setNewsShown(newItem: NewsItem) = io {
        newsSettings.lastNewsUID = newItem.uid
    }

    override fun getLatestNewsShown(): Long? {
        return newsSettings.lastNewsUID
    }
}
