package at.rtr.rmbt.android.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import at.rmbt.client.control.Server
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemMeasurementServerSelectionBinding
import at.rtr.rmbt.android.viewmodel.ServerWithDistance

/**
 * Used for Home Slider.
 */
class MeasurementServerSelectionAdapter(private val items: List<ServerWithDistance>, private val onSelect: (ServerWithDistance) -> Unit) :
    RecyclerView.Adapter<MeasurementServerSelectionAdapter.Holder>() {

    var selected: ServerWithDistance? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemMeasurementServerSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.run {
            val server = items[position]
            distance.text = distance.context.getString(R.string.text_server_distance_pattern, server.distance)
            name.text = server.name
            check.visibility = if (selected?.uuid == server.uuid) View.VISIBLE else View.GONE

            root.setOnClickListener { onSelect.invoke(server) }
        }
    }

    override fun getItemCount(): Int = items.count()

    class Holder(val binding: ItemMeasurementServerSelectionBinding) : RecyclerView.ViewHolder(binding.root)
}