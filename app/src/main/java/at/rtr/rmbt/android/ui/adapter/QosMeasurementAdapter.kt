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

package at.rtr.rmbt.android.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemQosMeasurementBinding
import at.rtr.rmbt.android.util.bindWith
import at.rtr.rmbt.client.v2.task.result.QoSTestResultEnum

class QosMeasurementAdapter : RecyclerView.Adapter<ViewHolder>() {

    private val initialItems = listOf(
        Pair(QoSTestResultEnum.WEBSITE, R.string.measurement_qos_web_site),
        Pair(QoSTestResultEnum.NON_TRANSPARENT_PROXY, R.string.measurement_qos_transparent_connection),
        Pair(QoSTestResultEnum.DNS, R.string.measurement_qos_dns),
        Pair(QoSTestResultEnum.TCP, R.string.measurement_qos_tcp_ports),
        Pair(QoSTestResultEnum.UDP, R.string.measurement_qos_udp_ports),
        Pair(QoSTestResultEnum.HTTP_PROXY, R.string.measurement_qos_unmodified_content),
        Pair(QoSTestResultEnum.TRACEROUTE, R.string.measurement_qos_traceroute),
        Pair(QoSTestResultEnum.VOIP, R.string.measurement_qos_voip)
    )

    private val _values = mutableMapOf<QoSTestResultEnum, Int>()
    private val items = mutableListOf<Pair<QoSTestResultEnum, Int>>()

    var values: Map<QoSTestResultEnum, Int>
        get() = _values
        set(value) {
            _values.clear()

            value.forEach { entry ->
                if (entry.value < 100) {
                    _values[entry.key] = entry.value
                }
            }

            items.clear()
            initialItems.forEach {
                if (_values.containsKey(it.first)) {
                    items.add(it)
                }
            }
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent.bindWith(R.layout.item_qos_measurement))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pair = items[position]
        val progress = _values[pair.first] ?: 0
        holder.binding.textQosTitle.text = holder.binding.textQosTitle.context.getString(pair.second)
        holder.binding.progressBarQos.progress = progress
    }
}

class ViewHolder(val binding: ItemQosMeasurementBinding) : RecyclerView.ViewHolder(binding.root)