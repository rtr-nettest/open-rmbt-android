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

package at.specure.di

import at.specure.measurement.MeasurementService
import at.specure.worker.request.SettingsWorker

object CoreInjector : CoreComponent {

    lateinit var component: CoreComponent

    override fun inject(settingsWorker: SettingsWorker) = component.inject(settingsWorker)

    override fun inject(service: MeasurementService) = component.inject(service)
}