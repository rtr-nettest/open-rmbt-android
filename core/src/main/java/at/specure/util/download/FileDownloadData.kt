package at.specure.util.download

import java.io.File

data class FileDownloadData(
    val file: File? = null,
    val progress: Int? = null,
    val error: String? = null
)