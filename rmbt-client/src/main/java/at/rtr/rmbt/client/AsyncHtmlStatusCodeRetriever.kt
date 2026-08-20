package at.rtr.rmbt.client

import android.os.AsyncTask
import java.net.HttpURLConnection
import java.net.URL

class AsyncHtmlStatusCodeRetriever : AsyncTask<String, Void, Int>() {

    private var listener: ContentRetrieverListener? = null

    /**
     * @author lb
     */
    interface ContentRetrieverListener {
        fun onContentFinished(statusCode: Int?)
    }

    fun setContentRetrieverListener(listener: ContentRetrieverListener) {
        this.listener = listener
    }

    override fun doInBackground(vararg params: String?): Int? {
        return try {
            val url = URL(params[0])
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 3000
                connection.connect()
                val statusCode = connection.responseCode
                println("response code: $statusCode")
                statusCode
            } catch (e: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onPostExecute(result: Int?) {
        super.onPostExecute(result)

        listener?.onContentFinished(result)
    }
}
