/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.rmbt.util.exception

import android.content.Context
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Error wrapper that allows to handle errors by UI
 */
open class HandledException constructor(val msg: String?, val stringResource: Int?, var titleRes: Int? = null) : Exception() {

    constructor(message: String) : this(message, null)

    constructor(stringResource: Int) : this(null, stringResource)

    open fun getText(context: Context): String =
        msg ?: stringResource?.let(context::getString)
        ?: "Unknown error"

    fun getTitle(context: Context): String? = if (titleRes == null) {
        null
    } else {
        context.getString(titleRes!!)
    }

    companion object {

        fun from(ex: Throwable) = fromWithMessage(ex, "")

        fun fromWithMessage(ex: Throwable, msg: String) = when (ex) {
            is HandledException -> ex
            is SocketTimeoutException -> ConnectionTimeoutException()
            is UnknownHostException, is ConnectException, is SocketException -> NoConnectionException()
            else -> HandledException("Message: ${ex.message} \nCause: ${ex.cause} \nCustom message: $msg")
        }
    }
}