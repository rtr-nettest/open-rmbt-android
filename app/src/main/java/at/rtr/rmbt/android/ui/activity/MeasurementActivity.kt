/*
 *
 *  Licensed under the Apache License, Version 2.0 (the “License”);
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an “AS IS” BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package at.rtr.rmbt.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ActivityMeasurementBinding
import at.rtr.rmbt.android.di.viewModelLazy
import at.rtr.rmbt.android.ui.adapter.QosMeasurementAdapter
import at.rtr.rmbt.android.viewmodel.MeasurementViewModel
import at.specure.measurement.MeasurementService

class MeasurementActivity : BaseActivity() {

    private val viewModel: MeasurementViewModel by viewModelLazy()
    private lateinit var binding: ActivityMeasurementBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = bindContentView(R.layout.activity_measurement)
        binding.state = viewModel.state
        binding.buttonStart.setOnClickListener {
            MeasurementService.startTests(this)
            viewModel.producer?.startTests()
        }

        binding.buttonStop.setOnClickListener {
            viewModel.producer?.stopTests()
        }
        binding.measurementBottomView.qosTestRecyclerView.apply {
            adapter = QosMeasurementAdapter(this@MeasurementActivity)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.attach(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.detach(this)
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, MeasurementActivity::class.java)
            context.startActivity(intent)
        }
    }
}