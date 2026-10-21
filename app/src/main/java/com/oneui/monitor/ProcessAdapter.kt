package com.oneui.monitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProcessAdapter(
    private var processList: List<ProcessModel>,
    private val onItemClick: (ProcessModel) -> Unit
) : RecyclerView.Adapter<ProcessAdapter.ProcessViewHolder>() {

    class ProcessViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvProcessName)
        val tvPackage: TextView = view.findViewById(R.id.tvPackageName)
        val tvStatus: TextView = view.findViewById(R.id.tvProcessStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProcessViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_process, parent, false)
        return ProcessViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProcessViewHolder, position: Int) {
        val item = processList[position]
        holder.tvName.text = item.appName
        holder.tvPackage.text = item.packageName
        holder.tvStatus.text = item.formattedStatus

        if (item.icon != null) {
            holder.ivIcon.setImageDrawable(item.icon)
        } else {
            holder.ivIcon.setImageResource(R.mipmap.ic_launcher)
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = processList.size

    fun updateList(newList: List<ProcessModel>) {
        processList = newList
        notifyDataSetChanged()
    }
}
