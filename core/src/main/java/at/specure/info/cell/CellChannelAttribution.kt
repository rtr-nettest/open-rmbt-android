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

package at.specure.info.cell

/**
 * Channel attribution type
 */
enum class CellChannelAttribution {

    /**
     *  New Radio Absolute Radio Frequency Channel Number.
     */
    NRARFCN,

    /**
     * 18-bit Absolute RF Channel Number
     */
    EARFCN,

    /**
     * 16-bit UMTS Absolute RF Channel Number
     */
    UARFCN,

    /**
     * 16-bit GSM Absolute RF Channel Number
     */
    ARFCN
}