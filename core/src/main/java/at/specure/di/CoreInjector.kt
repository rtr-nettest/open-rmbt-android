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
import at.specure.measurement.signal.SignalMeasurementService
import at.specure.worker.request.SendDataWorker
import at.specure.worker.request.SettingsWorker
import at.specure.worker.request.SignalMeasurementChunkWorker
import at.specure.worker.request.SignalMeasurementInfoWorker

object CoreInjector : CoreComponent {

    lateinit var component: CoreComponent

    override fun inject(settingsWorker: SettingsWorker) = component.inject(settingsWorker)

    override fun inject(sendDataWorker: SendDataWorker) = component.inject(sendDataWorker)

    override fun inject(service: MeasurementService) = component.inject(service)

    override fun inject(service: SignalMeasurementService) = component.inject(service)

    override fun inject(worker: SignalMeasurementInfoWorker) = component.inject(worker)

    override fun inject(worker: SignalMeasurementChunkWorker) = component.inject(worker)
}