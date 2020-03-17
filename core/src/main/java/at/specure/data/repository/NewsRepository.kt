package at.specure.data.repository

import at.rmbt.client.control.NewsItem
import kotlinx.coroutines.flow.Flow

interface NewsRepository {

    fun getNews(): Flow<List<NewsItem>?>

    fun setNewsShown(newItem: NewsItem)

    fun getLatestNewsShown(): Long?
}