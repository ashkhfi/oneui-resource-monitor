package com.oneui.monitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DebloatAdapter(
    private var debloatList: List<DebloatItem>,
    private val onItemClick: (DebloatItem) -> Unit
) : RecyclerView.Adapter<DebloatAdapter.DebloatViewHolder>() {

    class DebloatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvDebloatName)
        val tvStatus: TextView = view.findViewById(R.id.tvDebloatStatus)
        val tvPackage: TextView = view.findViewById(R.id.tvDebloatPackage)
        val tvDesc: TextView = view.findViewById(R.id.tvDebloatDesc)
        val tvRam: TextView = view.findViewById(R.id.tvDebloatRam)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebloatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_debloat, parent, false)
        return DebloatViewHolder(view)
    }

    override fun onBindViewHolder(holder: DebloatViewHolder, position: Int) {
        val item = debloatList[position]
        holder.tvName.text = item.appName
        holder.tvPackage.text = item.packageName
        holder.tvDesc.text = item.description
        holder.tvRam.text = "Estimasi RAM: +${item.ramEstimateMB} MB"

        val context = holder.itemView.context
        if (item.isFrozen) {
            holder.tvStatus.text = "❄️ Dibekukan"
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.temp_cool))
        } else {
            holder.tvStatus.text = "⚡ Sedang Aktif"
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.temp_warm))
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = debloatList.size

    fun updateList(newList: List<DebloatItem>) {
        debloatList = newList
        notifyDataSetChanged()
    }
}
