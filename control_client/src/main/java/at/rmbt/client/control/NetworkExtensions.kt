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

package at.rmbt.client.control

import at.rmbt.util.Maybe
import at.rmbt.util.exception.HandledException
import retrofit2.Call
import timber.log.Timber

/**
 * Allows to execute call and unwraps data and errors from response
 */
fun <T : BaseResponse> Call<T>.exec(silentError: Boolean = false): Maybe<T> {
    return try {
        val result = execute()
        if (result.isSuccessful) {
            val body = result.body()
            if (body != null) {
                if (body.error?.isNotEmpty() == true) {
                    val errorText = buildString {
                        body.error.forEachIndexed { index, s ->
                            append(s)
                            if (index < body.error.size - 1) {
                                append("\n")
                            }
                        }
                    }
                    Maybe(HandledException(errorText))
                } else {
                    Maybe(body)
                }
            } else {
                return Maybe(HandledException("Server Returned an empty response"))
            }
        } else {
            Maybe(HandledException("Server connection error ${result.code()}"))
        }
    } catch (t: Throwable) {
        if (silentError) {
            Timber.w("Failed to perform request : ${t.message}")
        } else {
            Timber.w(t)
        }
        Maybe(HandledException.from(t))
    }
}