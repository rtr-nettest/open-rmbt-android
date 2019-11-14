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

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import timber.log.Timber

val IO = object : CoroutineScope {
    override val coroutineContext = Dispatchers.IO
}

@Suppress("DeferredResultUnused")
fun io(block: suspend CoroutineScope.() -> (Unit)) {
    val catchingBlock: suspend CoroutineScope.() -> (Unit) = {
        try {
            block.invoke(this)
        } catch (throwable: Throwable) {
            Timber.e(throwable)
            Handler(Looper.getMainLooper())
                .post { throw throwable }
        }
    }

    IO.async(Dispatchers.IO, block = catchingBlock)
}