package at.specure.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.lifecycle.LiveData
import at.rmbt.client.control.Coordinates
import at.rmbt.client.control.Filter
import at.rmbt.client.control.MapOptions
import at.rmbt.client.control.MapServerClient
import at.rmbt.client.control.MarkersRequestBody
import at.rmbt.client.control.data.MapPresentationType
import at.rmbt.util.io
import at.specure.data.CoreDatabase
import at.specure.data.entity.MarkerMeasurementRecord
import at.specure.data.toModelList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.util.Locale
import javax.inject.Inject

class MapRepositoryImpl @Inject constructor(private val client: MapServerClient, private val db: CoreDatabase) : MapRepository {

    /**
     * precalculated values to request latlng related data from dao
     */
    private var distanceThresholdsByZoom = hashMapOf(
        3 to 0.7,
        4 to 0.4,
        5 to 0.2,
        6 to 0.1,
        7 to 0.05,
        8 to 0.025,
        9 to 0.01,
        10 to 0.005,
        11 to 0.0025,
        12 to 0.001,
        13 to 0.0005,
        14 to 0.00025,
        15 to 0.0001,
        16 to 0.00005,
        17 to 0.000025,
        18 to 0.000001
    )

    override fun loadMarkers(latitude: Double?, longitude: Double?, zoom: Int, loaded: (Boolean) -> Unit) = io {
        val coordinates = Coordinates(latitude, longitude, zoom)

        val body = MarkersRequestBody(
            language = Locale.getDefault().language,
            coordinates = coordinates,
            filter = Filter(statisticalMethod = "0.5", period = "180"), // todo add real values from filter
            options = MapOptions("mobile/download") // todo add real values from filter
        )
        val result = client.getMarkers(body)
        result.onSuccess {
            db.mapDao().insert(it.toModelList())
        }
        loaded.invoke(result.ok)
    }

    override fun getMarkers(latitude: Double?, longitude: Double?, zoom: Int): LiveData<List<MarkerMeasurementRecord>> =
        db.mapDao().get(latitude, longitude, distanceThresholdsByZoom[zoom])

    override fun loadTiles(x: Int, y: Int, zoom: Int, type: MapPresentationType): ByteArray? = runBlocking(Dispatchers.IO) {
        val result =
            client.loadTiles(x, y, zoom, type, hashMapOf("map_options" to "mobile/download", "statistical_method" to "0.5", "period" to "180"))
        // todo: update with implementing filters
        if (result.isSuccessful) {
            with(result.body()) {
                this?.let {
                    return@runBlocking bytes()
                }
            }
        }
        return@runBlocking null
    }

    override fun loadAutomaticTiles(x: Int, y: Int, zoom: Int): ByteArray? {
        val heatmapBytes = loadTiles(x, y, zoom, MapPresentationType.AUTOMATIC)
        val pointsBytes = loadTiles(x, y, zoom, MapPresentationType.POINTS)

        val heatmapBitmap = BitmapFactory.decodeByteArray(heatmapBytes, 0, heatmapBytes?.size ?: 0)
        val pointsBitmap = BitmapFactory.decodeByteArray(pointsBytes, 0, pointsBytes?.size ?: 0)

        val result = Bitmap.createBitmap(heatmapBitmap.width, heatmapBitmap.height, heatmapBitmap.config)
        val canvas = Canvas(result)
        canvas.drawBitmap(heatmapBitmap, 0f, 0f, null)
        canvas.drawBitmap(pointsBitmap, 0f, 0f, null)

        val stream = ByteArrayOutputStream()
        result.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    override fun prepareDetailsLink(openUUID: String) = client.prepareDetailsLink(openUUID)
}