package at.specure.data.repository

import at.rmbt.util.io
import at.specure.data.CoreDatabase
import at.specure.data.TermsAndConditions
import at.specure.data.entity.TacRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL

class TacRepositoryImpl(db: CoreDatabase, private val termsAndConditions: TermsAndConditions) : TacRepository {

    private val tacDao = db.tacDao()

    override fun getTac(language: String): Flow<TacRecord?> = flow {
        val tacRecord = getTacFromDB(language)
        if (tacRecord == null || termsAndConditions.tacVersion ?: -1 > tacRecord.version) {
            val contentFromServerLink = loadFromHtml(termsAndConditions.tacUrl ?: "")
            if (contentFromServerLink == null) {
                if (tacRecord == null || tacRecord.content.isEmpty()) {
                    val contentFromLocalLink = loadFromHtml(termsAndConditions.defaultUrl)
                    if (contentFromLocalLink == null) {
                        val contentFromLocalFile = parseAndPrepareHtml(termsAndConditions.localVersion)
                        val record = TacRecord(language, -1, contentFromLocalFile)
                        emit(record)
                    } else {
                        val record = TacRecord(language = language, version = -1, content = contentFromLocalLink)
                        emit(record)
                        saveTac(record)
                    }
                } else {
                    emit(tacRecord)
                }
            } else {
                val record = TacRecord(language = language, version = termsAndConditions.tacVersion ?: -1, content = contentFromServerLink)
                emit(record)
                saveTac(record)
            }
        } else {
            emit(tacRecord)
        }
    }

    private fun loadFromHtml(url: String): String? {
        return try {
            parseAndPrepareHtml(URL(url).openStream())
        } catch (e: IOException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun parseAndPrepareHtml(inputStream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(inputStream))
        return reader.readText()
    }

    private fun getTacFromDB(language: String): TacRecord? {
        return tacDao.loadTermsAndConditions(language)
    }

    override fun saveTac(tacRecord: TacRecord) = io {
        tacDao.clearInsertItems(tacRecord)
    }
}