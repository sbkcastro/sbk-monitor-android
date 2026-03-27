package com.sbkcastro.monitor.ui.processes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sbkcastro.monitor.R
import com.sbkcastro.monitor.api.ProcessInfo

class ProcessAdapter : ListAdapter<ProcessInfo, ProcessAdapter.ViewHolder>(DIFF) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvPid: TextView = view.findViewById(R.id.tvPid)
        val tvUser: TextView = view.findViewById(R.id.tvUser)
        val tvCpu: TextView = view.findViewById(R.id.tvCpu)
        val tvMem: TextView = view.findViewById(R.id.tvMem)
        val tvCommand: TextView = view.findViewById(R.id.tvCommand)
        val tvStat: TextView = view.findViewById(R.id.tvStat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_process, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val proc = getItem(position)
        holder.tvPid.text = proc.pid.toString()
        holder.tvUser.text = proc.user.take(8)
        holder.tvCpu.text = "%.1f%%".format(proc.cpu)
        holder.tvMem.text = "%.1f%%".format(proc.mem)
        holder.tvCommand.text = proc.command.take(60)
        holder.tvStat.text = proc.stat

        // Color CPU usage
        val cpuColor = when {
            proc.cpu >= 50 -> android.R.color.holo_red_light
            proc.cpu >= 20 -> android.R.color.holo_orange_light
            else -> android.R.color.holo_green_light
        }
        holder.tvCpu.setTextColor(ContextCompat.getColor(holder.itemView.context, cpuColor))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ProcessInfo>() {
            override fun areItemsTheSame(a: ProcessInfo, b: ProcessInfo) = a.pid == b.pid
            override fun areContentsTheSame(a: ProcessInfo, b: ProcessInfo) = a == b
        }
    }
}
