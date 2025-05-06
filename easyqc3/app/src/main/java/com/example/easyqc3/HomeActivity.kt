package com.example.easyqc3

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.easyqc3.adapter.SimpleHistoryAdapter
import com.example.easyqc3.model.HistoryItem
import com.example.easyqc3.model.SimpleHistoryItem

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SimpleHistoryAdapter
    private lateinit var historyList: List<SimpleHistoryItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        val gotoMeasuringBtn = findViewById<ImageView>(R.id.gotoMeasuringBtn)
        val gotoStandardBtn = findViewById<ImageView>(R.id.gotoStandardBtn)
        val gotoHistoryBtn = findViewById<ImageView>(R.id.gotoHistoryBtn)

        gotoMeasuringBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        gotoStandardBtn.setOnClickListener {
            val intent = Intent(this, StandardActivity::class.java)
            startActivity(intent)
        }

        gotoHistoryBtn.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        recyclerView = findViewById(R.id.homeRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val fullHistoryList = listOf(
            HistoryItem(100.0, 0.5, "cm", 105.0, "OK", "user1", "2023-05-05"),
            HistoryItem(110.0, 0.6, "cm", 115.0, "fail", "user2", "2023-05-04"),
            HistoryItem(120.0, 0.7, "cm", 125.0, "OK", "user3", "2023-05-03"),
            HistoryItem(1.0, 0.5, "cm", 1.0, "OK", "user4", "2023-05-05"),
            HistoryItem(80.0, 0.6, "cm", 83.0, "fail", "user5", "2023-05-04"),
            HistoryItem(111.0, 0.7, "cm", 112.0, "OK", "user6", "2023-05-03")
        )

        historyList = fullHistoryList.map { item ->
            SimpleHistoryItem(
                measurementDateTime = item.measurementDateTime,
                referenceLength = item.referenceLength,
                measuredValue = item.measuredValue,
                measurementUnit = item.measurementUnit,
                result = item.result
            )
        }

        adapter = SimpleHistoryAdapter(historyList)
        recyclerView.adapter = adapter
    }
}