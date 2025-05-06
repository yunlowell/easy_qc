package com.example.easyqc3.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.easyqc3.R
import com.example.easyqc3.model.SimpleHistoryItem

class SimpleHistoryAdapter(private val itemList: List<SimpleHistoryItem>) :
    RecyclerView.Adapter<SimpleHistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val resultImage: ImageView = itemView.findViewById(R.id.resultImage)
        val measurementDateTime: TextView = itemView.findViewById(R.id.measurementDateTime)
        val referenceLength: TextView = itemView.findViewById(R.id.referenceLength)
        val measuredValue: TextView = itemView.findViewById(R.id.measuredValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_simplehistory, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemList[position]

        // 이미지 설정: result 값이 OK면 체크, 아니면 X로 설정
        val imageRes = if (item.result == "OK") {
            R.drawable.baseline_check_24
        } else {
            R.drawable.baseline_close_24
        }
        holder.resultImage.setImageResource(imageRes)

        // 텍스트 설정
        holder.measurementDateTime.text = "측정 날짜: ${item.measurementDateTime}"
        holder.referenceLength.text = "기준 길이: ${item.referenceLength}${item.measurementUnit}"
        holder.measuredValue.text = "측정 길이: ${item.measuredValue}${item.measurementUnit}"
    }
}