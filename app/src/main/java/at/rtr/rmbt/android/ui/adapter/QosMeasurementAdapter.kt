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
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R

class QosMeasurementAdapter(val context: Context) : RecyclerView.Adapter<ViewHolder>() {

    private var items = listOf<String>(context.getString(R.string.measurement_qos_web_site),
        context.getString(R.string.measurement_qos_transparent_connection),
        context.getString(R.string.measurement_qos_dns),
        context.getString(R.string.measurement_qos_tcp_ports),
        context.getString(R.string.measurement_qos_udp_ports),
        context.getString(R.string.measurement_qos_unmodified_content),
        context.getString(R.string.measurement_qos_traceroute),
        context.getString(R.string.measurement_qos_voip))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.row_qos_measurement, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.textQosTitle.text = items[position]
    }
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val textQosTitle: TextView = view.findViewById(R.id.textQosTitle)
}