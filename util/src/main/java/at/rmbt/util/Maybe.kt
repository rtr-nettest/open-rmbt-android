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

package at.rmbt.util

import at.rmbt.util.exception.HandledException
import timber.log.Timber

/**
 * Represents fail or success principle
 */
class Maybe<out S>(private val successValue: S?, private val exceptionValue: HandledException?) {

    companion object {

        /**
         * Runs block of code if any of [HandledException] is thrown it will be returned as [Maybe] with failure
         */
        fun <S> of(block: () -> (S)): Maybe<S> {
            return try {
                Maybe(block.invoke())
            } catch (ex: Exception) {
                Timber.e(ex)
                Maybe(HandledException.from(ex))
            }
        }
    }

    constructor(success: S) : this(success, null)
    constructor(exception: HandledException) : this(null, exception)

    /**
     * Returns success result - [ok] should be checked first
     */
    val success: S
        get() = successValue!!

    /**
     * Returns failure result - [ok] should be checked first
     */
    val failure: HandledException
        get() = exceptionValue!!

    /**
     * Returns true if [Maybe] has [success] result
     */
    val ok: Boolean
        get() = successValue != null

    /**
     * Maps result to required success data with map block
     */
    fun <T> map(block: (S) -> (T)): Maybe<T> =
        if (ok) {
            Maybe(block.invoke(success))
        } else {
            Maybe(failure)
        }

    /**
     * Maps failure
     */
    fun mapError(block: (HandledException) -> HandledException): Maybe<S> =
        if (ok) {
            Maybe(success)
        } else {
            Maybe(block.invoke(failure))
        }

    /**
     * Executes block of code if result was successful
     */
    inline fun onSuccess(block: (S) -> (Unit)) {
        if (ok) {
            block.invoke(success)
        }
    }

    /**
     * Executes block of code if result was failed
     */
    inline fun onFailure(block: (HandledException) -> Unit) {
        if (!ok) {
            block.invoke(failure)
        }
    }
}