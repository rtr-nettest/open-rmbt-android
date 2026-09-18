package at.rtr.rmbt.android.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches the RTR coverage "raster" identifiers for a coordinate from the public endpoint
 * `https://frq.rtr.at/api/rpc/id?cov_longitude=<lon>&cov_latitude=<lat>`.
 *
 * The response is a JSON array; the first object carries the 100 m and 250 m raster cell ids
 * (`r100`/`r250`) and their long/short id variants. Only the non-empty raster fields are returned,
 * in display order, keyed by their JSON field name (the caller maps the key to a label).
 */
object RasterIdSource {

    data class RasterEntry(val key: String, val value: String)

    // The raster id fields to surface, in the order they should be shown.
    private val RASTER_FIELDS = listOf(
        "r100", "short_id100", "long_id100",
        "r250", "short_id250", "long_id250"
    )

    private const val TIMEOUT_MILLIS = 8000

    suspend fun fetch(latitude: Double, longitude: Double): List<RasterEntry> =
        withContext(Dispatchers.IO) {
            // Double.toString() is locale-independent ('.' decimal), safe to interpolate directly.
            val url = URL("https://frq.rtr.at/api/rpc/id?cov_longitude=$longitude&cov_latitude=$latitude")
            var connection: HttpURLConnection? = null
            try {
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = TIMEOUT_MILLIS
                    readTimeout = TIMEOUT_MILLIS
                }
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Timber.w("Raster id request failed: HTTP ${connection.responseCode}")
                    return@withContext emptyList()
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                parse(body)
            } catch (e: Exception) {
                Timber.w(e, "Raster id request error")
                emptyList()
            } finally {
                connection?.disconnect()
            }
        }

    private fun parse(body: String): List<RasterEntry> {
        val array = JSONArray(body)
        if (array.length() == 0) return emptyList()
        val obj = array.getJSONObject(0)
        return RASTER_FIELDS.mapNotNull { key ->
            if (obj.isNull(key)) return@mapNotNull null
            val value = obj.optString(key, "").trim()
            if (value.isEmpty()) null else RasterEntry(key, value)
        }
    }
}
