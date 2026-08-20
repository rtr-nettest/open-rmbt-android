/*******************************************************************************
 * Copyright 2013-2014 alladin-IT GmbH
 * Copyright 2013-2014 Rundfunk und Telekom Regulierungs-GmbH (RTR-GmbH)
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

import java.util.Properties

object RevisionHelper {
    val describe: String

    val revision: String

    val gitId: String

    val branch: String

    val dirty: Boolean

    init {
        var describeValue: String? = null
        var revisionValue: String? = null
        var gitIdValue: String? = null
        var branchValue: String? = null
        var dirtyValue = false
        val svnIS = RevisionHelper::class.java.classLoader?.getResourceAsStream("revision.properties")
        val properties = Properties()
        if (svnIS != null) {
            try {
                properties.load(svnIS)
                describeValue = properties.getProperty("git.describe")
                gitIdValue = properties.getProperty("git.id")
                branchValue = properties.getProperty("git.branch")
                val dirtyString = properties.getProperty("git.dirty")
                dirtyValue = dirtyString != null && dirtyString == "true"
                revisionValue = properties.getProperty("git.revision")
                if (dirtyValue) {
                    revisionValue = "$revisionValue-dirty"
                    gitIdValue = "$gitIdValue-dirty"
                    describeValue = "$describeValue-dirty"
                }
            } catch (e: Exception) {
                // there isn't much we can do here about it..
            }
        }
        describe = describeValue ?: "?"
        revision = revisionValue ?: "?"
        gitId = gitIdValue ?: "?"
        branch = branchValue ?: "?"
        dirty = dirtyValue
    }

    fun getVerboseRevision(): String = String.format("%s_%s", branch, describe)

    fun getServerVersion(): String = gitId
}
