package at.specure.util.download

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.text.TextUtils
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern
import javax.inject.Inject

class FileDownloader @Inject constructor(
    private val context: Context,
) {
    private val _downloadStateFlow: MutableStateFlow<DownloadState> =
        MutableStateFlow(DownloadState.Initial)
    val downloadStateFlow: StateFlow<DownloadState> = _downloadStateFlow

    sealed class DownloadState {
        object Initial : DownloadState()
        data class Downloading(val progress: Int) : DownloadState()
        data class Success(val file: File) : DownloadState()
        object Error : DownloadState()
    }

    suspend fun downloadFile(
        urlString: String,
        openUuid: String,
        format: String,
        fileName: String? = null
    ) {
        withContext(Dispatchers.IO) {
            try {
                _downloadStateFlow.value = DownloadState.Downloading(1)
                val name = if (fileName != null) {
                    "$fileName.$format"
                } else {
                    "$openUuid.$format"
                }
                val outputFile = if (Build.VERSION_CODES.Q >= Build.VERSION.SDK_INT) {
                    File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                        name
                    )
                } else {
                    File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                        name
                    )
                }
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                if (format == "pdf") {
                    connection.setRequestProperty("accept", "application/pdf")
                }

                val postData = if (format == "pdf") "open_test_uuid=$openUuid"
                else "open_test_uuid=$openUuid&format=$format"
                val postDataBytes = postData.toByteArray(Charsets.UTF_8)

                connection.outputStream.write(postDataBytes)

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val totalSize = connection.contentLength
                    var downloadedSize = 0

                    val inputStream = BufferedInputStream(connection.inputStream)
                    val outputStream = FileOutputStream(outputFile)

                    inputStream.use { input ->
                        outputStream.use { output ->
                            val buffer = ByteArray(1024)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloadedSize += bytesRead
                                val progress = (downloadedSize * 100) / totalSize
                                _downloadStateFlow.value = DownloadState.Downloading(progress)
                            }
                        }
                    }
                    _downloadStateFlow.value = DownloadState.Success(outputFile)
                } else {
                    // Handle HTTP error response
                    _downloadStateFlow.value = DownloadState.Error
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _downloadStateFlow.value = DownloadState.Error
            }
        }
    }

    fun openFile(file: File, onError: (e: Exception) -> Unit) {
        val parsedUri = convertUriForUseInIntent(file.path)
        parsedUri?.let { fileUri ->
            val intent = if (Build.VERSION_CODES.Q >= Build.VERSION.SDK_INT) {
                createOpenFileIntentV29(fileUri)
            } else {
                createOpenFileIntent(fileUri)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    private fun createOpenFileIntent(fileUri: Uri): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(fileUri, getMimeType(fileUri))
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.action = Intent.ACTION_VIEW
        return intent
    }

    private fun createOpenFileIntentV29(fileUri: Uri): Intent {
        val filename = fileUri.path?.getFileNameWithExtFromUriOrDefault() ?: "file"
        val file = File(
            ContextCompat.getExternalFilesDirs(context, Environment.DIRECTORY_DOWNLOADS)[0],
            filename
        )
        val uri = Uri.fromFile(file)
        val uriParsed = convertUriForUseInIntent(uri.toString())
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uriParsed, getMimeType(fileUri))
        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        return intent
    }

    private fun convertUriForUseInIntent(localUri: String?): Uri? {
        if (localUri != null) {
            val parsedUri = Uri.parse(localUri)
            parsedUri.path?.let { parsedPath ->
                return FileProvider.getUriForFile(
                    context, context.packageName + ".provider",
                    File(parsedPath)
                )
            }
        }
        return null
    }

    private fun getMimeType(uri: Uri): String? {
        val isContent = uri.scheme == "content"
        return if (isContent) {
            val cr = context.contentResolver
            cr.getType(uri)
        } else {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            )
        }
    }

    fun String.getFileNameWithExtFromUriOrDefault(): String {
        var url = this
        if (!TextUtils.isEmpty(url)) {
            url = removeFragmentPart(url)
            url = removeQueryPart(url)

            val filenamePos: Int = url.lastIndexOf('/')
            val filename: String = if (0 <= filenamePos) url.substring(filenamePos + 1) else url

            val notContainsSpecialCharacters = Pattern.matches("[a-zA-Z_0-9.\\-()%]+", filename)
            if (filename.isNotEmpty() &&
                notContainsSpecialCharacters
            ) {
                return filename
            }
        }
        return "Default"
    }

    private fun removeFragmentPart(uri: String): String {
        var url = uri
        val fragment: Int = url.lastIndexOf('#')
        if (fragment > 0) {
            url = url.substring(0, fragment)
        }
        return url
    }

    private fun removeQueryPart(uri: String): String {
        var url = uri
        val query: Int = url.lastIndexOf('?')
        if (query > 0) {
            url = url.substring(0, query)
        }
        return url
    }
}
