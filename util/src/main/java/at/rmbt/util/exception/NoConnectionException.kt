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

/**
 * An error that signals while no internet connection is available
 */
open class NoConnectionException : HandledException("Unable to connect with server. Please, check your internet connection.")

/**
 * An error that signals that connection timeout exception was received
 */
class ConnectionTimeoutException : NoConnectionException()