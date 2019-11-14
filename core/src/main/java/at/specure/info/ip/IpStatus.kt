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

package at.specure.info.ip

/**
 * Describes state of private and public Ip addresses
 */
enum class IpStatus {

    /**
     * No information about Ip addresses
     */
    NO_INFO,

    /**
     * Neither public and private addresses not found
     */
    NO_ADDRESS,

    /**
     * Only private address is available
     */
    ONLY_LOCAL,

    /**
     * Both private and public addresses are available using Address Translation
     */
    NAT,

    /**
     * Both private and public addresses are available without Address Translation
     */
    NO_NAT,

    /**
     * Both private and public addresses are available with Address Translation IPv4 to IPv6
     */
    NAT_IPV4_TO_IPV6,

    /**
     * Both private and public addresses are available with Address Translation IPv6 to IPv4
     */
    NAT_IPV6_TO_IPV4;
}