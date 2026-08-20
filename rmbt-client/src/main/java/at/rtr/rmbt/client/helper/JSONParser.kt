/*******************************************************************************
 * Copyright 2013-2016 alladin-IT GmbH
 * Copyright 2013-2016 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.rtr.rmbt.client.helper

import at.rtr.rmbt.util.capability.Capabilities
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

object JSONParser {
    var CONNECT_TIMEOUT = 5000

    var READ_TIMEOUT = 8000

    private var CAPABILITIES: JSONObject? = null

    fun setCapabilities(capabilities: Capabilities?) {
        try {
            CAPABILITIES = if (capabilities == null) {
                null
            } else {
                JSONObject(Gson().toJson(capabilities).toString())
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun readUrl(url: URL): String {
        val urlConnection = url.openConnection()
        try {
            urlConnection.connectTimeout = CONNECT_TIMEOUT
            urlConnection.readTimeout = READ_TIMEOUT

            val stringBuilder = StringBuilder()
            val reader = BufferedReader(InputStreamReader(urlConnection.getInputStream()))
            var read: Int
            val chars = CharArray(1024)
            while (reader.read(chars).also { read = it } != -1) {
                stringBuilder.append(chars, 0, read)
            }
            return stringBuilder.toString()
        } finally {
            if (urlConnection is HttpURLConnection) {
                urlConnection.disconnect()
            }
        }
    }

    fun sendToUrl(url: URL, data: String, connectTimeout: Int, headerValue: String?): String {
        val urlConnection = url.openConnection() as HttpURLConnection
        try {
            urlConnection.doOutput = true
            urlConnection.doInput = true

            urlConnection.connectTimeout = connectTimeout
            urlConnection.readTimeout = READ_TIMEOUT

            urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            urlConnection.setRequestProperty("Accept", "application/json")
            if (headerValue != null && headerValue.isNotEmpty()) {
                urlConnection.setRequestProperty("X-Nettest-Client", headerValue)
            }

            val bytes = data.toByteArray(Charset.forName("UTF-8"))
            urlConnection.setFixedLengthStreamingMode(bytes.size)
            urlConnection.outputStream.write(bytes)

            val stringBuilder = StringBuilder()
            val reader = BufferedReader(InputStreamReader(urlConnection.getInputStream()))
            var read: Int
            val chars = CharArray(1024)
            while (reader.read(chars).also { read = it } != -1) {
                stringBuilder.append(chars, 0, read)
            }
            return stringBuilder.toString()
        } finally {
            urlConnection.disconnect()
        }
    }

    fun getURL(url: URL): JSONObject? {
        // try parse the string to a JSON object
        return try {
            val data = readUrl(url)
            JSONObject(data)
        } catch (e: Exception) {
            null
        }
    }

    fun sendJSONToUrl(url: URL, data: JSONObject): JSONObject? {
        return sendJSONToUrl(url, data, CONNECT_TIMEOUT, null)
    }

    fun sendJSONToUrl(url: URL, data: JSONObject, headerValue: String?): JSONObject? {
        return sendJSONToUrl(url, data, CONNECT_TIMEOUT, headerValue)
    }

    fun sendJSONToUrl(url: URL, data: JSONObject, connectTimeout: Int, headerValue: String?): JSONObject? {
        return try {
            if (CAPABILITIES != null) {
                data.put("capabilities", CAPABILITIES)
            }
            val output = sendToUrl(url, data.toString(), connectTimeout, headerValue)
            JSONObject(output)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun toMap(obj: JSONObject): Map<String, Any?> {
        val map: MutableMap<String, Any?> = HashMap()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = fromJson(obj.get(key))
        }
        return map
    }

    fun toList(array: JSONArray): List<Any?> {
        val list: MutableList<Any?> = ArrayList()
        for (i in 0 until array.length()) {
            list.add(fromJson(array.get(i)))
        }
        return list
    }

    private fun fromJson(json: Any?): Any? {
        return when {
            json === JSONObject.NULL -> null
            json is JSONObject -> toMap(json)
            json is JSONArray -> toList(json)
            else -> json
        }
    }
}
