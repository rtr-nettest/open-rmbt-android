package at.rtr.rmbt.android.ui.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.databinding.ItemMarkerDetailsBinding
import at.rtr.rmbt.android.util.bindWith
import at.specure.data.entity.MarkerMeasurementRecord
import kotlin.math.abs

class MapMarkerDetailsAdapter(private val callback: MarkerDetailsCallback) :
    ListAdapter<MarkerMeasurementRecord, MapMarkerDetailsAdapter.Holder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding: ItemMarkerDetailsBinding = parent.bindWith(R.layout.item_marker_details)
        binding.root.layoutParams.width = (parent.width * WIDTH_COEF).toInt()
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.item = getItem(position)
        holder.binding.iconClose.setOnClickListener { callback.onCloseMarkerDetails() }
        holder.binding.moreDetails.setOnClickListener { callback.onMoreDetailsClicked() }
    }

    class Holder(val binding: ItemMarkerDetailsBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {

        private const val WIDTH_COEF = 0.9

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MarkerMeasurementRecord>() {
            override fun areItemsTheSame(oldItem: MarkerMeasurementRecord, newItem: MarkerMeasurementRecord): Boolean =
                oldItem.openTestUUID == newItem.openTestUUID

            override fun areContentsTheSame(old: MarkerMeasurementRecord, new: MarkerMeasurementRecord): Boolean {
                return old.uploadClass == new.uploadClass && old.uploadSpeedKbs == new.uploadSpeedKbs && old.downloadClass == new.downloadClass &&
                        old.downloadSpeedKbs == new.downloadSpeedKbs && old.signalClass == new.signalClass &&
                        old.signalStrength == new.signalStrength && old.pingClass == new.pingClass && old.pingMillis == new.pingMillis &&
                        abs(old.latitude - new.latitude) < 0.001 && abs(old.longitude - new.longitude) < 0.001 &&
                        old.networkTypeLabel == new.networkTypeLabel && old.providerName == new.providerName && old.wifiSSID == new.wifiSSID &&
                        old.openTestUUID == new.openTestUUID && old.time == new.time && old.timeString == new.timeString
            }
        }
    }

    interface MarkerDetailsCallback {
        fun onCloseMarkerDetails()
        fun onMoreDetailsClicked()
    }
}