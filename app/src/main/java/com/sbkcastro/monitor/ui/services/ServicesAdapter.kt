package com.sbkcastro.monitor.ui.services

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sbkcastro.monitor.R

class ServicesAdapter : ListAdapter<Service, ServicesAdapter.ServiceViewHolder>(ServiceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconText: TextView = itemView.findViewById(R.id.serviceIcon)
        private val nameText: TextView = itemView.findViewById(R.id.serviceName)
        private val urlText: TextView = itemView.findViewById(R.id.serviceUrl)
        private val statusText: TextView = itemView.findViewById(R.id.serviceStatus)
        private val responseText: TextView = itemView.findViewById(R.id.serviceResponse)

        fun bind(service: Service) {
            iconText.text = service.icon
            nameText.text = service.name
            urlText.text = service.url

            when (service.status) {
                ServiceStatus.ONLINE -> {
                    statusText.text = "✅ Online"
                    statusText.setTextColor(Color.parseColor("#4CAF50"))
                    responseText.text = "${service.responseTime}ms"
                    responseText.visibility = View.VISIBLE
                }
                ServiceStatus.OFFLINE -> {
                    statusText.text = "❌ Offline"
                    statusText.setTextColor(Color.parseColor("#F44336"))
                    responseText.visibility = View.GONE
                }
                ServiceStatus.CHECKING -> {
                    statusText.text = "⏳ Verificando..."
                    statusText.setTextColor(Color.parseColor("#FF9800"))
                    responseText.visibility = View.GONE
                }
            }
        }
    }

    class ServiceDiffCallback : DiffUtil.ItemCallback<Service>() {
        override fun areItemsTheSame(oldItem: Service, newItem: Service): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: Service, newItem: Service): Boolean {
            return oldItem == newItem
        }
    }
}
