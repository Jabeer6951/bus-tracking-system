package com.college.bustracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.college.bustracker.model.BusLog

class BusLogAdapter(private val logList: List<BusLog>) :
    RecyclerView.Adapter<BusLogAdapter.BusLogViewHolder>() {

    class BusLogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBusId: TextView = itemView.findViewById(R.id.tvBusId)
        val tvDriverId: TextView = itemView.findViewById(R.id.tvDriverId)
        val tvGate: TextView = itemView.findViewById(R.id.tvGate)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusLogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bus_log, parent, false)
        return BusLogViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusLogViewHolder, position: Int) {
        val log = logList[position]
        holder.tvBusId.text = "Bus: ${log.busId}"
        holder.tvDriverId.text = "Driver: ${log.driverId}"
        holder.tvGate.text = "Gate: ${log.gateName}"
        holder.tvTime.text = "Time: ${log.time}"
        holder.tvDate.text = "Date: ${log.date}"
    }

    override fun getItemCount(): Int = logList.size
}