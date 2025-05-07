package com.example.easyqc3.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.easyqc3.R
import com.example.easyqc3.model.HistoryItem

class HistoryAdapter(private val historyList: List<HistoryItem>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // val dataImage: ImageView = itemView.findViewById(R.id.dataImage)
        val measurementDateTime: TextView = itemView.findViewById(R.id.measurementDateTime)
        val referenceLength: TextView = itemView.findViewById(R.id.referenceLength)
        val measuredValue: TextView = itemView.findViewById(R.id.measuredValue)
        val tolerance: TextView = itemView.findViewById(R.id.tolerance)
        val result: TextView = itemView.findViewById(R.id.result)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val historyItem = historyList[position]
        holder.measurementDateTime.text = "계측 시간: "+historyItem.measurementDateTime
        holder.referenceLength.text = "계측 기준 길이: ${historyItem.referenceLength}${historyItem.measurementUnit}"
        holder.measuredValue.text = "계측값: ${historyItem.measuredValue}${historyItem.measurementUnit}"
        holder.tolerance.text = "계측 오차범위: ±${historyItem.tolerance}${historyItem.measurementUnit}"
        holder.result.text = "결과: "+historyItem.result
    }

    override fun getItemCount() = historyList.size
}